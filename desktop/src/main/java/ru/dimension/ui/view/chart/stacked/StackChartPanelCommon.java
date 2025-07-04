package ru.dimension.ui.view.chart.stacked;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.CProfile;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.chart.DetailChart;
import ru.dimension.ui.view.chart.HelperChart;
import ru.dimension.ui.view.chart.StackedChart;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.SeriesType;

@Log4j2
public abstract class StackChartPanelCommon extends JPanel implements DetailChart, HelperChart {

  protected final CategoryTableXYDatasetRealTime chartDataset;
  protected final ProfileTaskQueryKey profileTaskQueryKey;
  protected final QueryInfo queryInfo;
  protected final ChartInfo chartInfo;
  protected final ProcessType processType;

  protected final Metric metric;

  protected StackedChart stackedChart;
  private JFreeChart jFreeChart;
  private ChartPanel chartPanel;
  private DateAxis dateAxis;

  private String xAxisLabel = "xAxisLabel";
  private String yAxisLabel = "Value";
  protected int legendFontSize = 12;

  protected Set<String> series;

  @Setter
  protected EventListener eventListener;
  @Setter
  protected ProfileManager profileManager;
  @Setter
  protected SqlQueryState sqlQueryState;
  @Setter
  protected DStore dStore;

  @Getter
  protected SeriesType seriesType = SeriesType.COMMON;

  @Getter
  @Setter
  protected Entry<CProfile, List<String>> filter;

  public StackChartPanelCommon(CategoryTableXYDatasetRealTime chartDataset,
                               ProfileTaskQueryKey profileTaskQueryKey,
                               QueryInfo queryInfo,
                               ChartInfo chartInfo,
                               ProcessType processType,
                               Metric metric) {
    this.chartDataset = chartDataset;
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo;
    this.processType = processType;
    this.metric = metric;

    if (metric.getYAxis().getCsType() == null) {
      throw new NotFoundException("Column storage type is undefined for column profile: " + metric.getYAxis());
    }

    this.series = new LinkedHashSet<>();

    createStackedChart();
  }

  protected void createStackedChart() {
    this.stackedChart = new StackedChart(getChartPanel(this.chartDataset));
    this.stackedChart.setLegendFontSize(legendFontSize);
    this.stackedChart.initialize();

    LaF.setBackgroundAndTextColorForStackedChartPanel(LafColorGroup.CHART_PANEL, this.stackedChart);
  }

  public abstract void initialize();

  public abstract void loadData();

  public void loadSeriesColor(Map<String, Color> seriesColorMap) {
    seriesColorMap.forEach((seriesName, color) -> this.stackedChart.loadExternalSeriesColor(seriesName, color));
  }

  public void clearSeriesColor() {
    this.stackedChart.clearSeriesColor();
  }

  protected void initializeGUI() {
    this.setLayout(new BorderLayout());
    this.add(stackedChart.getChartPanel(), BorderLayout.CENTER);
  }

  protected void initializeCustom() {
    this.setLayout(new BorderLayout());
    this.createStackedChart();
  }

  protected void initializeCustomGUI(JXTableCase seriesSelectable) {
    this.add(seriesSelectable.getJScrollPane(), BorderLayout.EAST);
    this.add(stackedChart.getChartPanel(), BorderLayout.CENTER);
  }

  protected void initializeCustomUpdate() {
    BorderLayout layout = (BorderLayout) this.getLayout();
    this.remove(layout.getLayoutComponent(BorderLayout.CENTER));

    this.add(ProgressBarHelper.createProgressBar("Loading, please wait..."), BorderLayout.CENTER);
    this.revalidate();
    this.repaint();

    this.chartDataset.clear();

    createStackedChart();
  }

  protected void initializeCustomUpdateGUI() {
    BorderLayout layout = (BorderLayout) this.getLayout();
    this.remove(layout.getLayoutComponent(BorderLayout.CENTER));

    this.add(stackedChart.getChartPanel(), BorderLayout.CENTER);
    this.revalidate();
    this.repaint();
  }

  protected void initializeDateAxis(long begin, long end ) {
    this.stackedChart.setDateAxis(begin, end);
  }

  public Map<String, Color> getSeriesColorMap() {
    return stackedChart.getSeriesColorMap();
  }

  public CategoryTableXYDatasetRealTime getChartDataset() {
    return chartDataset;
  }

  protected ChartPanel getChartPanel(CategoryTableXYDatasetRealTime chartDataset) {
    xAxisLabel = metric.getYAxis().getColName();
    dateAxis = new DateAxis();
    jFreeChart = ChartFactory.createStackedXYAreaChart("", xAxisLabel, yAxisLabel, chartDataset,
                                                       PlotOrientation.VERTICAL, dateAxis, false, true, false);
    chartPanel = new ChartPanel(jFreeChart, true, true, true, false, true);

    return chartPanel;
  }

  protected void fillSeriesAnalyze(List<StackedColumn> sColumnList) {
    if (MetricFunction.COUNT.equals(metric.getMetricFunction())) {
      sColumnList.stream()
          .map(StackedColumn::getKeyCount)
          .map(Map::keySet)
          .flatMap(Collection::stream)
          .forEach(series::add);
    } else if (MetricFunction.SUM.equals(metric.getMetricFunction())) {
      sColumnList.stream()
          .map(StackedColumn::getKeySum)
          .map(Map::keySet)
          .flatMap(Collection::stream)
          .forEach(series::add);
    } else if (MetricFunction.AVG.equals(metric.getMetricFunction())) {
      sColumnList.stream()
          .map(StackedColumn::getKeyAvg)
          .map(Map::keySet)
          .flatMap(Collection::stream)
          .forEach(series::add);
    } else {
      sColumnList.stream()
          .map(StackedColumn::getKeyCount)
          .map(Map::keySet)
          .flatMap(Collection::stream)
          .forEach(series::add);
    }
  }

  public boolean hasNonEmptyKeyCount(List<StackedColumn> sColumnList) {
    return sColumnList.stream()
        .anyMatch(sColumn -> sColumn != null &&
            sColumn.getKeyCount() != null &&
            !sColumn.getKeyCount().isEmpty());
  }

  public JFreeChart getjFreeChart() {
    return jFreeChart;
  }

  public void setChartTitle(String titleText) {
    this.stackedChart.setChartTitle(titleText);
  }

  void disablePlotUpdates() {
    stackedChart.setNotifyPlot(false);
  }

  void enablePlotUpdates() {
    stackedChart.setNotifyPlot(true);
  }

  protected String getDate(long l) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    Date date = new Date(l);
    return dateFormat.format(date);
  }
}
