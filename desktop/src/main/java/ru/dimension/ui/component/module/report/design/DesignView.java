package ru.dimension.ui.component.module.report.design;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;
import static ru.dimension.ui.laf.LafColorGroup.REPORT;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.plaf.basic.CalendarHeaderHandler;
import org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.module.ReportChartModule;
import ru.dimension.ui.component.module.report.playground.MetricColumnPanel;
import ru.dimension.ui.component.panel.CollapseCardPanel;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.DesignHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.panel.DateTimePicker;

@Log4j2
@Data
public class DesignView extends JPanel {
  private static final int QUERY_PICK_COLUMN_INDEX = 1;
  private static final int QUERY_NAME_COLUMN_INDEX = 2;
  private static final int MIN_COLUMN_WIDTH = 30;
  private static final int MAX_COLUMN_WIDTH = 35;

  private final DesignModel model;
  private final JSplitPane mainSplitPane;
  private final JSplitPane modelSplitPane;
  private final JSplitPane configChartsSplitPane;
  private final JXTableCase designReportCase;
  private final DefaultCellEditor queryEditor;
  private final JCheckBox collapseCard;
  private final DateTimePicker dateTimePickerFrom;
  private final DateTimePicker dateTimePickerTo;
  private final JLabel lblFrom;
  private final JLabel lblTo;
  private final JButton showButton;
  private final JButton clearButton;
  private final JButton saveButton;
  private final JButton reportButton;
  private final JButton deleteButton;
  private final JXTaskPaneContainer cardContainer;
  private final JXTaskPaneContainer chartContainer;
  private final JScrollPane cardScrollPane;
  private final JScrollPane chartScrollPane;
  private final JScrollPane designListScrollPane;

  private final CollapseCardPanel collapseCardPanel;

  private List<File> designSaveDirs;

  private boolean programmaticDateUpdate = false;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public DesignView(DesignModel model) {
    this.model = model;
    this.mainSplitPane = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 240);
    this.modelSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 250);
    this.configChartsSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 50);
    this.designReportCase = createDesignTable();
    this.queryEditor = new DefaultCellEditor(new JCheckBox());
    this.collapseCard = createCollapseCheckBox();
    this.showButton = createShowButton();
    this.clearButton = createClearButton();
    this.saveButton = createSaveButton();
    this.reportButton = createGenerateReportButton();
    this.deleteButton = createDeleteButton();
    this.lblFrom = new JLabel("From");
    this.lblTo = new JLabel("To");
    this.dateTimePickerFrom = createDateTimePicker();
    this.dateTimePickerTo = createDateTimePicker();
    this.cardContainer = initContainerCard();
    this.cardScrollPane = createCardScrollPane();
    this.chartContainer = initChartContainer();
    this.chartScrollPane = createChartScrollPane();
    this.collapseCardPanel = new CollapseCardPanel();
    this.collapseCardPanel.setCollapseCheckBoxEnabled(false);

    this.designListScrollPane = new JScrollPane(designReportCase.getJScrollPane());

    this.designSaveDirs = new ArrayList<>();

    initDateTimePickers();
    setupDateChangeListeners();
    setupLayout();
  }

  private void setupLayout() {
    loadDesignConfiguration();
    modelSplitPane.setTopComponent(createModelPane());
    modelSplitPane.setBottomComponent(cardScrollPane);
    mainSplitPane.setLeftComponent(modelSplitPane);
    mainSplitPane.setRightComponent(createConfigChartsPanel());
    addMainSplitPaneToThis();
  }

  private void addMainSplitPaneToThis() {
    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(mainSplitPane).fillXY();
    gbl.done();
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
        .cell(reportButton)
        .cell(deleteButton)
        .cell(collapseCardPanel)
        .cell()
        .cell(new JLabel()).fillX();
    gblConfig.done();
    return configPanel;
  }

  private JPanel createModelPane() {
    JPanel panel = new JPanel();
    LaF.setBackgroundConfigPanel(CONFIG_PANEL, panel);
    panel.setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(panel, PGHelper.getPGConfig(), false);
    gbl.row().cell(new JXTitledSeparator("Design")).fillX();
    gbl.row().cell(designReportCase.getJScrollPane()).fillXY();
    gbl.done();

    return panel;
  }

  void loadDesignConfigurationByName(String designName) {
    designReportCase.addRow(new Object[]{designName});

    LocalDateTime dateTime = DesignHelper.parseDesignDate(designName);
    String folderDate = dateTime.format(DesignHelper.getFileFormatFormatter());

    designSaveDirs.add(new File(model.getFilesHelper().getDesignDir(), folderDate));
  }

  void loadDesignConfiguration() {
    designSaveDirs.clear();

    File designFolder = new File(model.getFilesHelper().getDesignDir());

    if (!designFolder.exists() || !designFolder.isDirectory()) {
      return;
    }

    File[] folders = designFolder.listFiles(File::isDirectory);
    if (folders == null) {
      return;
    }

    for (File folder : folders) {
      File[] files = folder.listFiles();
      if (files == null) {
        continue;
      }

      boolean hasJsonFile = Arrays.stream(files)
          .anyMatch(file -> file.isFile() && file.getName().toLowerCase().endsWith(".json"));

      if (hasJsonFile) {
        designSaveDirs.add(folder);
      } else if (files.length == 0) {
        folder.delete(); // Delete empty folders
      }
    }

    Collections.reverse(designSaveDirs);
    designReportCase.clearTable();

    for (File folder : designSaveDirs) {
      try {
        LocalDateTime dateTime = DesignHelper.parseFolderDate(folder.getName());
        String designName = DesignHelper.formatDesignName(dateTime);
        designReportCase.addRow(new Object[]{designName});
      } catch (DateTimeParseException e) {
        log.warn("Invalid folder name format: {}", folder.getName(), e);
      }
    }
  }

  private JXTaskPaneContainer initContainerCard() {
    JXTaskPaneContainer container = new JXTaskPaneContainer();
    LaF.setBackgroundColor(LafColorGroup.REPORT, container);
    container.setBackgroundPainter(null);
    return container;
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

  private void initDateTimePickers() {
    Map.Entry<Date, Date> range = DateHelper.getRangeDate();
    Date defaultFrom = range.getKey();
    Date defaultTo = range.getValue();

    dateTimePickerFrom.setDate(defaultFrom);
    dateTimePickerTo.setDate(defaultTo);

    model.getDesignDateRanges().put("default", Map.entry(defaultFrom, defaultTo));
  }

  public void setDatesWithoutValidation(Date from, Date to) {
    programmaticDateUpdate = true;
    dateTimePickerFrom.setDate(from);
    dateTimePickerTo.setDate(to);
    programmaticDateUpdate = false;
  }

  private void setupDateChangeListeners() {
    PropertyChangeListener dateChangeListener = evt -> {
      if (programmaticDateUpdate || !"date".equals(evt.getPropertyName())) {
        return;
      }

      String currentDesign = model.getLoadedDesignFolder();
      if (currentDesign.isEmpty()) currentDesign = "default";

      Entry<Date, Date> lastRange = model.getDesignDateRanges().get(currentDesign);
      if (lastRange == null) {
        lastRange = model.getDesignDateRanges().get("default");
        if (lastRange == null) {
          lastRange = Map.entry(new Date(), new Date());
        }
      }

      Date lastValidFrom = lastRange.getKey();
      Date lastValidTo = lastRange.getValue();

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

      model.getDesignDateRanges().put(currentDesign, Map.entry(candidateFrom, candidateTo));
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

  private JButton createDeleteButton() {
    JButton button = new JButton("Delete");
    button.setToolTipText("Delete saved design");
    button.setMnemonic('D');
    button.setEnabled(false);
    return button;
  }

  private JButton createGenerateReportButton() {
    JButton button = new JButton("Report");
    button.setToolTipText("Generate report");
    button.setMnemonic('T');
    button.setEnabled(false);
    return button;
  }

  private JButton createSaveButton() {
    JButton button = new JButton("Save");
    button.setToolTipText("Save design");
    button.setMnemonic('S');
    button.setEnabled(false);
    return button;
  }

  private JXTableCase createDesignTable() {
    return GUIHelper.getJXTableCase(7, new String[]{"Design name"});
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

  public void updateButtonStates() {
    boolean hasCharts = model.hasCharts();
    boolean hasCards = !model.getMapReportData().isEmpty();

    showButton.setEnabled(hasCharts);
    clearButton.setEnabled(hasCharts);
    reportButton.setEnabled(hasCharts);
    deleteButton.setEnabled(hasCharts);
    collapseCardPanel.setCollapseCheckBoxEnabled(hasCharts);

    collapseCard.setVisible(hasCards);
  }

  public void setEnabledButton(boolean show, boolean clear, boolean save, boolean report, boolean delete) {
    showButton.setEnabled(show);
    clearButton.setEnabled(clear);
    saveButton.setEnabled(save);
    reportButton.setEnabled(report);
    deleteButton.setEnabled(delete);
  }

  public MetricColumnPanel getCardComponent(ProfileTaskQueryKey key) {
    return Arrays.stream(cardContainer.getComponents())
        .filter(MetricColumnPanel.class::isInstance)
        .map(MetricColumnPanel.class::cast)
        .filter(card -> key.equals(card.getKey()))
        .findFirst()
        .orElseGet(() -> new MetricColumnPanel(
            Component.DESIGN,
            key,
            model.getProfileManager(),
            collapseCard,
            cardContainer));
  }
}