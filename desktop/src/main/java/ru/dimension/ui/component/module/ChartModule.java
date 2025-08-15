package ru.dimension.ui.component.module;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.chart.realtime.ClientRealtimeSCP;
import ru.dimension.ui.component.chart.realtime.ServerRealtimeSCP;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.model.DetailState;
import ru.dimension.ui.component.model.PanelTabType;
import ru.dimension.ui.component.module.chart.ChartModel;
import ru.dimension.ui.component.module.chart.ChartPresenter;
import ru.dimension.ui.component.module.chart.ChartView;
import ru.dimension.ui.exception.SeriesExceedException;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class ChartModule extends JXTaskPane {
  private final MessageBroker.Component component;

  @Getter
  private final ChartView view;
  private final ChartModel model;
  @Getter
  private final ChartPresenter presenter;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public ChartModule(MessageBroker.Component component,
                     ChartKey chartKey,
                     ProfileTaskQueryKey key,
                     Metric metric,
                     QueryInfo queryInfo,
                     ChartInfo chartInfo,
                     TableInfo tableInfo,
                     SqlQueryState sqlQueryState,
                     DStore dStore) {
    this.component = component;
    this.model = new ChartModel(chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);
    this.view = new ChartView(component);
    this.presenter = new ChartPresenter(component, model, view);

    this.setAnimated(false);
  }

  public Runnable initializeUI() {
    presenter.initializeCharts();

    return () -> PGHelper.cellXYRemainder(this, view.getTabbedPane(), false);
  }

  public void loadData() {
    try {
      this.presenter.getRealTimeChart().loadData();
    } catch (SeriesExceedException e) {
      log.info("Series count exceeded threshold, reinitializing chart in custom mode for " + model.getMetric().getYAxis());
      if (this.presenter.getRealTimeChart() instanceof ClientRealtimeSCP clientRealtimeSCP) {
        clientRealtimeSCP.reinitializeChartInCustomMode();
      } else if (this.presenter.getRealTimeChart() instanceof ServerRealtimeSCP serverRealtimeSCP) {
        serverRealtimeSCP.reinitializeChartInCustomMode();
      }
    }
  }

  public void handleLegendChange(ChartLegendState chartLegendState) {
    presenter.handleLegendChangeAll(ChartLegendState.SHOW.equals(chartLegendState));
  }

  public void setDetailState(DetailState detailState) {
    this.presenter.setDetailState(detailState);
  }

  public void setActiveTab(PanelTabType panelTabType) {
    this.presenter.setActiveTab(panelTabType);
  }

  public void updateRealTimeRange(RangeRealTime range) {
    this.presenter.updateRealTimeRange(range);
  }

  public void updateHistoryRange(RangeHistory range) {
    this.presenter.updateHistoryRange(range);
  }

  public void updateHistoryCustomRange(ChartRange range) {
    this.presenter.updateHistoryCustomRange(range);
  }

  public boolean isReadyRealTimeUpdate() {
    return this.presenter.isReadyRealTimeUpdate();
  }
}