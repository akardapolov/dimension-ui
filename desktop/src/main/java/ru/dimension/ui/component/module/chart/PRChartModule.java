package ru.dimension.ui.component.module.chart;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.chart.preview.realtime.PRChartModel;
import ru.dimension.ui.component.module.chart.preview.realtime.PRChartPresenter;
import ru.dimension.ui.component.module.chart.preview.realtime.PRChartView;
import ru.dimension.ui.component.module.preview.spi.IRealTimePreviewChart;
import ru.dimension.ui.exception.SeriesExceedException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class PRChartModule extends JXTaskPane implements IRealTimePreviewChart {

  private final PRChartModel model;
  @Getter
  private final PRChartView view;
  @Getter
  private final PRChartPresenter presenter;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public PRChartModule(MessageBroker.Component component,
                       ChartKey chartKey,
                       ProfileTaskQueryKey key,
                       Metric metric,
                       QueryInfo queryInfo,
                       ChartInfo chartInfo,
                       TableInfo tableInfo,
                       SqlQueryState sqlQueryState,
                       DStore dStore) {

    this.model = new PRChartModel(chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);
    this.view = new PRChartView(component);
    this.presenter = new PRChartPresenter(component, model, view, executor);
    this.presenter.initializePresenter();
    this.presenter.initializeCharts();

    ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder());

    this.setAnimated(false);
  }

  public Runnable initializeUI() {
    view.getConfigChartSplitPane().setBorder(GUIHelper.getConfigureBorder(1));

    return () -> PGHelper.cellXYRemainder(this, view.getConfigChartSplitPane(), 1, false);
  }

  public void loadData() {
    try {
      if (presenter.getChart() != null) {
        presenter.getChart().loadData();
      }
    } catch (SeriesExceedException e) {
      log.warn("Series count exceeded during loadData, re-updating chart for {}", model.getMetric().getYAxis());
      presenter.updateChart();
    }
  }

  public void handleLegendChange(ChartLegendState chartLegendState) {
    presenter.handleLegendChange(ChartLegendState.SHOW.equals(chartLegendState));
  }

  public void handleRealTimeRangeUI(RangeRealTime range) {
    this.presenter.handleRealTimeRangeChangeUI(range);
  }

  public void handleRealTimeRange(RangeRealTime range) {
    this.presenter.handleRealTimeRangeChange("", range);
  }

  public void handleChartConfigState(ChartConfigState chartConfigState) {
    this.presenter.handleChartConfigState(chartConfigState);
  }

  public boolean isReadyRealTimeUpdate() {
    return this.presenter.isReadyRealTimeUpdate();
  }

  @Override
  public JXTaskPane asTaskPane() {
    return this;
  }
}