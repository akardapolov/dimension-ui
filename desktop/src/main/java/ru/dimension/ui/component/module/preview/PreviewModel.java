package ru.dimension.ui.component.module.preview;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.module.preview.spi.IPreviewChart;
import ru.dimension.ui.component.module.preview.spi.RunMode;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.state.SqlQueryState;

@Data
@Log4j2
public class PreviewModel {
  private final RunMode runMode;
  private final Object key;
  private final QueryInfo queryInfo;
  private final ChartInfo chartInfo;
  private final TableInfo tableInfo;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;
  private final Metric metric;

  private final ConcurrentMap<CProfile, IPreviewChart> chartModules = new ConcurrentHashMap<>();

  public PreviewModel(RunMode runMode,
                      ProfileTaskQueryKey key,
                      QueryInfo queryInfo,
                      ChartInfo chartInfo,
                      TableInfo tableInfo,
                      SqlQueryState sqlQueryState,
                      DStore dStore) {
    this(runMode, key, null, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);
  }

  public PreviewModel(RunMode runMode,
                      Object key,
                      Metric metric,
                      QueryInfo queryInfo,
                      ChartInfo chartInfo,
                      TableInfo tableInfo,
                      SqlQueryState sqlQueryState,
                      DStore dStore) {
    this.runMode = runMode;
    this.key = key;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo.copy();
    this.tableInfo = tableInfo;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;
    this.metric = metric;
  }


  public void addChartModule(CProfile cProfile, IPreviewChart module) {
    chartModules.put(cProfile, module);
  }

  public void clearChartModules() {
    chartModules.clear();
  }

  public Optional<Entry<CProfile, IPreviewChart>> getChartModuleByColumnName(String columnName) {
    return chartModules.entrySet().stream()
        .filter(module -> module.getKey().getColName().equals(columnName))
        .findFirst();
  }

  public void removeChartModule(CProfile cProfile) {
    chartModules.remove(cProfile);
  }
}