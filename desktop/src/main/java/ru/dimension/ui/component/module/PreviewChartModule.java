package ru.dimension.ui.component.module;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.preview.chart.PreviewChartModel;
import ru.dimension.ui.component.module.preview.chart.PreviewChartPresenter;
import ru.dimension.ui.component.module.preview.chart.PreviewChartView;
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
public class PreviewChartModule extends JXTaskPane {
  private final MessageBroker.Component component;

  @Getter
  private final PreviewChartView view;
  private final PreviewChartModel model;
  @Getter
  private final PreviewChartPresenter presenter;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public PreviewChartModule(MessageBroker.Component component,
                            ChartKey chartKey,
                            ProfileTaskQueryKey key,
                            Metric metric,
                            QueryInfo queryInfo,
                            ChartInfo chartInfo,
                            TableInfo tableInfo,
                            SqlQueryState sqlQueryState,
                            DStore dStore) {
    this.component = component;
    this.model = new PreviewChartModel(chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);
    this.view = new PreviewChartView(component);
    this.presenter = new PreviewChartPresenter(component, model, view);

    this.setAnimated(false);
  }

  public Runnable initializeUI() {
    return () -> PGHelper.cellXYRemainder(this, view.getRealTimeConfigChart(), false);
  }

  public void loadData() {
    this.presenter.getRealTimeChart().loadData();
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
}