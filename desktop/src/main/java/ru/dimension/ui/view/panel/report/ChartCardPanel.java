package ru.dimension.ui.view.panel.report;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;
import static ru.dimension.ui.model.SourceConfig.COLUMNS;
import static ru.dimension.ui.model.SourceConfig.METRICS;
import static ru.dimension.ui.model.function.ChartType.LINEAR;
import static ru.dimension.ui.model.function.ChartType.STACKED;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.db.model.profile.table.BType;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXTextArea;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.SeriesExceedException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.ReportHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.SourceConfig;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.TimestampType;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.report.QueryReportData;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.view.action.RadioButtonActionExecutor;
import ru.dimension.ui.view.analyze.chart.ChartConfig;
import ru.dimension.ui.view.analyze.chart.SCP;
import ru.dimension.ui.view.analyze.chart.history.HistorySCP;
import ru.dimension.ui.view.chart.DetailChart;
import ru.dimension.ui.view.chart.HelperChart;
import ru.dimension.ui.view.detail.DetailDashboardPanel;

@Data
@Log4j2
public class ChartCardPanel extends JPanel implements HelperChart {

  @EqualsAndHashCode.Include
  private final ProfileTaskQueryKey key;
  private final JXLabel jlTitle;
  private final JXTextArea jtaDescription;
  private final JSplitPane jSplitPane;

  private final ButtonGroupFunction buttonGroupFunction;
  private final JPanel jPanelFunction;
  protected MetricFunction metricFunctionOnEdit = MetricFunction.NONE;
  private final Metric metric;

  protected ExecutorService executorService;
  protected final ProfileManager profileManager;
  protected final EventListener eventListener;
  protected final DStore fStore;
  private final ReportHelper reportHelper;

  private int id;
  private ChartInfo chartInfo;
  private SourceConfig sourceConfig;

  private final Map<ProfileTaskQueryKey, QueryReportData> mapReportData;
  private String descriptionFrom;
  private String descriptionTo;

  private final ResourceBundle bundleDefault;

  public ChartCardPanel(int id,
                        ChartInfo chartInfo,
                        ProfileTaskQueryKey key,
                        SourceConfig sourceConfig,
                        Metric metric,
                        ProfileManager profileManager,
                        EventListener eventListener,
                        DStore fStore,
                        ReportHelper reportHelper,
                        Map<ProfileTaskQueryKey, QueryReportData> mapReportData) {
    this.profileManager = profileManager;
    this.eventListener = eventListener;
    this.fStore = fStore;
    this.executorService = Executors.newSingleThreadExecutor();
    this.reportHelper = reportHelper;
    this.mapReportData = mapReportData;

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.id = id;
    this.key = key;
    this.chartInfo = chartInfo;
    this.sourceConfig = sourceConfig;

    this.metric = metric;

    this.jlTitle = new JXLabel(getTitle(sourceConfig));
    jlTitle.setForeground(new Color(0x0A8D0A));
    Font font = jlTitle.getFont();
    Font newFont = font.deriveFont(Font.PLAIN, 14);
    jlTitle.setFont(newFont);

    this.setToolTipText(getTooltip());

    buttonGroupFunction = new ButtonGroupFunction();

    buttonGroupFunction.getCount().addActionListener(new RadioListenerColumn());
    buttonGroupFunction.getSum().addActionListener(new RadioListenerColumn());
    buttonGroupFunction.getAverage().addActionListener(new RadioListenerColumn());

    this.jtaDescription = GUIHelper.getJXTextArea(2, 1);
    this.jtaDescription.setPrompt("Enter a comment...");

    this.jSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 250);

    JPanel bottom = new JPanel();
    bottom.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, bottom);

    JPanel top = new JPanel();
    top.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, top);

    jSplitPane.setBottomComponent(bottom);
    jSplitPane.setTopComponent(top);
    jSplitPane.setDividerLocation(250);

    jSplitPane.setResizeWeight(0.5);
    jSplitPane.setContinuousLayout(true);
    bottom.setMaximumSize(new Dimension(680, 100));
    bottom.setMinimumSize(new Dimension(680, 300));
    bottom.setPreferredSize(new Dimension(680, 200));

    top.setMaximumSize(new Dimension(680, 100));
    top.setMinimumSize(new Dimension(680, 300));
    top.setPreferredSize(new Dimension(680, 200));

    this.setBorder(new EtchedBorder());

    JPanel labelPanel = new JPanel();
    PainlessGridBag gblLabel = new PainlessGridBag(labelPanel, PGHelper.getPGConfig(0), false);

    JPanel radioBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    radioBtnPanel.add(buttonGroupFunction.getCount());
    radioBtnPanel.add(buttonGroupFunction.getSum());
    radioBtnPanel.add(buttonGroupFunction.getAverage());

    gblLabel.row()
        .cell(jlTitle)
        .cellXRemainder(radioBtnPanel).fillX();
    gblLabel.done();

    this.jPanelFunction = new JPanel();
    PainlessGridBag gblFunction = new PainlessGridBag(jPanelFunction, PGHelper.getPGConfig(0), false);
    gblFunction.row()
        .cell(new JXTitledSeparator("Description")).fillX();
    gblFunction.row()
        .cell(new JScrollPane(jtaDescription)).fillX();

    setEnabled(true);

    gblFunction.done();

    PainlessGridBag gblChart = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    gblChart.row()
        .cell(labelPanel).fillX();
    gblChart.row()
        .cell(jPanelFunction).fillX();
    gblChart.row()
        .cell(jSplitPane).fillXY();
    gblChart.done();

    JPopupMenu popupMenu = new JPopupMenu();
    JMenuItem insertItem = new JMenuItem("Insert");
    JMenuItem copyItem = new JMenuItem("Copy");
    JMenuItem cutItem = new JMenuItem("Cut");
    JMenuItem selectAllItem = new JMenuItem("Select All");

    insertItem.addActionListener(e -> {
      String clipboardText = getClipboardText();
      if (clipboardText != null) {
        int caretPosition = jtaDescription.getCaretPosition();
        jtaDescription.insert(clipboardText, caretPosition);
      }
    });

    copyItem.addActionListener(e -> jtaDescription.copy());

    cutItem.addActionListener(e -> jtaDescription.cut());

    selectAllItem.addActionListener(e -> jtaDescription.selectAll());

    popupMenu.add(insertItem);
    popupMenu.add(copyItem);
    popupMenu.add(cutItem);
    popupMenu.addSeparator();
    popupMenu.add(selectAllItem);

    jtaDescription.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        showPopupMenu(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        showPopupMenu(e);
      }

      private void showPopupMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
          popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    });

    this.getJtaDescription().addFocusListener(new FocusListener() {
      @Override
      public void focusLost(FocusEvent focusEvent) {
        descriptionTo = jtaDescription.getText();
        if (!descriptionFrom.trim().equals(descriptionTo.trim())) {
          reportHelper.setMadeChanges(true);
        }
      }

      @Override
      public void focusGained(FocusEvent focusEvent) {
        descriptionFrom = jtaDescription.getText();
      }
    });

  }

  private String getClipboardText() {
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    Transferable contents = clipboard.getContents(null);
    if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      try {
        return (String) contents.getTransferData(DataFlavor.stringFlavor);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  public void setEnabled(boolean flag) {
    buttonGroupFunction.getCount().setEnabled(flag);
    buttonGroupFunction.getSum().setEnabled(flag);
    buttonGroupFunction.getAverage().setEnabled(flag);
  }

  public void setSelectedRadioButton(MetricFunction metricFunction) {
    buttonGroupFunction.setSelectedRadioButton(metricFunction);
  }

  public class RadioListenerColumn implements ActionListener {

    public RadioListenerColumn() {
    }

    public void actionPerformed(ActionEvent e) {
      JRadioButton button = (JRadioButton) e.getSource();

      RadioButtonActionExecutor.execute(button, metricFunction -> {
        metricFunctionOnEdit = metricFunction;
        loadChart(id, chartInfo, key, ChartCardPanel.this, sourceConfig);
      });
    }
  }

  public void loadChart(int cId,
                        ChartInfo chartInfo,
                        ProfileTaskQueryKey key,
                        ChartCardPanel cardChart,
                        SourceConfig sourceConfig) {

    QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
    TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
    if (Objects.isNull(tableInfo)) {
      throw new NotFoundException(String.format("Table info with id=%s not found",
                                                queryInfo.getName()));
    }

    this.checkTimestampColumn(tableInfo);

    if (METRICS.equals(sourceConfig)) {
      this.loadChartMetric(cId, chartInfo, key, cardChart);
    } else if (COLUMNS.equals(sourceConfig)) {
      this.loadChartColumn(cId, chartInfo, key, cardChart);
    }

    log.info("Query: " + key.getQueryId());
  }

  private void loadChartMetric(int metricId,
                               ChartInfo chartInfo,
                               ProfileTaskQueryKey key,
                               ChartCardPanel cardChart) {

    QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
    TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
    if (Objects.isNull(tableInfo)) {
      throw new NotFoundException(String.format("Table info with id=%s not found",
                                                queryInfo.getName()));
    }

    Metric metric = queryInfo.getMetricList()
        .stream().filter(f -> f.getId() == metricId)
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found metric by id: " + metricId));

    executorService.submit(() -> {
      try {
        GUIHelper.addToJSplitPane(cardChart.getJSplitPane(),
                                  createProgressBar("Loading, please wait..."), JSplitPane.TOP, 200);

        loadChartByMetric(metric, queryInfo, key, chartInfo, tableInfo, cardChart);

        cardChart.repaint();
        cardChart.revalidate();
      } catch (SeriesExceedException exceed) {
        log.catching(exceed);
        GUIHelper.addToJSplitPaneProgressBar(cardChart.getJSplitPane(), exceed.getMessage(), JSplitPane.TOP, 200);
      } catch (Exception exception) {
        log.catching(exception);
        GUIHelper.addToJSplitPaneProgressBar(cardChart.getJSplitPane(), bundleDefault.getString("sysErrMsg"), JSplitPane.TOP, 200);
      }
    });

    log.info("Metric id: " + metricId);
  }

  private void loadChartColumn(int cProfileId,
                               ChartInfo chartInfo,
                               ProfileTaskQueryKey key,
                               ChartCardPanel cardChart) {

    QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
    TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
    if (Objects.isNull(tableInfo)) {
      throw new NotFoundException(String.format("Table info with id=%s not found",
                                                queryInfo.getName()));
    }

    executorService.submit(() -> {
      try {

        GUIHelper.addToJSplitPane(cardChart.getJSplitPane(),
                                  createProgressBar("Loading, please wait..."), JSplitPane.TOP, 200);

        CProfile cProfile = tableInfo.getCProfiles()
            .stream()
            .filter(f -> f.getColId() == cProfileId)
            .findAny()
            .orElseThrow(() -> new NotFoundException("Not found CProfile: " + cProfileId));

        Metric metric = getMetricByCProfile(cProfile, tableInfo);

        setMetricFunctionAndChartType(metric);

        tableInfo.setBackendType(BType.BERKLEYDB);

        loadChartByMetric(metric, queryInfo, key, chartInfo, tableInfo, cardChart);

        cardChart.repaint();
        cardChart.revalidate();
      } catch (SeriesExceedException exceed) {
        log.catching(exceed);
        GUIHelper.addToJSplitPaneProgressBar(cardChart.getJSplitPane(), exceed.getMessage(), JSplitPane.TOP, 200);
      } catch (Exception exception) {
        log.catching(exception);
        GUIHelper.addToJSplitPaneProgressBar(cardChart.getJSplitPane(), bundleDefault.getString("sysErrMsg"), JSplitPane.TOP, 200);
      }
    });

    log.info("Column profile id: " + cProfileId);
  }

  protected Metric getMetricByCProfile(CProfile cProfile,
                                       TableInfo tableInfo) {
    Metric metric = new Metric();
    metric.setXAxis(tableInfo.getCProfiles().stream().filter(f -> f.getCsType().isTimeStamp()).findAny().orElseThrow());
    metric.setYAxis(cProfile);
    metric.setGroup(cProfile);

    if (MetricFunction.NONE.equals(metricFunctionOnEdit)) {
      setMetricFunctionAndChartNotEdit(cProfile, metric);
    } else {
      setMetricFunctionAndChartType(metric);
    }

    return metric;
  }

  private void setMetricFunctionAndChartType(Metric metric) {
    switch (metricFunctionOnEdit) {
      case COUNT -> {
        metric.setMetricFunction(metricFunctionOnEdit);
        metric.setChartType(STACKED);
      }
      case SUM, AVG -> {
        metric.setMetricFunction(metricFunctionOnEdit);
        metric.setChartType(LINEAR);
      }
    }
  }

  private void setMetricFunctionAndChartNotEdit(CProfile cProfile,
                                                Metric metric) {
    if (CType.STRING.equals(cProfile.getCsType().getCType())) {
      metric.setMetricFunction(MetricFunction.COUNT);
      metric.setChartType(STACKED);
    } else {
      if (Arrays.stream(TimestampType.values()).anyMatch((t) -> t.name().equals(cProfile.getColDbTypeName()))) {
        metric.setMetricFunction(MetricFunction.COUNT);
        metric.setChartType(STACKED);
      } else {
        metric.setMetricFunction(MetricFunction.AVG);
        metric.setChartType(LINEAR);
      }
    }
  }

  private void loadChartByMetric(Metric metric,
                                 QueryInfo queryInfo,
                                 ProfileTaskQueryKey key,
                                 ChartInfo chartInfo,
                                 TableInfo tableInfo,
                                 ChartCardPanel cardChart) {

    SCP chartPanel = getChartPanel(metric, queryInfo, key, chartInfo);
    chartPanel.initialize();

    DetailDashboardPanel detailPanel =
        getDetail(fStore, metric, queryInfo, tableInfo, chartPanel.getSeriesColorMap(), chartPanel, chartPanel.getSeriesType());

    GUIHelper.addToJSplitPane(cardChart.getJSplitPane(), chartPanel, JSplitPane.TOP, 200);
    GUIHelper.addToJSplitPane(cardChart.getJSplitPane(), detailPanel, JSplitPane.BOTTOM, 200);
  }

  @NotNull
  private SCP getChartPanel(Metric metric,
                     QueryInfo queryInfo,
                     ProfileTaskQueryKey key,
                     ChartInfo chartInfo) {
    ChartConfig config = buildChartConfig(key, metric, chartInfo, queryInfo);

    config.getChartInfo().setCustomBegin(chartInfo.getCustomBegin());
    config.getChartInfo().setCustomEnd(chartInfo.getCustomEnd());

    return new HistorySCP(fStore, config, key, null);
  }

  private DetailDashboardPanel getDetail(DStore fStore,
                                         Metric metric,
                                         QueryInfo queryInfo,
                                         TableInfo tableInfo,
                                         Map<String, Color> seriesColorMap,
                                         DetailChart dynamicChart,
                                         SeriesType seriesType) {
    DetailDashboardPanel detailPanel =
        new DetailDashboardPanel(fStore, queryInfo, tableInfo, metric, seriesColorMap, ProcessType.HISTORY, seriesType);

    dynamicChart.addChartListenerReleaseMouse(detailPanel);
    return detailPanel;
  }

  private ChartConfig buildChartConfig(ProfileTaskQueryKey key,
                                       Metric metric,
                                       ChartInfo chartInfo,
                                       QueryInfo queryInfo) {
    ChartConfig config = new ChartConfig();

    ChartKey chartKey = new ChartKey(key, metric.getYAxis());
    Metric metricCopy = metric.copy();
    ChartInfo chartInfoCopy = chartInfo.copy();

    metricCopy.setMetricFunction(metric.getMetricFunction());
    metricCopy.setChartType(MetricFunction.COUNT.equals(metric.getMetricFunction()) ? ChartType.STACKED : ChartType.LINEAR);

    chartInfoCopy.setRangeHistory(RangeHistory.DAY);

    config.setChartKey(chartKey);

    config.setTitle("");
    config.setXAxisLabel(metric.getYAxis().getColName());
    config.setYAxisLabel("Value");
    config.setMetric(metricCopy);
    config.setChartInfo(chartInfoCopy);
    config.setQueryInfo(queryInfo);

    return config;
  }

  private void checkTimestampColumn(TableInfo tableInfo) {
    if (Objects.isNull(tableInfo.getCProfiles())) {
      throw new NotFoundException("Metadata not found, need to reload query metadata in configuration");
    }

    tableInfo.getCProfiles().stream()
        .filter(f -> f.getCsType().isTimeStamp())
        .findAny()
        .orElseThrow(
            () -> new NotFoundException("Not found column timestamp for table: " + tableInfo.getTableName()));
  }

  private String getTitle(SourceConfig sourceConfig) {
    if (sourceConfig == SourceConfig.METRICS) {
      return "<html><b>Metric: </b>" + metric.getName() + "</html>";
    } else if (sourceConfig == COLUMNS) {
      return "<html><b>Column: </b>" + metric.getName() + "</html>";
    }

    return "";
  }

  private String getTooltip() {
    return "<html><b>Metric:</b> " + metric.getName() + " <br>"
        + "  <b> Y Axis: </b> " + metric.getYAxis().getColName()
        + ";  <b>Group:</b>" + metric.getGroup().getColName() +
        ";  <b>Chart:</b> " + metric.getChartType() + " </html>";
  }
}
