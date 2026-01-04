package ru.dimension.ui.component.module.report.playground;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;
import static ru.dimension.ui.laf.LafColorGroup.REPORT;

import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.plaf.basic.CalendarHeaderHandler;
import org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swingx.JXTableTables;
import ru.dimension.ui.component.module.chart.ReportChartModule;
import ru.dimension.ui.component.panel.CollapseCardPanel;
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
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.panel.DateTimePicker;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.PickableQueryRow;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Data
@EqualsAndHashCode(callSuper = true)
public class PlaygroundView extends JPanel implements ListSelectionListener {

  private final PlaygroundModel model;
  private final JSplitPane mainSplitPane;
  private final JSplitPane modelSplitPane;
  private final JSplitPane configChartsSplitPane;

  private final TTRegistry registry;
  private final TTTable<ProfileRow, JXTable> profileTable;
  private final TTTable<TaskRow, JXTable> taskTable;
  private final TTTable<PickableQueryRow, JXTable> queryTable;

  private int queryPickColumnIndex = -1;
  private boolean ignoreCheckboxEvents = false;

  public interface QueryCheckboxChangeListener {
    void onQueryCheckboxChanged(PickableQueryRow pickableQueryRow, boolean selected);
  }

  @Setter
  private QueryCheckboxChangeListener queryCheckboxChangeListener;

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

  private final CollapseCardPanel collapseCardPanel;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public PlaygroundView(PlaygroundModel model) {
    this.model = model;

    this.registry = TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();

    this.mainSplitPane = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 240);
    this.modelSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 430);
    this.configChartsSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 50);

    this.profileTable = createProfileTable();
    this.taskTable = createTaskTable();
    this.queryTable = createQueryTable();

    setupQueryTableListener();

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
    this.collapseCardPanel = new CollapseCardPanel();
    this.collapseCardPanel.setCollapseCheckBoxEnabled(false);

    initDateTimePickers();
    setupDateChangeListeners();
    setupSelectionListeners();
    setupLayout();
  }

  private TTTable<ProfileRow, JXTable> createProfileTable() {
    TTTable<ProfileRow, JXTable> tt = JXTableTables.create(
        registry,
        ProfileRow.class,
        TableUi.<ProfileRow>builder()
            .rowIcon(ModelIconProviders.forProfileRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));
    table.setEditable(false);

    if (table.getColumnExt("ID") != null) {
      table.getColumnExt("ID").setVisible(false);
    }

    return tt;
  }

  private TTTable<TaskRow, JXTable> createTaskTable() {
    TTTable<TaskRow, JXTable> tt = JXTableTables.create(
        registry,
        TaskRow.class,
        TableUi.<TaskRow>builder()
            .rowIcon(ModelIconProviders.forTaskRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));
    table.setEditable(false);

    if (table.getColumnExt("ID") != null) {
      table.getColumnExt("ID").setVisible(false);
    }

    return tt;
  }

  private TTTable<PickableQueryRow, JXTable> createQueryTable() {
    TTTable<PickableQueryRow, JXTable> tt = JXTableTables.create(
        registry,
        PickableQueryRow.class,
        TableUi.<PickableQueryRow>builder()
            .rowIcon(ModelIconProviders.forPickableQueryRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));
    table.setEditable(true);

    if (table.getColumnExt("ID") != null) {
      table.getColumnExt("ID").setVisible(false);
    }

    if (table.getColumnExt("Name") != null) {
      table.getColumnExt("Name").setEditable(false);
    }

    queryPickColumnIndex = tt.model().schema().modelIndexOf("pick");

    return tt;
  }

  private void setupQueryTableListener() {
    if (queryPickColumnIndex < 0) {
      return;
    }

    queryTable.model().addTableModelListener(e -> {
      if (ignoreCheckboxEvents || e.getType() != TableModelEvent.UPDATE) {
        return;
      }

      if (e.getColumn() == queryPickColumnIndex) {
        int row = e.getFirstRow();
        if (row >= 0 && row < queryTable.model().getRowCount()) {
          PickableQueryRow item = queryTable.model().itemAt(row);
          if (item != null && queryCheckboxChangeListener != null) {
            queryCheckboxChangeListener.onQueryCheckboxChanged(item, item.isPick());
          }
        }
      }
    });
  }

  public void setQuerySelected(int queryId, boolean selected) {
    ignoreCheckboxEvents = true;
    try {
      for (int i = 0; i < queryTable.model().getRowCount(); i++) {
        PickableQueryRow row = queryTable.model().itemAt(i);
        if (row != null && row.getId() == queryId) {
          queryTable.model().setValueAt(selected, i, queryPickColumnIndex);
          break;
        }
      }
    } finally {
      ignoreCheckboxEvents = false;
    }
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
    javax.swing.UIManager.put(CalendarHeaderHandler.uiControllerID, SpinningCalendarHeaderHandler.class.getName());
    picker.getMonthView().setZoomable(true);
    return picker;
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
    profileTable.table().getSelectionModel().addListSelectionListener(this);
    taskTable.table().getSelectionModel().addListSelectionListener(this);
    queryTable.table().getSelectionModel().addListSelectionListener(this);
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
    gbl.row().cell(profileTable.scrollPane()).fillXY();
    gbl.row().cell(new JXTitledSeparator("Task")).fillX();
    gbl.row().cell(taskTable.scrollPane()).fillXY();
    gbl.row().cell(new JXTitledSeparator("Query")).fillX();
    gbl.row().cell(queryTable.scrollPane()).fillXY();
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
        .cell(collapseCardPanel)
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
    List<ProfileRow> profileRows = model.getConfigurationManager()
        .getConfigList(Profile.class)
        .stream()
        .map(profile -> new ProfileRow(profile.getId(), profile.getName()))
        .collect(Collectors.toList());

    profileTable.setItems(profileRows);

    if (!profileRows.isEmpty()) {
      profileTable.table().setRowSelectionInterval(0, 0);
    }
  }

  public ProfileTaskQueryKey getProfileTaskQueryKey() {
    int profileRowIndex = profileTable.table().getSelectedRow();
    if (profileRowIndex < 0) {
      throw new IllegalStateException("No profile selected");
    }
    ProfileRow profileRow = profileTable.model().itemAt(profileRowIndex);
    int profileId = profileRow.getId();

    int taskRowIndex = taskTable.table().getSelectedRow();
    if (taskRowIndex < 0) {
      throw new IllegalStateException("No task selected");
    }
    TaskRow taskRow = taskTable.model().itemAt(taskRowIndex);
    int taskId = taskRow.getId();

    int queryRowIndex = queryTable.table().getSelectedRow();
    if (queryRowIndex < 0) {
      throw new IllegalStateException("No query selected");
    }
    PickableQueryRow pickableQueryRow = queryTable.model().itemAt(queryRowIndex);
    int queryId = pickableQueryRow.getId();

    return new ProfileTaskQueryKey(profileId, taskId, queryId);
  }

  public PickableQueryRow getSelectedQueryRow() {
    int queryRowIndex = queryTable.table().getSelectedRow();
    if (queryRowIndex < 0) {
      return null;
    }
    return queryTable.model().itemAt(queryRowIndex);
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
    ignoreCheckboxEvents = true;
    try {
      for (int i = 0; i < queryTable.model().getRowCount(); i++) {
        queryTable.model().setValueAt(false, i, queryPickColumnIndex);
      }

      chartContainer.removeAll();
      chartContainer.revalidate();
      chartContainer.repaint();
    } finally {
      ignoreCheckboxEvents = false;
    }
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

    if (e.getSource() == profileTable.table().getSelectionModel()) {
      handleProfileSelection();
    } else if (e.getSource() == taskTable.table().getSelectionModel()) {
      handleTaskSelection();
    }
  }

  public void updateButtonStates() {
    boolean hasCharts = model.hasCharts();
    boolean hasCards = !model.getMapReportData().isEmpty();

    showButton.setEnabled(hasCharts);
    clearButton.setEnabled(hasCharts);
    saveButton.setEnabled(hasCharts);
    collapseCardPanel.setCollapseCheckBoxEnabled(hasCharts);

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
    taskTable.setItems(Collections.emptyList());
  }

  private int getSelectedProfileId() {
    int rowIndex = profileTable.table().getSelectedRow();
    if (rowIndex < 0) {
      return -1;
    }
    ProfileRow row = profileTable.model().itemAt(rowIndex);
    return row != null ? row.getId() : -1;
  }

  private void showProfileNotSelectedError() {
    JOptionPane.showMessageDialog(null,
                                  "Profile is not selected",
                                  "General Error",
                                  JOptionPane.ERROR_MESSAGE);
  }

  private void loadTasksForProfile(int profileId) {
    List<TaskRow> taskRows = model.getProfileManager()
        .getProfileInfoById(profileId)
        .getTaskInfoList()
        .stream()
        .map(taskId -> {
          TaskInfo taskInfo = model.getProfileManager().getTaskInfoById(taskId);
          if (taskInfo == null) {
            throw new NotFoundException("Not found task: " + taskId);
          }
          return new TaskRow(taskInfo.getId(), taskInfo.getName());
        })
        .collect(Collectors.toList());

    taskTable.setItems(taskRows);
  }

  private void updateTaskSelection() {
    if (taskTable.model().getRowCount() > 0) {
      taskTable.table().setRowSelectionInterval(0, 0);
    } else {
      clearQueryTable();
    }
  }

  private void clearQueryTable() {
    queryTable.setItems(Collections.emptyList());
  }

  private void handleTaskSelection() {
    clearQueryTable();
    int taskId = getSelectedTaskId();

    if (taskTable.model().getRowCount() > 0) {
      loadQueriesForTask(taskId);
    } else {
      loadAllQueries();
    }

    updateQuerySelection();
    markSelectedQueries();
  }

  private int getSelectedTaskId() {
    int rowIndex = taskTable.table().getSelectedRow();
    if (rowIndex < 0) {
      return -1;
    }
    TaskRow row = taskTable.model().itemAt(rowIndex);
    return row != null ? row.getId() : -1;
  }

  private void loadQueriesForTask(int taskId) {
    TaskInfo taskInfo = model.getProfileManager().getTaskInfoById(taskId);
    if (taskInfo == null) {
      throw new NotFoundException("Not found task: " + taskId);
    }

    List<PickableQueryRow> pickableQueryRows = taskInfo.getQueryInfoList()
        .stream()
        .map(queryId -> {
          QueryInfo queryInfo = model.getProfileManager().getQueryInfoById(queryId);
          if (queryInfo == null) {
            throw new NotFoundException("Not found query: " + queryId);
          }
          return new PickableQueryRow(queryInfo.getId(), queryInfo.getName(), false);
        })
        .collect(Collectors.toList());

    queryTable.setItems(pickableQueryRows);
  }

  private void loadAllQueries() {
    List<PickableQueryRow> pickableQueryRows = model.getProfileManager()
        .getQueryInfoList()
        .stream()
        .map(queryInfo -> new PickableQueryRow(queryInfo.getId(), queryInfo.getName(), false))
        .collect(Collectors.toList());

    queryTable.setItems(pickableQueryRows);
  }

  private void updateQuerySelection() {
    if (queryTable.model().getRowCount() > 0) {
      queryTable.table().setRowSelectionInterval(0, 0);
    }
  }

  private void markSelectedQueries() {
    if (model.getMapReportData().isEmpty()) {
      return;
    }

    int profileId = getSelectedProfileId();
    int taskId = getSelectedTaskId();

    ignoreCheckboxEvents = true;
    try {
      for (ProfileTaskQueryKey key : model.getMapReportData().keySet()) {
        if (profileId == key.getProfileId() && taskId == key.getTaskId()) {
          markQueryIfSelected(key);
        }
      }
    } finally {
      ignoreCheckboxEvents = false;
    }
  }

  private void markQueryIfSelected(ProfileTaskQueryKey key) {
    for (int row = 0; row < queryTable.model().getRowCount(); row++) {
      PickableQueryRow pickableQueryRow = queryTable.model().itemAt(row);
      if (pickableQueryRow != null && pickableQueryRow.getId() == key.getQueryId()) {
        queryTable.model().setValueAt(true, row, queryPickColumnIndex);
        break;
      }
    }
  }
}