package ru.dimension.ui.component.module.preview.chart;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;

@Data
@Log4j2
public class PreviewChartModel {
  private final ChartKey chartKey;
  private final ProfileTaskQueryKey key;
  private final Metric metric;
  private final QueryInfo queryInfo;
  private final ChartInfo chartInfo;
  private final TableInfo tableInfo;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  public PreviewChartModel(ChartKey chartKey,
                           ProfileTaskQueryKey key,
                           Metric metric,
                           QueryInfo queryInfo,
                           ChartInfo chartInfo,
                           TableInfo tableInfo,
                           SqlQueryState sqlQueryState,
                           DStore dStore) {
    this.chartKey = chartKey;
    this.key = key;
    this.metric = metric;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo.copy();
    this.tableInfo = tableInfo;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;
  }
}
