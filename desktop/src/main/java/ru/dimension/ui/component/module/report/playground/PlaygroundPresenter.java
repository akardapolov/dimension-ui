package ru.dimension.ui.component.module.report.playground;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Block;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.module.ReportChartModule;
import ru.dimension.ui.helper.DesignHelper;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.KeyHelper;
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

@Log4j2
public class PlaygroundPresenter implements ActionListener, MessageAction {
  private final PlaygroundModel model;
  private PlaygroundView view;

  private final MessageBroker broker = MessageBroker.getInstance();

  private static final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public PlaygroundPresenter(PlaygroundModel model, PlaygroundView view) {
    this.model = model;
    this.view = view;

    setupEventHandlers();
  }

  private void setupEventHandlers() {
    view.getQueryEditor().addCellEditorListener(new CellEditorListener() {
      @Override
      public void editingStopped(ChangeEvent e) {
        handleQueryEditorChange(e);
      }

      @Override
      public void editingCanceled(ChangeEvent e) {}
    });

    this.view.getQueryReportCase().getJxTable()
        .getColumnModel()
        .getColumn(0)
        .setCellEditor(view.getQueryEditor());

    this.view.getCollapseCard().addActionListener(this);
    this.view.getShowButton().addActionListener(this);
    this.view.getClearButton().addActionListener(this);
    this.view.getSaveButton().addActionListener(this);

    this.view.getCollapseCardPanel().setStateChangeConsumer(this::handleCollapseCardChange);
  }

  private void handleCollapseCardChange(ChartCardState cardState) {
    ArrayList<Component> containerCards = new ArrayList<>(List.of(view.getChartContainer().getComponents()));
    boolean collapseAll = ChartCardState.EXPAND_ALL.equals(cardState);

    containerCards.stream()
        .filter(c -> c instanceof ReportChartModule)
        .map(c -> (ReportChartModule) c)
        .forEach(cardChart -> cardChart.setCollapsed(collapseAll));
  }

  @Override
  public void receive(Message message) {
    switch (message.action()) {
      case ADD_CHART -> handleAddChart(message);
      case REMOVE_CHART -> handleRemoveChart(message);
      case NEED_TO_SAVE_COMMENT -> handleNeedSaveComment(message);
      case NEED_TO_SAVE_GROUP_FUNCTION -> handleNeedSaveGroupFunction(message);
    }
  }

  private void handleNeedSaveComment(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    CProfile cProfile = message.parameters().get("cProfile");
    String comment = message.parameters().get("comment");

    Optional<CProfileReport> cProfileReport = model.getMapReportData().get(key)
        .getCProfileReportList()
        .stream()
        .filter(f -> f.getColId() == cProfile.getColId())
        .findAny();

    cProfileReport.ifPresent(profileReport -> profileReport.setComment(comment));

    view.getSaveButton().setEnabled(true);
  }

  private void handleNeedSaveGroupFunction(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    CProfile cProfile = message.parameters().get("cProfile");
    GroupFunction groupFunction = message.parameters().get("groupFunction");

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

  private void handleAddChart(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    CProfile cProfile = message.parameters().get("cProfile");
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

    log.info("Add task pane: " + keyValue);

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

  private void handleRemoveChart(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    CProfile cProfile = message.parameters().get("cProfile");

    QueryReportData reportData = model.getMapReportData().get(key);
    if (reportData != null) {
      reportData.getCProfileReportList().removeIf(report -> report.getColId() == cProfile.getColId());
    }

    ReportChartModule taskPane = model.getChartPanes().get(key).get(cProfile);

    try {
      logChartAction(message.action(), cProfile);

      ChartKey chartKey = new ChartKey(key, cProfile);

      Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);
      broker.deleteReceiver(destinationHistory, taskPane.getPresenter());

      model.getChartPanes().get(key).remove(cProfile);
    } finally {
      log.info("Remove task pane: " + taskPane.getTitle());
      view.removeChartCard(taskPane);
      if (model.getChartPanes().get(key).isEmpty()) {
        model.getChartPanes().remove(key);
      }
      view.updateButtonStates();
    }
  }

  private Destination getDestination(Panel panel,
                                     ChartKey chartKey) {
    return Destination.builder().component(MessageBroker.Component.PLAYGROUND)
        .module(Module.CHART)
        .panel(panel)
        .block(Block.CHART)
        .chartKey(chartKey).build();
  }

  private void logChartAction(Action action,
                              CProfile cProfile) {
    log.info("Message action: " + action + " for " + cProfile);
  }

  private void handleQueryEditorChange(ChangeEvent e) {
    TableCellEditor editor = (TableCellEditor) e.getSource();
    Boolean isSelected = (Boolean) editor.getCellEditorValue();
    ProfileTaskQueryKey key = view.getProfileTaskQueryKey();

    if (isSelected) {
      addQueryCard(key);
    } else {
      removeQueryCard(key);
    }
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
    MetricColumnPanel cardMetricCol = view.getCardComponent(key);

    cardMetricCol.setTitle(query.getName());
    cardMetricCol.setToolTipText(createCardTitle(profile.getName(), task.getName(), ""));

    initializeMetricTable(cardMetricCol, metricList);
    initializeColumnTable(cardMetricCol, cProfileList);

    view.getCardContainer().add(cardMetricCol);
    view.getCardContainer().revalidate();
    view.getCardContainer().repaint();
  }

  public void removeQueryCard(ProfileTaskQueryKey key) {
    if (view.isVisibleCard(key)) {
      view.getCardContainer().remove(view.getCardComponent(key));
      view.getCardContainer().revalidate();
      view.getCardContainer().repaint();

      model.getMapReportData().remove(key);

      if (model.getChartPanes().containsKey(key)) {
        model.getChartPanes().get(key).values().forEach(view::removeChartCard);
        model.getChartPanes().remove(key);
      }
    }
  }

  private String createCardTitle(String profileName,
                                 String taskName,
                                 String queryName) {
    if (!queryName.equals("")) {
      return "<html><b>Profile:</b> " + profileName + " <br>"
          + "  <b>Task:</b> " + taskName + " <br>"
          + "  <b>Query:</b> " + queryName + " </html>";
    } else {
      return "<html><b>Profile:</b> " + profileName + "<br>"
          + "  <b>Task:</b> " + taskName + "</html>";
    }
  }

  private void initializeMetricTable(MetricColumnPanel cardMetricCol,
                                     List<Metric> metricList) {
    cardMetricCol.getJtcMetric().getDefaultTableModel().getDataVector().removeAllElements();
    cardMetricCol.getJtcMetric().getDefaultTableModel().fireTableDataChanged();

    if (metricList.size() != 0) {
      for (Metric m : metricList) {
        cardMetricCol.getJtcMetric().getDefaultTableModel().addRow(new Object[]{m.getId(), false, m.getName()});
      }
    }
  }

  private void initializeColumnTable(MetricColumnPanel cardMetricCol,
                                     List<CProfile> cProfileList) {
    cardMetricCol.getJtcColumn().getDefaultTableModel().getDataVector().removeAllElements();
    cardMetricCol.getJtcColumn().getDefaultTableModel().fireTableDataChanged();

    if (cProfileList != null) {
      cProfileList.stream()
          .filter(f -> !f.getCsType().isTimeStamp())
          .forEach(c -> cardMetricCol.getJtcColumn().getDefaultTableModel()
              .addRow(new Object[]{c.getColId(), false, c.getColName()}));
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
                                                   DialogHelper.showErrorDialog("Failed to save design: " + e.getMessage(),"Error", e));
      } finally {
        view.updateButtonStates();
        log.info("Design saved: {}", DesignHelper.formatFolderName(now));

        view.getSaveButton().setEnabled(true);
        view.getSaveButton().setText("Save design");

        broker.sendMessage(Message.builder()
                               .destination(Destination.withDefault(MessageBroker.Component.DESIGN))
                               .action(Action.NEED_TO_UPDATE_LIST_DESIGN)
                               .parameter("designName", DesignHelper.formatDesignName(now))
                               .build());
      }
    });
  }

  private void handleClear() {
    model.getChartPanes().forEach((key, innerMap) -> {
      innerMap.forEach((cProfile, chartModule) -> {
        ChartKey chartKey = new ChartKey(key, cProfile);
        Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);
        broker.deleteReceiver(destinationHistory, chartModule.getPresenter());
        view.removeChartCard(chartModule);
      });
    });
    model.getChartPanes().clear();

    view.getCardContainer().removeAll();
    view.getCardContainer().revalidate();
    view.getCardContainer().repaint();

    model.getMapReportData().clear();

    for (int row = 0; row < view.getQueryReportCase().getJxTable().getRowCount(); row++) {
      view.getQueryReportCase().getJxTable().setValueAt(false, row, 0);
    }

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
}