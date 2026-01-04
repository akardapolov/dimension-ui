package ru.dimension.ui.component.module.report.playground;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Block;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.module.chart.ReportChartModule;
import ru.dimension.ui.component.module.factory.MetricColumnPanelFactory;
import ru.dimension.ui.component.module.report.event.AddChartEvent;
import ru.dimension.ui.component.module.report.event.RemoveChartEvent;
import ru.dimension.ui.component.module.report.event.SaveCommentEvent;
import ru.dimension.ui.component.module.report.event.SaveGroupFunctionEvent;
import ru.dimension.ui.component.module.report.event.UpdateDesignListEvent;
import ru.dimension.ui.helper.DesignHelper;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.KeyHelper;
import ru.dimension.ui.helper.event.EventRouteRegistry;
import ru.dimension.ui.helper.event.EventUtils;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.report.CProfileReport;
import ru.dimension.ui.model.report.DesignReportData;
import ru.dimension.ui.model.report.QueryReportData;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;
import ru.dimension.ui.view.table.row.Rows.MetricRow;
import ru.dimension.ui.view.table.row.Rows.PickableQueryRow;

@Log4j2
public class PlaygroundPresenter implements ActionListener {
  private final PlaygroundModel model;
  private PlaygroundView view;

  private final EventBus eventBus;
  private final EventRouteRegistry eventRouter;
  private final MetricColumnPanelFactory metricColumnPanelFactory;

  private final MessageBroker broker = MessageBroker.getInstance();

  private static final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public PlaygroundPresenter(PlaygroundModel model,
                             PlaygroundView view,
                             EventBus eventBus,
                             MetricColumnPanelFactory metricColumnPanelFactory) {
    this.model = model;
    this.view = view;
    this.eventBus = eventBus;
    this.metricColumnPanelFactory = metricColumnPanelFactory;

    this.eventRouter = EventRouteRegistry
        .forComponent(MessageBroker.Component.PLAYGROUND, EventUtils::getComponent)
        .route(AddChartEvent.class,          this::handleAddChart)
        .route(RemoveChartEvent.class,       this::handleRemoveChart)
        .route(SaveCommentEvent.class,       this::handleNeedSaveComment)
        .route(SaveGroupFunctionEvent.class, this::handleNeedSaveGroupFunction)
        .register(eventBus);

    setupEventHandlers();
  }

  private void setupEventHandlers() {
    view.setQueryCheckboxChangeListener(this::handleQueryCheckboxChange);

    this.view.getCollapseCard().addActionListener(this);
    this.view.getShowButton().addActionListener(this);
    this.view.getClearButton().addActionListener(this);
    this.view.getSaveButton().addActionListener(this);

    this.view.getCollapseCardPanel().setStateChangeConsumer(this::handleCollapseCardChange);
  }

  private void handleQueryCheckboxChange(PickableQueryRow pickableQueryRow, boolean isSelected) {
    ProfileTaskQueryKey key = view.getProfileTaskQueryKey();

    if (isSelected) {
      addQueryCard(key);
    } else {
      removeQueryCard(key);
    }
  }

  private void handleCollapseCardChange(ChartCardState cardState) {
    ArrayList<Component> containerCards = new ArrayList<>(List.of(view.getChartContainer().getComponents()));
    boolean collapseAll = ChartCardState.EXPAND_ALL.equals(cardState);

    containerCards.stream()
        .filter(c -> c instanceof ReportChartModule)
        .map(c -> (ReportChartModule) c)
        .forEach(cardChart -> cardChart.setCollapsed(collapseAll));
  }

  private void handleNeedSaveComment(SaveCommentEvent event) {
    ProfileTaskQueryKey key = event.getKey();
    CProfile cProfile = event.getCProfile();
    String comment = event.getComment();

    Optional<CProfileReport> cProfileReport = model.getMapReportData().get(key)
        .getCProfileReportList()
        .stream()
        .filter(f -> f.getColId() == cProfile.getColId())
        .findAny();

    cProfileReport.ifPresent(profileReport -> profileReport.setComment(comment));

    view.getSaveButton().setEnabled(true);
  }

  private void handleNeedSaveGroupFunction(SaveGroupFunctionEvent event) {
    ProfileTaskQueryKey key = event.getKey();
    CProfile cProfile = event.getCProfile();
    GroupFunction groupFunction = event.getGroupFunction();

    Optional<CProfileReport> cProfileReport = model.getMapReportData().get(key)
        .getCProfileReportList()
        .stream()
        .filter(f -> f.getColId() == cProfile.getColId())
        .findAny();

    cProfileReport.ifPresent(profileReport -> {
      profileReport.setGroupFunction(groupFunction);
      ChartType chartType = GroupFunction.COUNT.equals(groupFunction) ?
          ChartType.STACKED : ChartType.LINEAR;
      profileReport.setChartType(chartType);
    });

    view.getSaveButton().setEnabled(true);
  }

  private void handleAddChart(AddChartEvent event) {
    ProfileTaskQueryKey key = event.getKey();
    CProfile cProfile = event.getCProfile();
    ChartKey chartKey = new ChartKey(key, cProfile);

    QueryInfo queryInfo = model.getProfileManager().getQueryInfoById(key.getQueryId());
    ChartInfo chartInfo = model.getProfileManager().getChartInfoById(key.getQueryId());
    TableInfo tableInfo = model.getProfileManager().getTableInfoByTableName(queryInfo.getName());

    Metric metric = new Metric(tableInfo, cProfile);

    QueryReportData reportData = model.getMapReportData().get(key);
    if (reportData != null) {
      CProfileReport cProfileReport = new CProfileReport();
      cProfileReport.setComment("");
      cProfileReport.setColId(cProfile.getColId());
      cProfileReport.setColIdSql(cProfile.getColIdSql());
      cProfileReport.setColName(cProfile.getColName());
      cProfileReport.setColDbTypeName(cProfile.getColDbTypeName());
      cProfileReport.setColSizeDisplay(cProfile.getColSizeDisplay());
      cProfileReport.setCsType(cProfile.getCsType());
      cProfileReport.setGroupFunction(metric.getGroupFunction());
      cProfileReport.setChartType(metric.getChartType());

      reportData.getCProfileReportList().add(cProfileReport);
    }

    ChartInfo chartInfoCopy = chartInfo.copy();
    chartInfoCopy.setRangeHistory(RangeHistory.CUSTOM);
    Date beginDate = view.getDateTimePickerFrom().getDate();
    Date endDate = view.getDateTimePickerTo().getDate();
    chartInfoCopy.setCustomBegin(beginDate.getTime());
    chartInfoCopy.setCustomEnd(endDate.getTime());

    DStore dStore = model.getDStore();

    ReportChartModule taskPane =
        new ReportChartModule(model.getComponent(), chartKey, key, metric, queryInfo, chartInfoCopy, tableInfo, dStore);

    String keyValue = KeyHelper.getKey(model.getProfileManager(), key, cProfile);
    taskPane.setTitle(keyValue);

    log.info("Add task pane: {}", keyValue);

    view.addChartCard(taskPane, (module, error) -> {
      if (error != null) {
        DialogHelper.showErrorDialog("Failed to load chart: " + error.getMessage(), "Error", error);
        return;
      }
      model.getChartPanes().computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(cProfile, taskPane);

      Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);
      broker.addReceiver(destinationHistory, taskPane.getPresenter());

      taskPane.revalidate();
      taskPane.repaint();

      ChartCardState chartCardState = UIState.INSTANCE.getChartCardStateAll(model.getComponent().name());
      taskPane.setCollapsed(ChartCardState.EXPAND_ALL.equals(chartCardState));

      view.updateButtonStates();
    });
  }

  private void handleRemoveChart(RemoveChartEvent event) {
    ProfileTaskQueryKey key = event.getKey();
    CProfile cProfile = event.getCProfile();

    QueryReportData reportData = model.getMapReportData().get(key);
    if (reportData != null) {
      reportData.getCProfileReportList().removeIf(report -> report.getColId() == cProfile.getColId());
    }

    ReportChartModule taskPane = model.getChartPanes().get(key).get(cProfile);

    try {
      log.info("Remove chart: {}", cProfile);

      if (taskPane != null) {
        ChartKey chartKey = new ChartKey(key, cProfile);
        Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);
        broker.deleteReceiver(destinationHistory, taskPane.getPresenter());
      }

      model.getChartPanes().get(key).remove(cProfile);
    } finally {
      if (taskPane != null) {
        log.info("Remove task pane: {}", taskPane.getTitle());
        view.removeChartCard(taskPane);
      }
      if (model.getChartPanes().get(key).isEmpty()) {
        model.getChartPanes().remove(key);
      }
      view.updateButtonStates();
    }
  }

  private Destination getDestination(Panel panel, ChartKey chartKey) {
    return Destination.builder()
        .component(model.getComponent())
        .module(Module.CHART)
        .panel(panel)
        .block(Block.CHART)
        .chartKey(chartKey)
        .build();
  }

  private void addQueryCard(ProfileTaskQueryKey key) {
    view.getCollapseCard().setVisible(true);
    model.getMapReportData().put(key, new QueryReportData());

    QueryInfo query = model.getProfileManager().getQueryInfoById(key.getQueryId());
    ProfileInfo profile = model.getProfileManager().getProfileInfoById(key.getProfileId());
    TaskInfo task = model.getProfileManager().getTaskInfoById(key.getTaskId());
    List<Metric> metricList = query.getMetricList();
    TableInfo tableInfo = model.getProfileManager().getTableInfoByTableName(query.getName());

    List<CProfile> cProfileList = tableInfo.getCProfiles();
    MetricColumnPanel cardMetricCol = getCardComponent(key);

    cardMetricCol.setTitle(query.getName());
    cardMetricCol.setToolTipText(createCardTitle(profile.getName(), task.getName(), ""));

    initializeMetricTable(cardMetricCol, metricList);
    initializeColumnTable(cardMetricCol, cProfileList);

    view.getCardContainer().add(cardMetricCol);
    view.getCardContainer().revalidate();
    view.getCardContainer().repaint();
  }

  public void removeQueryCard(ProfileTaskQueryKey key) {
    if (isVisibleCard(key)) {
      view.getCardContainer().remove(getCardComponent(key));
      view.getCardContainer().revalidate();
      view.getCardContainer().repaint();

      model.getMapReportData().remove(key);

      if (model.getChartPanes().containsKey(key)) {
        model.getChartPanes().get(key).values().forEach(chartModule -> {
          view.removeChartCard(chartModule);

          ChartKey chartKey = chartModule.getModel().getChartKey();
          Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);
          broker.deleteReceiver(destinationHistory, chartModule.getPresenter());
        });
        model.getChartPanes().remove(key);
      }
    }
  }

  private String createCardTitle(String profileName, String taskName, String queryName) {
    if (!queryName.equals("")) {
      return "<html><b>Profile:</b> " + profileName + " <br>"
          + "  <b>Task:</b> " + taskName + " <br>"
          + "  <b>Query:</b> " + queryName + " </html>";
    } else {
      return "<html><b>Profile:</b> " + profileName + "<br>"
          + "  <b>Task:</b> " + taskName + "</html>";
    }
  }

  private void initializeMetricTable(MetricColumnPanel cardMetricCol, List<Metric> metricList) {
    if (metricList != null && !metricList.isEmpty()) {
      List<MetricRow> rows = metricList.stream()
          .map(m -> new MetricRow(m, false))
          .collect(Collectors.toList());
      cardMetricCol.setMetricItems(rows);
    } else {
      cardMetricCol.setMetricItems(List.of());
    }
  }

  private void initializeColumnTable(MetricColumnPanel cardMetricCol, List<CProfile> cProfileList) {
    if (cProfileList != null) {
      List<ColumnRow> rows = cProfileList.stream()
          .filter(f -> !f.getCsType().isTimeStamp())
          .map(c -> new ColumnRow(c, false))
          .collect(Collectors.toList());
      cardMetricCol.setColumnItems(rows);
    } else {
      cardMetricCol.setColumnItems(List.of());
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();

    if (source == view.getCollapseCard()) {
      handleCollapseDesign();
    } else if (source == view.getShowButton()) {
      handleShow();
    } else if (source == view.getClearButton()) {
      handleClear();
    } else if (source == view.getSaveButton()) {
      handleSaveDesign();
    }
  }

  private void handleSaveDesign() {
    if (model.getMapReportData().isEmpty()) {
      JOptionPane.showMessageDialog(view,
                                    "Nothing to save – select at least one query.",
                                    "Information", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    view.getSaveButton().setEnabled(false);
    view.getSaveButton().setText("Saving…");

    LocalDateTime now = LocalDateTime.now();
    String folderDate = DesignHelper.formatFolderName(now);

    virtualThreadExecutor.execute(() -> {
      try {
        Map<String, QueryReportData> configToSave = new HashMap<>();

        for (Map.Entry<ProfileTaskQueryKey, QueryReportData> entry : model.getMapReportData().entrySet()) {
          configToSave.put(DesignHelper.keyToString(entry.getKey()), entry.getValue());
        }

        DesignReportData designData = new DesignReportData(
            folderDate,
            view.getDateTimePickerFrom().getDate().getTime(),
            view.getDateTimePickerTo().getDate().getTime()
        );
        designData.getMapReportData().putAll(configToSave);

        model.getReportManager().saveDesign(designData);
      } catch (Exception e) {
        log.error("Failed to save design", e);
        javax.swing.SwingUtilities.invokeLater(() ->
                                                   DialogHelper.showErrorDialog("Failed to save design: " + e.getMessage(), "Error", e));
      } finally {
        view.updateButtonStates();
        log.info("Design saved: {}", DesignHelper.formatFolderName(now));

        view.getSaveButton().setEnabled(true);
        view.getSaveButton().setText("Save design");

        eventBus.publish(new UpdateDesignListEvent(DesignHelper.formatDesignName(now)));
      }
    });
  }

  private void handleClear() {
    model.getChartPanes().forEach((key, innerMap) -> {
      innerMap.forEach((cProfile, chartModule) -> {
        view.removeChartCard(chartModule);

        ChartKey chartKey = chartModule.getModel().getChartKey();
        Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);
        broker.deleteReceiver(destinationHistory, chartModule.getPresenter());
      });
    });
    model.getChartPanes().clear();

    view.getCardContainer().removeAll();
    view.getCardContainer().revalidate();
    view.getCardContainer().repaint();

    model.getMapReportData().clear();

    view.clearAllCharts();

    view.updateButtonStates();
  }

  private void handleShow() {
    Date beginDate = view.getDateTimePickerFrom().getDate();
    Date endDate = view.getDateTimePickerTo().getDate();
    ChartRange range = new ChartRange(beginDate.getTime(), endDate.getTime());

    model.getChartPanes().values().stream()
        .flatMap(innerMap -> innerMap.values().stream())
        .forEach(chartPane -> chartPane.updateHistoryCustomRange(range));
  }

  private void handleCollapseDesign() {
    ArrayList<Component> containerCards = new ArrayList<>(List.of(view.getCardContainer().getComponents()));
    boolean collapseAll = view.getCollapseCard().getText().equals("Collapse all");

    containerCards.stream()
        .filter(c -> c instanceof MetricColumnPanel)
        .map(c -> (MetricColumnPanel) c)
        .forEach(cardChart -> cardChart.setCollapsed(collapseAll));

    view.getCollapseCard().setText(collapseAll ? "Expand all" : "Collapse all");
  }

  private MetricColumnPanel getCardComponent(ProfileTaskQueryKey key) {
    return Arrays.stream(view.getCardContainer().getComponents())
        .filter(MetricColumnPanel.class::isInstance)
        .map(MetricColumnPanel.class::cast)
        .filter(card -> key.equals(card.getKey()))
        .findFirst()
        .orElseGet(() -> metricColumnPanelFactory.create(MessageBroker.Component.PLAYGROUND,
                                                         key,
                                                         view.getCollapseCard(),
                                                         view.getCardContainer()));
  }

  public boolean isVisibleCard(ProfileTaskQueryKey key) {
    return Arrays.stream(view.getCardContainer().getComponents())
        .filter(MetricColumnPanel.class::isInstance)
        .map(MetricColumnPanel.class::cast)
        .anyMatch(card -> key.equals(card.getKey()));
  }
}