package ru.dimension.ui.view.analyze.chart;


import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.output.StackedColumn;
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
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.chart.DetailChart;
import ru.dimension.ui.view.chart.HelperChart;
import ru.dimension.ui.view.chart.StackedChart;
import ru.dimension.ui.model.view.SeriesType;

@Log4j2
public abstract class SCP extends JPanel implements HelperChart, DetailChart {

  @Getter
  protected final ChartConfig config;

  protected final CategoryTableXYDatasetRealTime chartDataset;
  protected final ProfileTaskQueryKey profileTaskQueryKey;

  protected StackedChart stackedChart;
  private JFreeChart jFreeChart;

  protected Set<String> series;

  @Setter
  protected SqlQueryState sqlQueryState;
  @Setter
  protected DStore dStore;
  @Getter
  protected SeriesType seriesType = SeriesType.COMMON;

  public SCP(ChartConfig config,
             ProfileTaskQueryKey profileTaskQueryKey) {
    this.config = config;
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.chartDataset = new CategoryTableXYDatasetRealTime();

    if (config.getMetric().getYAxis().getCsType() == null) {
      throw new NotFoundException("Column storage type undefined: " + config.getMetric().getYAxis());
    }

    this.series = new LinkedHashSet<>();

    createStackedChart();
  }

  protected void createStackedChart() {
    this.stackedChart = new StackedChart(getChartPanel(this.chartDataset));
    this.stackedChart.setLegendFontSize(config.getLegendFontSize());
    this.stackedChart.initialize();

    LaF.setBackgroundAndTextColorForStackedChartPanel(LafColorGroup.CHART_PANEL, this.stackedChart);
  }

  protected ChartPanel getChartPanel(CategoryTableXYDatasetRealTime dataset) {
    DateAxis dateAxis = new DateAxis();
    jFreeChart = ChartFactory.createStackedXYAreaChart(config.getTitle(),
                                                       config.getXAxisLabel(),
                                                       config.getYAxisLabel(),
                                                       dataset,
                                                       PlotOrientation.VERTICAL,
                                                       dateAxis,
                                                       config.isLegend(),
                                                       config.isTooltips(),
                                                       config.isUrls());

    return new ChartPanel(jFreeChart, true, true, true, false, true);
  }

  public abstract void initialize();

  public abstract void loadData();

  public void loadSeriesColor(Metric metric, Map<String, Color> seriesColorMap) {
    if (MetricFunction.COUNT.equals(metric.getMetricFunction())) {
      log.info("Try to load series color from the main chart");

      if (seriesColorMap == null || seriesColorMap.isEmpty()) {
        log.info("Empty series color map");
      } else {
        loadSeriesColorExternal(seriesColorMap);
      }
    } else {
      loadSeriesColorExternal(Map.of(metric.getYAxis().getColName(), new Color(255, 93, 93)));
      loadSeriesColorInternal(metric.getYAxis().getColName());
    }
  }

  public void loadSeriesColorExternal(Map<String, Color> seriesColorMap) {
    seriesColorMap.forEach((seriesName, color) -> this.stackedChart.loadExternalSeriesColor(seriesName, color));
  }

  public void loadSeriesColorInternal(String seriesName) {
    this.stackedChart.loadSeriesColorInternal(seriesName);
  }

  protected void initializeGUI() {
    this.setLayout(new BorderLayout());
    this.add(stackedChart.getChartPanel(), BorderLayout.CENTER);
  }

  protected void initializeCustom() {
    this.setLayout(new BorderLayout());
    this.createStackedChart();
  }

  protected void initializeCustomGUI(JXTableCase jxTableCase) {
    this.add(jxTableCase.getJScrollPane(), BorderLayout.EAST);
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

  protected void fillSeriesAnalyze(List<StackedColumn> sColumnList) {
    if (MetricFunction.COUNT.equals(config.getMetric().getMetricFunction())) {
      sColumnList.stream()
          .map(StackedColumn::getKeyCount)
          .map(Map::keySet)
          .flatMap(Collection::stream)
          .forEach(series::add);
    } else if (MetricFunction.SUM.equals(config.getMetric().getMetricFunction())) {
      sColumnList.stream()
          .map(StackedColumn::getKeySum)
          .map(Map::keySet)
          .flatMap(Collection::stream)
          .forEach(series::add);
    } else if (MetricFunction.AVG.equals(config.getMetric().getMetricFunction())) {
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

  public void clearSelectionRegion() {
    stackedChart.clearSelectionRegion();
  }

  public void setChartTitle(String titleText) {
    this.stackedChart.setChartTitle(titleText);
  }

  protected String getDate(long l) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    Date date = new Date(l);
    return dateFormat.format(date);
  }

  protected void disablePlotUpdates() {
    stackedChart.setNotifyPlot(false);
  }

  protected void enablePlotUpdates() {
    stackedChart.setNotifyPlot(true);
  }
}
