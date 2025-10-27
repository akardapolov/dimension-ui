package ru.dimension.ui.component.module.chart;

import java.util.List;
import javax.swing.JTabbedPane;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.base.AbstractTabbedModule;
import ru.dimension.ui.component.module.chart.main.ChartModel;
import ru.dimension.ui.component.module.chart.main.ChartPresenter;
import ru.dimension.ui.component.module.chart.main.unit.HistoryUnitView;
import ru.dimension.ui.component.module.chart.main.unit.InsightUnitView;
import ru.dimension.ui.component.module.chart.main.unit.RealtimeUnitView;
import ru.dimension.ui.exception.SeriesExceedException;
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
public class ChartModule extends AbstractTabbedModule<ChartModel> {

  private final MessageBroker.Component component;

  @Getter
  private final ChartPresenter presenter;

  @Getter
  private final RealtimeUnitView realtimeUnitView;
  @Getter
  private final HistoryUnitView historyUnitView;
  @Getter
  private final InsightUnitView insightUnitView;

  public ChartModule(MessageBroker.Component component,
                     ChartKey chartKey,
                     ProfileTaskQueryKey key,
                     Metric metric,
                     QueryInfo queryInfo,
                     ChartInfo chartInfo,
                     TableInfo tableInfo,
                     SqlQueryState sqlQueryState,
                     DStore dStore) {
    super(new ChartModel(chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore));
    this.component = component;

    this.realtimeUnitView = new RealtimeUnitView(component);
    this.historyUnitView = new HistoryUnitView(component);
    this.insightUnitView = new InsightUnitView(component);

    this.presenter = new ChartPresenter(component, model, view, realtimeUnitView, historyUnitView, insightUnitView);
  }

  @Override
  protected List<TabDef> buildUnits() {
    return List.of(
        new TabDef(Panel.REALTIME, realtimeUnitView.getRootComponent()),
        new TabDef(Panel.HISTORY, historyUnitView.getRootComponent()),
        new TabDef(Panel.INSIGHT, insightUnitView.getRootComponent())
    );
  }

  @Override
  public Runnable initializeUI() {
    Runnable attach = super.initializeUI();
    presenter.initializeCharts();

    getTabbedPane().addChangeListener(e -> {
      JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
      int selectedIndex = sourceTabbedPane.getSelectedIndex();

      if (selectedIndex == Panel.HISTORY.ordinal()) {
        presenter.initHistoryUnitIfNeeded();
      } else if (selectedIndex == Panel.INSIGHT.ordinal()) {
        presenter.initInsightUnitIfNeeded();
      }
    });

    return attach;
  }

  public void loadData() {
    try {
      this.presenter.getRealTimeChart().loadData();
    } catch (SeriesExceedException e) {
      log.info("Series count exceeded threshold, reinitializing chart in custom mode for {}",
               model.getMetric().getYAxis());
      try {
        this.presenter.getRealTimeChart().initialize();
      } catch (Exception ignored) { }
    }
  }

  public void handleLegendChange(ChartLegendState chartLegendState) {
    presenter.handleLegendChangeAll(chartLegendState);
  }

  public void setActiveTab(MessageBroker.Panel panel) {
    presenter.setActiveTab(panel);
  }

  public void updateRealTimeRange(RangeRealTime range) {
    presenter.updateRealTimeRange(range);
  }

  public void updateHistoryRange(RangeHistory range) {
    presenter.updateHistoryRange(range);
  }

  public void updateHistoryCustomRange(ChartRange range) {
    presenter.updateHistoryCustomRange(range);
  }

  public boolean isReadyRealTimeUpdate() {
    return presenter.isReadyRealTimeUpdate();
  }
}