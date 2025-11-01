package ru.dimension.ui.component.module.chart.preview.history;

import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.module.chart.main.ChartModel;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class PHChartModel extends ChartModel {

  public PHChartModel(ChartKey chartKey,
                      Object key,
                      Metric metric,
                      QueryInfo queryInfo,
                      ChartInfo chartInfo,
                      TableInfo tableInfo,
                      SqlQueryState sqlQueryState,
                      DStore dStore) {
    super(chartKey, (ProfileTaskQueryKey) key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);
  }
}