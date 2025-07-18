package ru.dimension.ui.component.module.adhoc.chart;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;

@Data
@Log4j2
public class AdHocChartModel {
  private final AdHocKey adHocKey;
  private final Metric metric;
  private final QueryInfo queryInfo;
  private final ChartInfo chartInfo;
  private final TableInfo tableInfo;
  private final DStore dStore;

  public AdHocChartModel(AdHocKey adHocKey,
                         Metric metric,
                         QueryInfo queryInfo,
                         ChartInfo chartInfo,
                         TableInfo tableInfo,
                         DStore dStore) {
    this.adHocKey = adHocKey;
    this.metric = metric;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo.copy();
    this.tableInfo = tableInfo;
    this.dStore = dStore;
  }
}