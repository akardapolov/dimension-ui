package ru.dimension.ui.component.module.report.playground;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;
import static ru.dimension.ui.laf.LafColorGroup.REPORT;

import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.plaf.basic.CalendarHeaderHandler;
import org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.module.ReportChartModule;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.DesignHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.column.ProfileColumnNames;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.model.column.TaskColumnNames;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.panel.DateTimePicker;

@Log4j2
@Data
@EqualsAndHashCode(callSuper = true)
public class PlaygroundView extends JPanel implements ListSelectionListener {

  private static final int QUERY_PICK_COLUMN_INDEX = 1;
  private static final int QUERY_NAME_COLUMN_INDEX = 2;
  private static final int MIN_COLUMN_WIDTH = 30;
  private static final int MAX_COLUMN_WIDTH = 35;

  private final PlaygroundModel model;
  private final JSplitPane mainSplitPane;
  private final JSplitPane modelSplitPane;
  private final JSplitPane configChartsSplitPane;
  private final JXTableCase profileReportCase;
  private final JXTableCase taskReportCase;
  private final JXTableCase queryReportCase;
  private final DefaultCellEditor queryEditor;
  private final JCheckBox collapseCard;
  private final DateTimePicker dateTimePickerFrom;
  private final DateTimePicker dateTimePickerTo;
  private Date lastValidFrom;
  private Date lastValidTo;
  private final JLabel lblFrom;
  private final JLabel lblTo;
  private final JButton showButton;
  private final JButton clearButton;
  private final JButton saveButton;
  private final JXTaskPaneContainer cardContainer;
  private final JXTaskPaneContainer chartContainer;
  private final JScrollPane cardScrollPane;
  private final JScrollPane chartScrollPane;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public PlaygroundView(PlaygroundModel model) {
    this.model = model;
    this.mainSplitPane = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 240);
    this.modelSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 430);
    this.configChartsSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 50);
    this.profileReportCase = createProfileTable();
    this.taskReportCase = createTaskTable();
    this.queryReportCase = createQueryTable();
    this.queryEditor = new DefaultCellEditor(new JCheckBox());
    this.collapseCard = createCollapseCheckBox();
    this.lblFrom = new JLabel("From");
    this.lblTo = new JLabel("To");
    this.dateTimePickerFrom = createDateTimePicker();
    this.dateTimePickerTo = createDateTimePicker();
    this.showButton = createShowButton();
    this.clearButton = createClearButton();
    this.saveButton = createSaveButton();
    this.cardContainer = initContainerCard();
    this.cardScrollPane = createCardScrollPane();
    this.chartContainer = initChartContainer();
    this.chartScrollPane = createChartScrollPane();

    initDateTimePickers();
    setupDateChangeListeners();
    setupSelectionListeners();
    configureQueryTableColumns();
    setupLayout();
  }

  private void initDateTimePickers() {
    Map.Entry<Date, Date> range = DateHelper.getRangeDate();
    lastValidFrom = range.getKey();
    lastValidTo = range.getValue();

    dateTimePickerFrom.setDate(lastValidFrom);
    dateTimePickerTo.setDate(lastValidTo);

    ChartRange chartRange = new ChartRange(lastValidFrom.getTime(), lastValidTo.getTime());
    UIState.INSTANCE.putHistoryRangeAll(model.getComponent().name(), RangeHistory.CUSTOM);
    UIState.INSTANCE.putHistoryCustomRangeAll(model.getComponent().name(), chartRange);
  }

  private void setupDateChangeListeners() {
    PropertyChangeListener dateChangeListener = evt -> {
      if (!"date".equals(evt.getPropertyName())) {
        return;
      }

      boolean fromChanged = evt.getSource() == dateTimePickerFrom;
      Date newDate = (Date) evt.getNewValue();

      Date candidateFrom = fromChanged ? newDate : dateTimePickerFrom.getDate();
      Date candidateTo = fromChanged ? dateTimePickerTo.getDate() : newDate;

      if (candidateTo.before(candidateFrom)) {

        if (fromChanged) {
          dateTimePickerFrom.setDate(lastValidFrom);
        } else {
          dateTimePickerTo.setDate(lastValidTo);
        }

        JOptionPane.showMessageDialog(
            this,
            "Start date must be before the end date",
            "Invalid Date Range",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      lastValidFrom = candidateFrom;
      lastValidTo = candidateTo;

      ChartRange chartRange = new ChartRange(lastValidFrom.getTime(), lastValidTo.getTime());
      UIState.INSTANCE.putHistoryRangeAll(model.getComponent().name(), RangeHistory.CUSTOM);
      UIState.INSTANCE.putHistoryCustomRangeAll(model.getComponent().name(), chartRange);
    };

    dateTimePickerFrom.addPropertyChangeListener(dateChangeListener);
    dateTimePickerTo.addPropertyChangeListener(dateChangeListener);
  }

  private DateTimePicker createDateTimePicker() {
    DateTimePicker picker = new DateTimePicker();
    SimpleDateFormat format = new SimpleDateFormat(DesignHelper.DATE_FORMAT_PATTERN);
    picker.setFormats(format);
    picker.setTimeFormat(format);
    UIManager.put(CalendarHeaderHandler.uiControllerID, SpinningCalendarHeaderHandler.class.getName());
    picker.getMonthView().setZoomable(true);
    return picker;
  }

  private JXTableCase createProfileTable() {
    return GUIHelper.getJXTableCase(5,
                                    new String[]{
                                        ProfileColumnNames.ID.getColName(),
                                        ProfileColumnNames.NAME.getColName()
                                    });
  }

  private JXTableCase createTaskTable() {
    return GUIHelper.getJXTableCase(5,
                                    new String[]{
                                        TaskColumnNames.ID.getColName(),
                                        TaskColumnNames.NAME.getColName()
                                    });
  }

  private JXTableCase createQueryTable() {
    return GUIHelper.getJXTableCaseCheckBox(5,
                                            new String[]{
                                                QueryColumnNames.ID.getColName(),
                                                QueryColumnNames.PICK.getColName(),
                                                QueryColumnNames.NAME.getColName()
                                            }, QUERY_PICK_COLUMN_INDEX);
  }

  private JCheckBox createCollapseCheckBox() {
    JCheckBox checkBox = new JCheckBox("Collapse all");
    checkBox.setMnemonic('A');
    checkBox.setVisible(false);
    return checkBox;
  }

  private JButton createShowButton() {
    JButton button = new JButton("Show");
    button.setMnemonic('H');
    button.setEnabled(false);
    return button;
  }

  private JButton createClearButton() {
    JButton button = new JButton("Clear");
    button.setMnemonic('C');
    button.setEnabled(false);
    return button;
  }

  private JButton createSaveButton() {
    JButton b = new JButton("Save design");
    b.setMnemonic('S');
    b.setEnabled(false);
    return b;
  }

  private JScrollPane createCardScrollPane() {
    JPanel cardPanel = new JPanel(new VerticalLayout());
    LaF.setBackgroundColor(REPORT, cardPanel);
    cardPanel.add(collapseCard);
    cardPanel.add(cardContainer);

    JScrollPane scrollPane = new JScrollPane();
    GUIHelper.setScrolling(scrollPane);
    scrollPane.setViewportView(cardPanel);
    return scrollPane;
  }

  private JXTaskPaneContainer initChartContainer() {
    JXTaskPaneContainer container = new JXTaskPaneContainer();
    LaF.setBackgroundColor(LafColorGroup.REPORT, container);
    container.setBackgroundPainter(null);
    return container;
  }

  private JScrollPane createChartScrollPane() {
    JPanel chartPanel = new JPanel(new VerticalLayout());
    LaF.setBackgroundColor(REPORT, chartPanel);
    chartPanel.add(chartContainer);

    JScrollPane scrollPane = new JScrollPane();
    GUIHelper.setScrolling(scrollPane);
    scrollPane.setViewportView(chartPanel);
    return scrollPane;
  }

  private void setupSelectionListeners() {
    profileReportCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    taskReportCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    queryReportCase.getJxTable().getSelectionModel().addListSelectionListener(this);
  }

  private void configureQueryTableColumns() {
    queryReportCase.getJxTable().getColumnExt(0).setVisible(false);
    TableColumn col = queryReportCase.getJxTable().getColumnModel().getColumn(0);
    col.setMinWidth(MIN_COLUMN_WIDTH);
    col.setMaxWidth(MAX_COLUMN_WIDTH);
  }

  private void setupLayout() {
    fillModel();
    modelSplitPane.setTopComponent(createModelPane());
    modelSplitPane.setBottomComponent(cardScrollPane);
    mainSplitPane.setLeftComponent(modelSplitPane);
    mainSplitPane.setRightComponent(createConfigChartsPanel());
    addMainSplitPaneToThis();
  }

  private JPanel createModelPane() {
    JPanel panel = new JPanel();
    LaF.setBackgroundConfigPanel(CONFIG_PANEL, panel);
    panel.setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(panel, PGHelper.getPGConfig(), false);
    gbl.row().cell(new JXTitledSeparator("Profile")).fillX();
    gbl.row().cell(profileReportCase.getJScrollPane()).fillXY();
    gbl.row().cell(new JXTitledSeparator("Task")).fillX();
    gbl.row().cell(taskReportCase.getJScrollPane()).fillXY();
    gbl.row().cell(new JXTitledSeparator("Query")).fillX();
    gbl.row().cell(queryReportCase.getJScrollPane()).fillXY();
    gbl.done();

    return panel;
  }

  private JSplitPane createConfigChartsPanel() {
    JPanel configPanel = createConfigPanel();
    configChartsSplitPane.setTopComponent(configPanel);
    configChartsSplitPane.setBottomComponent(chartScrollPane);
    configChartsSplitPane.setDividerLocation(50);
    return configChartsSplitPane;
  }

  private JPanel createConfigPanel() {
    JPanel configPanel = new JPanel();
    configPanel.setBorder(new EtchedBorder());

    PainlessGridBag gblConfig = new PainlessGridBag(configPanel, PGHelper.getPGConfig(), false);
    gblConfig.row()
        .cell(lblFrom).cell(dateTimePickerFrom)
        .cell(lblTo).cell(dateTimePickerTo)
        .cell(showButton)
        .cell(clearButton)
        .cell(saveButton)
        .cell(new JLabel()).fillX();
    gblConfig.done();
    return configPanel;
  }

  private void addMainSplitPaneToThis() {
    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(mainSplitPane).fillXY();
    gbl.done();
  }

  private void fillModel() {
    model.getConfigurationManager().getConfigList(Profile.class)
        .forEach(profile -> profileReportCase.getDefaultTableModel().addRow(
            new Object[]{profile.getId(), profile.getName()}));

    if (profileReportCase.getDefaultTableModel().getRowCount() > 0) {
      profileReportCase.getJxTable().setRowSelectionInterval(0, 0);
    }

    profileReportCase.getJxTable().getColumnExt(0).setVisible(false);
    profileReportCase.getJxTable().getColumnModel().getColumn(0)
        .setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());

    taskReportCase.getJxTable().getColumnExt(0).setVisible(false);
    taskReportCase.getJxTable().getColumnModel().getColumn(0)
        .setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());
  }

  public ProfileTaskQueryKey getProfileTaskQueryKey() {
    int profileId = getSelectedId(profileReportCase, ProfileColumnNames.ID.getColName());
    int taskId = getSelectedId(taskReportCase, TaskColumnNames.ID.getColName());
    int queryId = getSelectedId(queryReportCase, QueryColumnNames.ID.getColName());
    return new ProfileTaskQueryKey(profileId, taskId, queryId);
  }

  private int getSelectedId(JXTableCase tableCase,
                            String columnName) {
    int selectedRow = tableCase.getJxTable().getSelectedRow();
    int columnIndex = tableCase.getDefaultTableModel().findColumn(columnName);
    return (int) tableCase.getDefaultTableModel().getValueAt(selectedRow, columnIndex);
  }

  public MetricColumnPanel getCardComponent(ProfileTaskQueryKey key) {
    return Arrays.stream(cardContainer.getComponents())
        .filter(MetricColumnPanel.class::isInstance)
        .map(MetricColumnPanel.class::cast)
        .filter(card -> key.equals(card.getKey()))
        .findFirst()
        .orElseGet(() -> new MetricColumnPanel(
            Component.PLAYGROUND,
            key,
            model.getProfileManager(),
            collapseCard,
            cardContainer));
  }

  public boolean isVisibleCard(ProfileTaskQueryKey key) {
    return Arrays.stream(cardContainer.getComponents())
        .filter(MetricColumnPanel.class::isInstance)
        .map(MetricColumnPanel.class::cast)
        .anyMatch(card -> key.equals(card.getKey()));
  }

  private JXTaskPaneContainer initContainerCard() {
    JXTaskPaneContainer container = new JXTaskPaneContainer();
    LaF.setBackgroundColor(LafColorGroup.REPORT, container);
    container.setBackgroundPainter(null);
    return container;
  }

  public void addChartCard(ReportChartModule taskPane,
                           BiConsumer<ReportChartModule, Exception> onComplete) {

    addChartCard(taskPane);

    SwingTaskRunner.runWithProgress(
        taskPane,
        executor,
        taskPane::initializeUI,
        e -> {
          removeChartCard(taskPane);
          onComplete.accept(null, e);
        },
        () -> createProgressBar("Loading, please wait..."),
        () -> onComplete.accept(taskPane, null)
    );
  }

  public void addChartCard(ReportChartModule taskPane) {
    chartContainer.add(taskPane);
    chartContainer.revalidate();
    chartContainer.repaint();
  }

  public void removeChartCard(ReportChartModule taskPane) {
    if (taskPane != null) {
      chartContainer.remove(taskPane);
      chartContainer.revalidate();
      chartContainer.repaint();
    }
  }

  public void clearAllCharts() {
    chartContainer.removeAll();
    chartContainer.revalidate();
    chartContainer.repaint();
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) {
      return;
    }

    ListSelectionModel selectionModel = (ListSelectionModel) e.getSource();
    if (selectionModel.isSelectionEmpty()) {
      return;
    }

    if (e.getSource() == profileReportCase.getJxTable().getSelectionModel()) {
      handleProfileSelection();
    } else if (e.getSource() == taskReportCase.getJxTable().getSelectionModel()) {
      handleTaskSelection();
    }
  }

  public void updateButtonStates() {
    boolean hasCharts = model.hasCharts();
    boolean hasCards = !model.getMapReportData().isEmpty();

    showButton.setEnabled(hasCharts);
    clearButton.setEnabled(hasCharts);
    saveButton.setEnabled(hasCharts);
    collapseCard.setVisible(hasCards);
  }

  private void handleProfileSelection() {
    clearTaskTable();
    int profileId = getSelectedProfileId();

    if (profileId < 0) {
      showProfileNotSelectedError();
      return;
    }

    loadTasksForProfile(profileId);
    updateTaskSelection();
  }

  private void clearTaskTable() {
    taskReportCase.getDefaultTableModel().getDataVector().removeAllElements();
    taskReportCase.getDefaultTableModel().fireTableDataChanged();
  }

  private int getSelectedProfileId() {
    return GUIHelper.getIdByColumnName(
        profileReportCase.getJxTable(),
        profileReportCase.getDefaultTableModel(),
        profileReportCase.getJxTable().getSelectionModel(),
        ProfileColumnNames.ID.getColName()
    );
  }

  private void showProfileNotSelectedError() {
    JOptionPane.showMessageDialog(null,
                                  "Profile is not selected",
                                  "General Error",
                                  JOptionPane.ERROR_MESSAGE);
  }

  private void loadTasksForProfile(int profileId) {
    model.getProfileManager().getProfileInfoById(profileId).getTaskInfoList()
        .forEach(taskId -> {
          TaskInfo taskInfo = model.getProfileManager().getTaskInfoById(taskId);
          if (taskInfo == null) {
            throw new NotFoundException("Not found task: " + taskId);
          }
          taskReportCase.getDefaultTableModel().addRow(
              new Object[]{taskInfo.getId(), taskInfo.getName()});
        });
  }

  private void updateTaskSelection() {
    if (taskReportCase.getDefaultTableModel().getRowCount() > 0) {
      taskReportCase.getJxTable().setRowSelectionInterval(0, 0);
    } else {
      clearQueryTable();
    }
  }

  private void clearQueryTable() {
    queryReportCase.getDefaultTableModel().getDataVector().removeAllElements();
    queryReportCase.getDefaultTableModel().fireTableDataChanged();
  }

  private void handleTaskSelection() {
    clearQueryTable();
    int taskId = getSelectedTaskId();

    if (taskReportCase.getDefaultTableModel().getRowCount() > 0) {
      loadQueriesForTask(taskId);
    } else {
      loadAllQueries();
    }

    updateQuerySelection();
    markSelectedQueries();
  }

  private int getSelectedTaskId() {
    return GUIHelper.getIdByColumnName(
        taskReportCase.getJxTable(),
        taskReportCase.getDefaultTableModel(),
        taskReportCase.getJxTable().getSelectionModel(),
        TaskColumnNames.ID.getColName()
    );
  }

  private void loadQueriesForTask(int taskId) {
    TaskInfo taskInfo = model.getProfileManager().getTaskInfoById(taskId);
    if (taskInfo == null) {
      throw new NotFoundException("Not found task: " + taskId);
    }

    AtomicInteger row = new AtomicInteger();
    taskInfo.getQueryInfoList().forEach(queryId -> {
      QueryInfo queryInfo = model.getProfileManager().getQueryInfoById(queryId);
      if (queryInfo == null) {
        throw new NotFoundException("Not found query: " + queryId);
      }
      addQueryToTable(row.getAndIncrement(), queryInfo);
    });
  }

  private void addQueryToTable(int row,
                               QueryInfo queryInfo) {
    queryReportCase.getDefaultTableModel().addRow(new Object[0]);
    queryReportCase.getJxTable().setValueAt(false, row, 0);
    queryReportCase.getDefaultTableModel().setValueAt(queryInfo.getId(), row, 0);
    queryReportCase.getDefaultTableModel().setValueAt(queryInfo.getName(), row, QUERY_NAME_COLUMN_INDEX);
  }

  private void loadAllQueries() {
    List<QueryInfo> queryList = model.getProfileManager().getQueryInfoList();
    for (int i = 0; i < queryList.size(); i++) {
      queryReportCase.getDefaultTableModel().addRow(new Object[0]);
      queryReportCase.getDefaultTableModel().setValueAt(false, i, QUERY_PICK_COLUMN_INDEX);
      queryReportCase.getDefaultTableModel().setValueAt(queryList.get(i).getId(), i, 0);
      queryReportCase.getDefaultTableModel().setValueAt(queryList.get(i).getName(), i, QUERY_NAME_COLUMN_INDEX);
    }
  }

  private void updateQuerySelection() {
    if (queryReportCase.getDefaultTableModel().getRowCount() > 0) {
      queryReportCase.getJxTable().setRowSelectionInterval(0, 0);
    } else {
      clearQueryTable();
    }
  }

  private void markSelectedQueries() {
    if (model.getMapReportData().isEmpty()) {
      return;
    }

    int profileId = getSelectedId(profileReportCase, ProfileColumnNames.ID.getColName());
    int taskId = getSelectedTaskId();

    for (ProfileTaskQueryKey key : model.getMapReportData().keySet()) {
      if (profileId == key.getProfileId() && taskId == key.getTaskId()) {
        markQueryIfSelected(key);
      }
    }
  }

  private void markQueryIfSelected(ProfileTaskQueryKey key) {
    for (int row = 0; row < queryReportCase.getJxTable().getRowCount(); row++) {
      int queryId = getQueryIdAtRow(row);
      if (queryId == key.getQueryId()) {
        queryReportCase.getJxTable().setValueAt(true, row, 0);
      }
    }
  }

  private int getQueryIdAtRow(int row) {
    return (int) queryReportCase.getDefaultTableModel()
        .getValueAt(row, queryReportCase.getDefaultTableModel()
            .findColumn(QueryColumnNames.ID.getColName()));
  }
}