package ru.dimension.ui.view.detail.raw;

import java.awt.Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.detail.RawDataPanelCommon;

@Log4j2
public class RawDataReportPanel extends RawDataPanelCommon {

  private final DStore dStore;
  private final Map<String, Color> pendingSeriesColorMap;

  public RawDataReportPanel(DStore dStore,
                            TableInfo tableInfo,
                            CProfile cProfile,
                            long begin,
                            long end,
                            boolean useFetchSize) {
    this(dStore, tableInfo, cProfile, begin, end, useFetchSize, null);
  }

  public RawDataReportPanel(DStore dStore,
                            TableInfo tableInfo,
                            CProfile cProfile,
                            long begin,
                            long end,
                            boolean useFetchSize,
                            Map<String, Color> seriesColorMap) {
    super(tableInfo, cProfile, useFetchSize);

    this.dStore = dStore;
    this.pendingSeriesColorMap = seriesColorMap != null ? new LinkedHashMap<>(seriesColorMap) : null;

    this.loadResultSet(tableInfo.getTableName(), begin, end);
    this.loadRawData(tableInfo.getTableName(), begin, end);
  }

  @Override
  protected void loadResultSet(String tableName,
                               long begin,
                               long end) {
    if (useFetchSize) {
      batchResultSet = dStore.getBatchResultSet(tableName, begin, end, fetchSize);
    }
  }

  @Override
  protected void loadRawData(String tableName,
                             long begin,
                             long end) {
    log.info("Parameters begin: {}, end: {}", getDate(begin), getDate(end));

    List<List<Object>> rawData;
    if (useFetchSize) {
      rawData = batchResultSet.next() ? batchResultSet.getObject() : Collections.emptyList();
    } else {
      rawData = dStore.getRawDataAll(tableName, begin, end);
    }

    if (!rawData.isEmpty()) {
      hasData = true;
    }

    loadToModel(rawData);

    if (pendingSeriesColorMap != null && !pendingSeriesColorMap.isEmpty()) {
      Map<String, Color> presentSeriesColorMap = filterByPresentSeries(pendingSeriesColorMap);
      if (!presentSeriesColorMap.isEmpty()) {
        initializeFilterStrip(presentSeriesColorMap);
      }
    }
  }

  private Map<String, Color> filterByPresentSeries(Map<String, Color> originalMap) {
    java.util.Set<String> presentKeys = collectPresentSeriesKeys();
    Map<String, Color> filtered = new LinkedHashMap<>();
    for (Map.Entry<String, Color> entry : originalMap.entrySet()) {
      if (presentKeys.contains(entry.getKey())) {
        filtered.put(entry.getKey(), entry.getValue());
      }
    }
    return filtered;
  }
}