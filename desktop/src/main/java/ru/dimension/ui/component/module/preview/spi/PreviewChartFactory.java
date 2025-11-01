package ru.dimension.ui.component.module.preview.spi;

import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.chart.PAChartModule;
import ru.dimension.ui.component.module.chart.PHChartModule;
import ru.dimension.ui.component.module.chart.PRChartModule;
import ru.dimension.ui.component.module.chart.preview.DetailChartContext;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;

@FunctionalInterface
public interface PreviewChartFactory {
  IPreviewChart create(MessageBroker.Component component,
                       ChartKey chartKey,
                       Object key,
                       Metric metric,
                       QueryInfo queryInfo,
                       ChartInfo chartInfo,
                       TableInfo tableInfo,
                       SqlQueryState sqlQueryState,
                       DStore dStore);

  static PreviewChartFactory realTime() {
    return (component, chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore) ->
        new PRChartModule(component, chartKey, (ProfileTaskQueryKey) key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);
  }

  static PreviewChartFactory history(DetailChartContext detailContext) {
    return (component, chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore) ->
        new PHChartModule(component, chartKey, (ProfileTaskQueryKey) key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore, detailContext);
  }

  static PreviewChartFactory history() {
    return history(new DetailChartContext(null, null));
  }

  static PreviewChartFactory historyAdHoc(DetailChartContext detailContext) {
    return (component, chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore) ->
        new PAChartModule(component, (AdHocKey) key, metric, queryInfo, chartInfo, tableInfo, dStore, detailContext);
  }
}