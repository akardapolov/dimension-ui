package ru.dimension.ui.view.chart.search;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JPanel;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.config.prototype.chart.WorkspaceChartModule;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.listener.CollectStartStopListener;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.chart.DetailChart;
import ru.dimension.ui.view.chart.HelperChart;
import ru.dimension.ui.view.chart.StackedChart;

@Log4j2
public abstract class SearchStackChartPanel extends JPanel implements DetailChart, HelperChart,
    CollectStartStopListener {

  protected StackedChart stackedChart;
  private ChartPanel chartPanel;
  private JFreeChart jFreeChart;
  private DateAxis dateAxis;

  private String name;
  private String xAxisLabel = "xAxisLabel";
  private String yAxisLabel = "Value";
  protected int legendFontSize = 12;

  protected Set<String> series;

  private final WorkspaceQueryComponent workspaceQueryComponent;

  protected final CategoryTableXYDatasetRealTime chartDataset;

  protected final ProfileTaskQueryKey profileTaskQueryKey;
  protected final QueryInfo queryInfo;
  protected final TableInfo tableInfo;
  protected final ChartInfo chartInfo;
  protected final ProcessType processType;

  @Inject
  @Named("eventListener")
  EventListener eventListener;

  @Inject
  @Named("localDB")
  DStore dStore;

  @Inject
  @Named("sqlQueryState")
  SqlQueryState sqlQueryState;

  public SearchStackChartPanel(WorkspaceQueryComponent workspaceQueryComponent,
                               CategoryTableXYDatasetRealTime chartDataset,
                               ProfileTaskQueryKey profileTaskQueryKey,
                               QueryInfo queryInfo,
                               TableInfo tableInfo,
                               ChartInfo chartInfo,
                               ProcessType processType) {
    this.workspaceQueryComponent = workspaceQueryComponent;
    this.workspaceQueryComponent.initChart(new WorkspaceChartModule(this)).inject(this);

    this.chartDataset = chartDataset;
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.queryInfo = queryInfo;
    this.tableInfo = tableInfo;
    this.chartInfo = chartInfo;
    this.processType = processType;

    this.series = new LinkedHashSet<>();

    this.stackedChart = new StackedChart(getChartPanel(this.chartDataset));
    this.stackedChart.setLegendFontSize(legendFontSize);
    this.stackedChart.initialize();

    LaF.setBackgroundAndTextColorForStackedChartPanel(CHART_PANEL, this.stackedChart);

    this.setLayout(new BorderLayout());
  }

  public abstract void initialize();

  protected void initializeSearch() {
    this.setLayout(new BorderLayout());
    this.add("Center", stackedChart.getChartPanel());
  }

  protected abstract void loadData();

  public Map<String, Color> getSeriesColorMap() {
    return stackedChart.getSeriesColorMap();
  }

  protected ChartPanel getChartPanel(CategoryTableXYDatasetRealTime chartDataset) {
    xAxisLabel = "Query: " + queryInfo.getName();
    dateAxis = new DateAxis();
    jFreeChart = ChartFactory.createStackedXYAreaChart("", xAxisLabel, yAxisLabel, chartDataset,
                                                       PlotOrientation.VERTICAL, dateAxis, false, true, false);
    chartPanel = new ChartPanel(jFreeChart, true, true, true, false, true);

    return chartPanel;
  }

  @Override
  public void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("fireOnStartCollect for profileTaskQueryKey: " + profileTaskQueryKey);
  }

  @Override
  public void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("fireOnStopCollect for profileTaskQueryKey:" + profileTaskQueryKey);
  }

  @Override
  public void addChartListenerReleaseMouse(IDetailPanel l) {
    stackedChart.addChartListenerReleaseMouse(l);
  }

  @Override
  public void removeChartListenerReleaseMouse(IDetailPanel l) {
    stackedChart.removeChartListenerReleaseMouse(l);
  }
}
