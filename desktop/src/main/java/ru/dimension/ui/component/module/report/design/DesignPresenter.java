package ru.dimension.ui.component.module.report.design;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Block;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.module.ReportChartModule;
import ru.dimension.ui.component.module.report.pdf.PdfReportDialog;
import ru.dimension.ui.component.module.report.pdf.PdfReportGenerator;
import ru.dimension.ui.component.module.report.playground.MetricColumnPanel;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.DesignHelper;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.KeyHelper;
import ru.dimension.ui.manager.ProfileManager;
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
import ru.dimension.ui.model.report.MetricReport;
import ru.dimension.ui.model.report.QueryReportData;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.UIState;

@Log4j2
public class DesignPresenter implements ActionListener, ListSelectionListener, MessageAction {

  private final DesignModel model;
  private DesignView view;

  private final MessageBroker broker = MessageBroker.getInstance();

  private static final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public DesignPresenter(DesignModel model,
                         DesignView view) {
    this.model = model;
    this.view = view;
    setupEventHandlers();
  }

  private void setupEventHandlers() {
    this.view.getDesignReportCase().getJxTable().getSelectionModel().addListSelectionListener(this);

    this.view.getCollapseCard().addActionListener(this);
    this.view.getShowButton().addActionListener(this);
    this.view.getClearButton().addActionListener(this);
    this.view.getSaveButton().addActionListener(this);
    this.view.getReportButton().addActionListener(this);
    this.view.getDeleteButton().addActionListener(this);

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
    log.info("Message received >>> " + message.destination() + " with action >>> " + message.action());

    switch (message.action()) {
      case ADD_CHART -> handleAddChart(message);
      case REMOVE_CHART -> handleRemoveChart(message);
      case NEED_TO_SAVE_COMMENT -> handleNeedSaveComment(message);
      case NEED_TO_SAVE_GROUP_FUNCTION -> handleNeedSaveGroupFunction(message);
      case NEED_TO_UPDATE_LIST_DESIGN -> handleNeedUpdateList(message);
    }
  }

  private void handleNeedSaveGroupFunction(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    CProfile cProfile = message.parameters().get("cProfile");
    GroupFunction groupFunction = message.parameters().get("groupFunction");

    log.info("Key: " + key);
    log.info("CProfile: " + cProfile);
    log.info("GroupFunction: " + groupFunction);

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

  private void handleNeedUpdateList(Message message) {
    String designName = message.parameters().get("designName");
    SwingUtilities.invokeLater(() -> {
      view.loadDesignConfigurationByName(designName);
    });
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
      view.getDesignReportCase().clearSelection();
    } else if (source == view.getSaveButton()) {
      handleSave();
      view.setEnabledButton(true, true, false, true, true);
    } else if (source == view.getReportButton()) {
      handleGeneratePdfReport();
    } else if (source == view.getDeleteButton()) {
      handleDeleteDesign();
    }
  }

  private void handleDeleteDesign() {
    int selectedRow = view.getDesignReportCase().getJxTable().getSelectedRow();
    if (selectedRow == -1) {
      JOptionPane.showMessageDialog(
          view,
          "No design selected. Please select a design to delete.",
          "Error",
          JOptionPane.ERROR_MESSAGE
      );
      return;
    }

    String designName = (String) view.getDesignReportCase().getDefaultTableModel().getValueAt(selectedRow, 0);
    LocalDateTime dateTime = DesignHelper.parseDesignDate(designName);
    String folderName = DesignHelper.formatFolderName(dateTime);

    // Create confirmation dialog with checkbox
    JCheckBox deleteReportsCheckBox = new JCheckBox("Delete associated reports");
    Object[] message = {
        "Are you sure you want to delete the design: " + designName + "?",
        deleteReportsCheckBox
    };

    int option = JOptionPane.showConfirmDialog(
        view,
        message,
        "Confirm Deletion",
        JOptionPane.YES_NO_OPTION
    );

    if (option == JOptionPane.YES_OPTION) {
      try {
        String designDirPath = model.getFilesHelper().getDesignDir() + File.separator + folderName;
        File designDir = new File(designDirPath);

        if (deleteReportsCheckBox.isSelected()) {
          // Delete entire directory with reports
          deleteDirectory(designDir);
        } else {
          // Delete only design files (non-PDF files)
          File[] files = designDir.listFiles();
          if (files != null) {
            for (File file : files) {
              if (!file.getName().toLowerCase().endsWith(".pdf")) {
                if (file.isDirectory()) {
                  deleteDirectory(file);
                } else {
                  file.delete();
                }
              }
            }
          }
          // Delete the directory if empty
          if (designDir.list() != null && designDir.list().length == 0) {
            designDir.delete();
          }
        }

        // Refresh the design list
        view.loadDesignConfiguration();

        // Clear current view if we deleted the loaded design
        if (model.getLoadedDesignFolder().isEmpty() &&
            model.getLoadedDesignFolder().equals(folderName)) {
          handleClear();
        }

      } catch (Exception e) {
        log.error("Failed to delete design", e);
        JOptionPane.showMessageDialog(
            view,
            "Failed to delete design: " + e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
      }

      handleClear();
      view.setEnabledButton(false, false, false, false, false);
    }
  }

  private void deleteDirectory(File directory) {
    if (directory.isDirectory()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          deleteDirectory(file);
        }
      }
    }
    directory.delete();
  }

  private void handleSave() {
    try {
      String designDir = GUIHelper.getNameByColumnName(
          view.getDesignReportCase().getJxTable(),
          view.getDesignReportCase().getDefaultTableModel(),
          view.getDesignReportCase().getJxTable().getSelectionModel(),
          "Design name"
      );

      LocalDateTime dateTime = DesignHelper.parseDesignDate(designDir);
      String folderDate = DesignHelper.formatFolderName(dateTime);

      saveDesignConfig(folderDate,
                       model.getMapReportData(),
                       view.getDateTimePickerFrom().getDate(),
                       view.getDateTimePickerTo().getDate());

      boolean hasChanges = hasConfigChanges();
      view.setEnabledButton(true, true, hasChanges, true, true);

    } catch (IOException e) {
      log.error("Failed to save design", e);
      DialogHelper.showErrorDialog("Failed to save design: " + e.getMessage(), "Error", e);
    }
  }

  public void saveDesignConfig(String folderDate,
                               Map<ProfileTaskQueryKey, QueryReportData> mapReportData,
                               Date startDate,
                               Date endDate) throws IOException {
    Map<String, QueryReportData> configToSave = new HashMap<>();

    for (Map.Entry<ProfileTaskQueryKey, QueryReportData> entry : mapReportData.entrySet()) {
      configToSave.put(DesignHelper.keyToString(entry.getKey()), entry.getValue());
    }

    DesignReportData designData = new DesignReportData(
        folderDate,
        startDate.getTime(),
        endDate.getTime()
    );

    designData.getMapReportData().putAll(configToSave);

    String fileJson = folderDate + model.getFilesHelper().getFileSeparator() + "design.json";

    // Delete existing design before saving
    try {
      model.getReportManager().deleteDesign(fileJson);
    } catch (IOException e) {
      log.warn("Design file didn't exist or could not be deleted: {}", fileJson, e);
    }

    // Save new configuration
    model.getReportManager().saveDesign(designData);
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) {
      return;
    }

    ListSelectionModel selectionModel = (ListSelectionModel) e.getSource();
    if (selectionModel.isSelectionEmpty()) {
      log.info("Clearing profile fields");
      view.setEnabledButton(false, false, false, false, false);
      return;
    }

    loadSelectedDesign();
  }

  private void loadSelectedDesign() {
    String designDir = GUIHelper.getNameByColumnName(
        view.getDesignReportCase().getJxTable(),
        view.getDesignReportCase().getDefaultTableModel(),
        view.getDesignReportCase().getJxTable().getSelectionModel(),
        "Design name");

    LocalDateTime dateTime = DesignHelper.parseDesignDate(designDir);
    String folderName = DesignHelper.formatFolderName(dateTime);

    model.setLoadedDesignFolder(folderName);

    try {
      loadDesignConfiguration(folderName);
    } catch (IOException e) {
      log.error("Failed to load design configuration for folder: {}", folderName, e);
      DialogHelper.showErrorDialog("Failed to load design: " + e.getMessage(), "Error", e);
      model.setLoadedDesignFolder("");
      return;
    }

    boolean hasChanges = hasConfigChanges();
    view.setEnabledButton(true, true, hasChanges, true, true);
  }

  private void loadDesignConfiguration(String folderName) throws IOException {
    DesignReportData designReportData = model.getReportManager().loadDesign(folderName);
    if (Objects.isNull(designReportData)) {
      return;
    }

    handleClear();

    processDesignEntry(designReportData);
  }

  private void processDesignEntry(DesignReportData designData) {
    ProfileManager profileManager = model.getProfileManager();

    Date dateBegin = new Date(designData.getDateFrom());
    Date dateEnd = new Date(designData.getDateTo());

    model.getDesignDateRanges().put(designData.getFolderName(), Map.entry(dateBegin, dateEnd));

    for (Map.Entry<String, QueryReportData> entry : designData.getMapReportData().entrySet()) {
      ProfileTaskQueryKey key = DesignHelper.stringToKey(entry.getKey());
      QueryReportData queryReportData = entry.getValue();

      QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
      TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());

      List<MetricReport> metricReportList = queryReportData.getMetricReportList();
      List<CProfileReport> cProfileReportList = queryReportData.getCProfileReportList();

      model.getMapReportData().putIfAbsent(key, new QueryReportData(cProfileReportList, metricReportList));

      ChartInfo chartInfo = profileManager.getChartInfoById(queryInfo.getId());
      if (Objects.isNull(chartInfo)) {
        throw new NotFoundException(String.format("Chart info with id=%s not found", queryInfo.getId()));
      }

      addQueryCard(key);

      if (metricReportList.size() != 0 || cProfileReportList.size() != 0) {
        MetricColumnPanel card = view.getCardComponent(key);

        for (MetricReport metric : metricReportList) {
          setCheckboxInTable(card.getJtcMetric(), metric.getId(), true);
        }

        for (CProfileReport cProfileReport : cProfileReportList) {
          CProfile cProfile = tableInfo.getCProfiles().stream()
              .filter(cp -> cp.getColId() == cProfileReport.getColId())
              .findFirst()
              .orElseThrow(() -> new NotFoundException("CProfile not found for colId: " + cProfileReport.getColId()));

          setCheckboxInTable(card.getJtcColumn(), cProfile.getColId(), true);

          ChartInfo chartInfoCopy = chartInfo.copy();
          chartInfoCopy.setRangeHistory(RangeHistory.CUSTOM);
          chartInfoCopy.setCustomBegin(dateBegin.getTime());
          chartInfoCopy.setCustomEnd(dateEnd.getTime());

          ChartKey chartKey = new ChartKey(key, cProfile);
          Metric metric = new Metric(tableInfo, cProfile);
          Optional.ofNullable(cProfileReport.getGroupFunction()).ifPresent(metric::setGroupFunction);
          Optional.ofNullable(cProfileReport.getChartType()).ifPresent(metric::setChartType);

          ReportChartModule taskPane = new ReportChartModule(
              model.getComponent(),
              chartKey,
              key,
              metric,
              queryInfo,
              chartInfoCopy,
              tableInfo,
              model.getDStore()
          );

          Optional.ofNullable(cProfileReport.getComment()).ifPresent(taskPane.getModel().getDescription()::setText);

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
      }

      view.setDatesWithoutValidation(dateBegin, dateEnd);
    }
  }

  private void setCheckboxInTable(JXTableCase tableCase,
                                  Object id,
                                  boolean checked) {
    DefaultTableModel model = tableCase.getDefaultTableModel();
    for (int i = 0; i < model.getRowCount(); i++) {
      if (model.getValueAt(i, 0).equals(id)) {
        model.setValueAt(checked, i, 1);
        break;
      }
    }
  }

  private void addQueryCard(ProfileTaskQueryKey key) {
    view.getCollapseCard().setVisible(true);

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

    view.getChartContainer().removeAll();
    view.getChartContainer().revalidate();
    view.getChartContainer().repaint();

    model.getMapReportData().clear();

    view.updateButtonStates();

    model.setLoadedDesignFolder("");
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

  private Destination getDestination(Panel panel,
                                     ChartKey chartKey) {
    return Destination.builder().component(model.getComponent())
        .module(Module.CHART)
        .panel(panel)
        .block(Block.CHART)
        .chartKey(chartKey).build();
  }

  private void handleAddChart(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    CProfile cProfile = message.parameters().get("cProfile");

    QueryInfo queryInfo = model.getProfileManager().getQueryInfoById(key.getQueryId());
    ChartInfo chartInfo = model.getProfileManager().getChartInfoById(queryInfo.getId());
    TableInfo tableInfo = model.getProfileManager().getTableInfoByTableName(queryInfo.getName());

    ChartKey chartKey = new ChartKey(key, cProfile);
    Metric metric = new Metric(tableInfo, cProfile);

    QueryReportData reportData = model.getMapReportData().computeIfAbsent(key, k -> new QueryReportData());

    CProfileReport existingReport = null;
    for (CProfileReport report : reportData.getCProfileReportList()) {
      if (report.getColId() == cProfile.getColId()) {
        existingReport = report;
        break;
      }
    }

    CProfileReport cProfileReport;
    if (existingReport != null) {
      cProfileReport = existingReport;
    } else {
      cProfileReport = new CProfileReport();
      reportData.getCProfileReportList().add(cProfileReport);
    }

    cProfileReport.setComment("");
    cProfileReport.setColId(cProfile.getColId());
    cProfileReport.setColIdSql(cProfile.getColIdSql());
    cProfileReport.setColName(cProfile.getColName());
    cProfileReport.setColDbTypeName(cProfile.getColDbTypeName());
    cProfileReport.setColSizeDisplay(cProfile.getColSizeDisplay());
    cProfileReport.setCsType(cProfile.getCsType());
    cProfileReport.setGroupFunction(metric.getGroupFunction());
    cProfileReport.setChartType(metric.getChartType());

    ChartInfo chartInfoCopy = chartInfo.copy();
    chartInfoCopy.setRangeHistory(RangeHistory.CUSTOM);
    Date beginDate = view.getDateTimePickerFrom().getDate();
    Date endDate = view.getDateTimePickerTo().getDate();
    chartInfoCopy.setCustomBegin(beginDate.getTime());
    chartInfoCopy.setCustomEnd(endDate.getTime());

    ReportChartModule taskPane = new ReportChartModule(
        model.getComponent(),
        chartKey,
        key,
        metric,
        queryInfo,
        chartInfoCopy,
        tableInfo,
        model.getDStore()
    );

    String keyValue = KeyHelper.getKey(model.getProfileManager(), key, cProfile);
    taskPane.setTitle(keyValue);

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

      boolean hasChanges = hasConfigChanges();
      view.setEnabledButton(false, false, hasChanges, hasChanges, false);
    });
  }

  private void handleRemoveChart(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    CProfile cProfile = message.parameters().get("cProfile");
    ChartKey chartKey = new ChartKey(key, cProfile);

    QueryReportData reportData = model.getMapReportData().get(key);
    if (reportData != null) {
      reportData.getCProfileReportList().removeIf(cpr -> cpr.getColId() == cProfile.getColId());
    }

    Map<CProfile, ReportChartModule> innerMap = model.getChartPanes().get(key);
    if (innerMap != null) {
      ReportChartModule chartModule = innerMap.remove(cProfile);
      if (chartModule != null) {
        Destination destinationHistory = getDestination(Panel.HISTORY, chartKey);
        broker.deleteReceiver(destinationHistory, chartModule.getPresenter());

        view.removeChartCard(chartModule);

        view.getChartContainer().revalidate();
        view.getChartContainer().repaint();
      }
      if (innerMap.isEmpty()) {
        model.getChartPanes().remove(key);
      }
    }

    boolean hasChanges = hasConfigChanges();
    view.setEnabledButton(false, false, hasChanges, hasChanges, false);
  }

  private void handleNeedSaveComment(Message message) {
    ProfileTaskQueryKey key = message.parameters().get("key");
    CProfile cProfile = message.parameters().get("cProfile");
    String comment = message.parameters().get("comment");

    log.info("Key: " + key);
    log.info("CProfile: " + cProfile);
    log.info("Comment: " + comment);

    Optional<CProfileReport> cProfileReport = model.getMapReportData().get(key)
        .getCProfileReportList()
        .stream()
        .filter(f -> f.getColId() == cProfile.getColId())
        .findAny();

    cProfileReport.ifPresent(profileReport -> profileReport.setComment(comment));

    view.getSaveButton().setEnabled(true);
  }

  private boolean hasConfigChanges() {
    int row = view.getDesignReportCase().getJxTable().getSelectedRow();
    if (row < 0)
      return false;

    String designDir = GUIHelper.getNameByColumnName(
        view.getDesignReportCase().getJxTable(),
        view.getDesignReportCase().getDefaultTableModel(),
        view.getDesignReportCase().getJxTable().getSelectionModel(),
        "Design name");

    LocalDateTime dateTime = DesignHelper.parseDesignDate(designDir);
    String folderName = DesignHelper.formatFolderName(dateTime);

    try {
      DesignReportData diskDesignData = model.getReportManager().loadDesign(folderName);

      Map<ProfileTaskQueryKey, QueryReportData> disk = new HashMap<>();
      for (Map.Entry<String, QueryReportData> e : diskDesignData.getMapReportData().entrySet()) {
        disk.put(DesignHelper.stringToKey(e.getKey()), e.getValue());
      }

      log.info("=== DISK CONFIG ===");
      logDetailedConfig(disk);
      log.info("=== MEMORY CONFIG ===");
      logDetailedConfig(model.getMapReportData());
      log.info("=========================");

      log.info("----Result-----");
      log.info(disk.equals(model.getMapReportData()));

      return !disk.equals(model.getMapReportData());
    } catch (IOException e) {
      log.error("Failed to load design from disk for comparison", e);
      return true;
    }
  }

  private void logDetailedConfig(Map<ProfileTaskQueryKey, QueryReportData> config) {
    for (Map.Entry<ProfileTaskQueryKey, QueryReportData> entry : config.entrySet()) {
      log.info("Key: " + entry.getKey());
      QueryReportData data = entry.getValue();

      log.info("  CProfileReports:");
      for (CProfileReport cpr : data.getCProfileReportList()) {
        log.info("    - colId: " + cpr.getColId() +
                     ", groupFunction: " + cpr.getGroupFunction() +
                     ", chartType: " + cpr.getChartType());
      }

      log.info("  MetricReports: " + data.getMetricReportList().size());
    }
  }

  private void handleGeneratePdfReport() {
    closeOpenPdfDialog();
    
    view.getReportButton().setEnabled(false);
    view.getReportButton().setText("Generation…");

    virtualThreadExecutor.execute(() -> {
      try {
        PdfReportGenerator pdfReportGenerator = new PdfReportGenerator(model.getFilesHelper(), model.getProfileManager());
        LocalDateTime now   = LocalDateTime.now();
        String formattedDate = now.format(DesignHelper.getFileFormatFormatter());

        String designDir = GUIHelper.getNameByColumnName(
            view.getDesignReportCase().getJxTable(),
            view.getDesignReportCase().getDefaultTableModel(),
            view.getDesignReportCase().getJxTable().getSelectionModel(),
            "Design name");

        LocalDateTime dateTime = DesignHelper.parseDesignDate(designDir);
        String folderName = DesignHelper.formatFolderName(dateTime);

        pdfReportGenerator.deleteOldReportFiles(folderName);

        List<File> htmlFiles = new ArrayList<>();
        int pageCount = 0;

        for (var outer : model.getChartPanes().entrySet()) {
          for (var inner : outer.getValue().entrySet()) {
            ReportChartModule chartModule = inner.getValue();
            ProfileTaskQueryKey key = outer.getKey();

            Map<String, Object> chartData =
                pdfReportGenerator.createChartData(key,
                                                   chartModule,
                                                   folderName);

            File html = pdfReportGenerator.generateHtmlReport(chartData, folderName,
                                                              formattedDate, 0, pageCount++);
            htmlFiles.add(html);
          }
        }

        pdfReportGenerator.generatePdfReport(htmlFiles, folderName, formattedDate);

        SwingUtilities.invokeLater(() -> {
          File pdfFile = new File(model.getFilesHelper().getDesignDir() +
                                      model.getFilesHelper().getFileSeparator() + folderName +
                                      model.getFilesHelper().getFileSeparator() + "report_" + formattedDate + ".pdf");

          if (pdfFile.exists()) {
            try {
              PdfReportDialog.showReportDialog(view, pdfFile);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        });

      } catch (Exception ex) {
        log.error("Failed to generate report", ex);
        javax.swing.SwingUtilities.invokeLater(() ->
                                                   DialogHelper.showErrorDialog("Failed to generate report: " + ex.getMessage(),
                                                                                "Error", ex));
      } finally {
        SwingUtilities.invokeLater(() -> {
          view.getReportButton().setEnabled(true);
          view.getReportButton().setText("Report");
        });
      }
    });
  }

  private void closeOpenPdfDialog() {
    Window[] windows = Window.getWindows();
    for (Window window : windows) {
      if (window instanceof PdfReportDialog) {
        window.dispose();
      }
    }
  }
}