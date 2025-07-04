package ru.dimension.ui.view.handler.adhoc;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;
import static ru.dimension.ui.model.SourceConfig.COLUMNS;
import static ru.dimension.ui.model.SourceConfig.METRICS;
import static ru.dimension.ui.model.view.ProcessType.ADHOC;
import static ru.dimension.ui.model.view.ProcessType.ADHOC_ANALYZE;
import static ru.dimension.ui.model.view.ProcessTypeWorkspace.VISUALIZE;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.column.ProfileColumnNames;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.analyze.CustomAction;
import ru.dimension.ui.view.chart.DetailChart;
import ru.dimension.ui.view.chart.HelperChart;
import ru.dimension.ui.view.handler.MouseListenerImpl;
import ru.dimension.ui.view.panel.adhoc.AdHocPanel;
import ru.dimension.ui.view.structure.workspace.query.DetailsControlPanel;
import ru.dimension.ui.view.tab.AdHocTab;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.SourceConfig;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.TimestampType;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.analyze.chart.adhoc.AdHocSCP;
import ru.dimension.ui.view.analyze.chart.adhoc.StackChartAdHocPanel;
import ru.dimension.ui.view.analyze.panel.AnalyzeAdHocPanel;
import ru.dimension.ui.view.chart.holder.DetailAndAnalyzeHolder;
import ru.dimension.ui.view.action.RadioButtonActionExecutor;
import ru.dimension.ui.view.detail.DetailAdHocPanel;

@Log4j2
public abstract class AdHocChartHandler extends MouseListenerImpl implements HelperChart {
  static int LOCATION_SIZE = 250;

  protected AdHocPanel adHocPanel;
  protected AdHocTab adHocTab;

  protected QueryInfo queryInfo;
  protected ChartInfo chartInfo;
  protected TableInfo tableInfo;

  protected SourceConfig sourceConfig = COLUMNS;

  protected final JXTableCase timestampCase;
  protected final JXTableCase metricCase;
  protected final JXTableCase columnCase;

  protected JSplitPane visualizeSplitPane;
  protected JPanel analyzePanel;

  protected ExecutorService executorService;

  protected final ResourceBundle bundleDefault;

  protected Color colorBlack;
  protected Color colorBlue;

  protected MetricFunction metricFunctionOnEdit = MetricFunction.NONE;

  protected DetailsControlPanel detailsControlPanel;

  protected EventListener eventListener;

  protected ProfileManager profileManager;

  protected JXTableCase connectionCase;
  protected JXTableCase tableCase;

  public AdHocChartHandler(AdHocPanel adHocPanel,
                           JXTableCase metricCase,
                           JXTableCase timestampCase,
                           JXTableCase columnCase,
                           JXTableCase connectionCase,
                           JXTableCase tableCase) {
    this.adHocPanel = adHocPanel;
    this.adHocTab = adHocPanel.getAdHocTab();

    this.timestampCase = timestampCase;
    this.metricCase = metricCase;
    this.columnCase = columnCase;
    this.connectionCase = connectionCase;
    this.tableCase = tableCase;

    this.detailsControlPanel = adHocPanel.getDetailsControlPanel();
    this.visualizeSplitPane = adHocPanel.getVisualizeSplitPane();
    this.analyzePanel = adHocPanel.getAnalyzePanel();

    this.queryInfo = new QueryInfo();
    this.chartInfo = new ChartInfo();
    this.tableInfo = new TableInfo();

    this.executorService = Executors.newSingleThreadExecutor();

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.colorBlack = Color.BLACK;
    this.colorBlue = (Color) bundleDefault.getObject("colorBlue");

    this.detailsControlPanel.getButtonGroupFunction().getCount().addActionListener(new RadioListenerColumn());
    this.detailsControlPanel.getButtonGroupFunction().getSum().addActionListener(new RadioListenerColumn());
    this.detailsControlPanel.getButtonGroupFunction().getAverage().addActionListener(new RadioListenerColumn());

    this.metricCase.getJxTable().addMouseListener(this);
    this.columnCase.getJxTable().addMouseListener(this);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    JXTable source = (JXTable) e.getSource();

    this.setSourceConfig(source);

    log.info("Source config: " + sourceConfig);
  }

  protected void setSourceConfig(JXTable source) {
    if (source.equals(metricCase.getJxTable())) {
      sourceConfig = METRICS;
    }

    if (source.equals(columnCase.getJxTable())) {
      sourceConfig = COLUMNS;
    }

    log.info("Source config: " + sourceConfig);
  }

  protected void loadChart(ProcessType processType,
                           DStore dStore,
                           TableInfo tableInfo,
                           QueryInfo queryInfo) {
    log.info("Load chart method call");

    this.adHocTab.setSelectedTab(VISUALIZE);

    if (COLUMNS.equals(sourceConfig)) {
      this.loadChartColumn(processType, dStore, tableInfo, queryInfo);
    } else if (METRICS.equals(sourceConfig)) {
      log.info("Metrics not supported yet");
    }
  }

  private void loadChartColumn(ProcessType processType,
                               DStore dStore,
                               TableInfo tableInfo,
                               QueryInfo queryInfo) {
    CProfile cProfile = getCProfileFromUI(tableInfo);

    executorService.submit(() -> {
      fillPanelWithProgressBar(processType);

      try {
        Metric metric = getMetricByCProfile(cProfile, tableInfo);

        setMetricFunctionAndChartType(metric);

        SeriesType seriesType = SeriesType.COMMON;

        if (MetricFunction.COUNT.equals(metric.getMetricFunction())) {
          ChartRange chartRange = getChartRange(dStore, tableInfo.getTableName(), chartInfo);

          List<String> distinct = getDistinct(dStore, tableInfo.getTableName(), metric.getYAxis(), chartRange, THRESHOLD_SERIES);

          if (distinct.size() == THRESHOLD_SERIES) {
            log.info("Column data series size exceeds threshold value = " + THRESHOLD_SERIES);
            seriesType = SeriesType.CUSTOM;
          } else {
            log.info("Column data series size = " + distinct.size());
          }
        }

        loadChartByMetric(metric, processType, dStore, tableInfo, queryInfo, seriesType);
      } catch (Exception exception) {
        cleanAllPanels();

        DialogHelper.showErrorDialog(null, "Error on loading chart for: " + cProfile.getColName(), "Error", exception);

        log.catching(exception);
        throw new RuntimeException(exception);
      }
    });
  }

  // TODO Add support: this.loadChartMetric(processType, dStore, tableInfo, queryInfo);
  private void loadChartMetric(ProcessType processType,
                               DStore dStore,
                               TableInfo tableInfo,
                               QueryInfo queryInfo) {

    Metric metric = getMetricFromUI();
    executorService.submit(() -> {
      fillPanelWithProgressBar(processType);

      try {
        loadChartByMetric(metric, processType, dStore, tableInfo, queryInfo, SeriesType.COMMON);
      } catch (Exception exception) {
        log.catching(exception);
        throw new RuntimeException(exception);
      }
    });
  }

  private void fillPanelWithProgressBar(ProcessType processType) {
    JSplitPane splitPane = getChartGanttPanel(processType);

    JPanel progressBar = ProgressBarHelper.createProgressBar("Loading, please wait...");
    configurePanel(progressBar);
    addPanelToSplitPane(splitPane, progressBar, JSplitPane.TOP);

    JPanel jPanelBottom = new JPanel();
    configurePanel(jPanelBottom);
    addPanelToSplitPane(splitPane, jPanelBottom, JSplitPane.BOTTOM);
  }

  protected void addPanelToSplitPane(JSplitPane splitPane, JPanel panel, String position) {
    GUIHelper.addToJSplitPane(splitPane, panel, position, LOCATION_SIZE);
  }

  protected void configurePanel(JPanel panel) {
    LaF.setBackgroundConfigPanel(CHART_PANEL, panel);
  }

  protected void cleanPanels(JSplitPane topBottomSplitPane, boolean addTop, boolean addBottom) {
    JPanel panel;

    if (addTop) {
      panel = new JPanel();
      configurePanel(panel);
      addPanelToSplitPane(topBottomSplitPane, panel, JSplitPane.TOP);
    }

    if (addBottom) {
      panel = new JPanel();
      configurePanel(panel);
      addPanelToSplitPane(topBottomSplitPane, panel, JSplitPane.BOTTOM);
    }
  }

  protected void cleanAllPanels() {
    cleanPanels(getChartGanttPanel(ADHOC), true, true);
    analyzePanel.removeAll();
  }

  protected Metric getMetricFromUI() {
    int metricId = GUIHelper.getIdByColumnName(metricCase.getJxTable(),
                                               metricCase.getDefaultTableModel(),
                                               metricCase.getJxTable().getSelectionModel(),
                                               MetricsColumnNames.ID.getColName());

    log.info("Metric id: " + metricId);

    return queryInfo.getMetricList()
        .stream().filter(f -> f.getId() == metricId)
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found metric by id: " + metricId));
  }

  protected CProfile getCProfileFromUI(TableInfo tableInfo) {
    int cProfileId = GUIHelper.getIdByColumnName(columnCase.getJxTable(),
                                                 columnCase.getDefaultTableModel(),
                                                 columnCase.getJxTable().getSelectionModel(),
                                                 ProfileColumnNames.ID.getColName());

    log.info("Column profile id: " + cProfileId);

    return tableInfo.getCProfiles()
        .stream()
        .filter(f -> f.getColId() == cProfileId)
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found CProfile: " + cProfileId));
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
        metric.setChartType(ChartType.STACKED);
      }
      case SUM, AVG -> {
        metric.setMetricFunction(metricFunctionOnEdit);
        metric.setChartType(ChartType.LINEAR);
      }
    }
  }

  private void setMetricFunctionAndChartNotEdit(CProfile cProfile,
                                                Metric metric) {
    if (CType.STRING.equals(cProfile.getCsType().getCType())) {
      metric.setMetricFunction(MetricFunction.COUNT);
      metric.setChartType(ChartType.STACKED);
    } else {
      if (Arrays.stream(TimestampType.values()).anyMatch((t) -> t.name().equals(cProfile.getColDbTypeName()))) {
        metric.setMetricFunction(MetricFunction.COUNT);
        metric.setChartType(ChartType.STACKED);
      } else {
        metric.setMetricFunction(MetricFunction.AVG);
        metric.setChartType(ChartType.LINEAR);
      }
    }
  }

  private void loadChartByMetric(Metric metric,
                                 ProcessType processType,
                                 DStore dStore,
                                 TableInfo tableInfo,
                                 QueryInfo queryInfo,
                                 SeriesType seriesType) {
    boolean isStackedYAxisSameCount = metric.isStackedYAxisSameCount();
    boolean isLinearYAxisSameSum = metric.isLinearYAxisSameSum();
    boolean isLinearYAxisAvg = metric.isLinearYAxisAvg();

    if (isStackedYAxisSameCount || isLinearYAxisSameSum || isLinearYAxisAvg) {
      addPanelsToVisualize(metric, processType, dStore, tableInfo, queryInfo, seriesType);
    }
  }

  private void addPanelsToVisualize(Metric metric,
                                    ProcessType processType,
                                    DStore dStore,
                                    TableInfo tableInfo,
                                    QueryInfo queryInfo,
                                    SeriesType seriesType) {
    StackChartAdHocPanel stackChartPanel = getStackChartPanel(metric, chartInfo, dStore, queryInfo, seriesType);
    DetailAdHocPanel detailPanel = getDetailPanel(dStore, metric, tableInfo, stackChartPanel.getSeriesColorMap(),
                                                  stackChartPanel, metric.getChartType(), seriesType);

    GUIHelper.addToJSplitPane(getChartGanttPanel(processType), stackChartPanel, JSplitPane.TOP, 250);
    GUIHelper.addToJSplitPane(getChartGanttPanel(processType), detailPanel, JSplitPane.BOTTOM, 250);

    CustomAction customAction = addPanelToAnalyze(dStore, metric, stackChartPanel);

    if (stackChartPanel instanceof AdHocSCP adHocSCP) {
      adHocSCP.setDetailAndAnalyzeHolder(new DetailAndAnalyzeHolder(detailPanel, customAction));
    }
  }

  protected StackChartAdHocPanel getStackChartPanel(Metric metric,
                                                    ChartInfo chartInfo,
                                                    DStore dStore,
                                                    QueryInfo queryInfo,
                                                    SeriesType seriesType) {
    CategoryTableXYDatasetRealTime chartDataset = new CategoryTableXYDatasetRealTime();

    StackChartAdHocPanel stackChartReportPanel =
        new AdHocSCP(chartDataset, chartInfo, metric, dStore, queryInfo, tableInfo, seriesType);

    stackChartReportPanel.initialize();

    return stackChartReportPanel;
  }

  protected DetailAdHocPanel getDetailPanel(DStore dStore,
                                            Metric metric,
                                            TableInfo tableInfo,
                                            Map<String, Color> seriesColorMap,
                                            DetailChart dynamicChart,
                                            ChartType chartType,
                                            SeriesType seriesType) {

    DetailAdHocPanel detailPanel =
        new DetailAdHocPanel(dStore, tableInfo, metric, seriesColorMap, chartType, seriesType);

    dynamicChart.addChartListenerReleaseMouse(detailPanel);

    return detailPanel;
  }

  private JSplitPane getChartGanttPanel(ProcessType processType) {
    if (ADHOC.equals(processType)) {
      return visualizeSplitPane;
    } else if (ADHOC_ANALYZE.equals(processType)) {
      throw new RuntimeException("Not supported");
    } else {
      throw new RuntimeException("Not supported");
    }
  }

  private class RadioListenerColumn implements ActionListener {

    public RadioListenerColumn() {
    }

    public void actionPerformed(ActionEvent e) {
      JRadioButton button = (JRadioButton) e.getSource();
      RadioButtonActionExecutor.execute(button, metricFunction -> metricFunctionOnEdit = metricFunction);
    }
  }

  private CustomAction addPanelToAnalyze(DStore dStore,
                                         Metric metric,
                                         StackChartAdHocPanel stackChartPanel) {
    analyzePanel.removeAll();

    ChartRange chartRange = getChartRange(dStore, tableInfo.getTableName(), chartInfo);
    chartInfo.setCustomBegin(chartRange.getBegin());
    chartInfo.setCustomEnd(chartRange.getEnd());

    AnalyzeAdHocPanel panel =
        new AnalyzeAdHocPanel(queryInfo, tableInfo, chartInfo, sourceConfig, metric,
                              stackChartPanel.getSeriesColorMap(), stackChartPanel.getSeriesType(),
                              dStore);

    if (SeriesType.CUSTOM.equals(stackChartPanel.getSeriesType())) {
      panel.setFilter(stackChartPanel.getFilter());
    }

    PGHelper.cellXYRemainder(analyzePanel, panel, false);

    stackChartPanel.addChartListenerReleaseMouse(panel);

    analyzePanel.revalidate();
    analyzePanel.repaint();

    return panel;
  }
}
