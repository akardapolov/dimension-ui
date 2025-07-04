package ru.dimension.ui.view.chart.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.ProcessType;

@Log4j2
public class SearchSCP extends SearchStackChartPanel {

  private final String searchString;

  public SearchSCP(WorkspaceQueryComponent workspaceQueryComponent,
                   CategoryTableXYDatasetRealTime chartDataset,
                   ProfileTaskQueryKey profileTaskQueryKey,
                   QueryInfo queryInfo,
                   TableInfo tableInfo,
                   ChartInfo chartInfo,
                   ProcessType processType,
                   String searchString) {
    super(workspaceQueryComponent, chartDataset, profileTaskQueryKey, queryInfo, tableInfo, chartInfo, processType);

    this.searchString = searchString;
  }

  public void initialize() {
    initializeSearch();
  }

  public void loadData() {
    log.info("Load search data");

    long serverDateTime = System.currentTimeMillis() - queryInfo.getDeltaLocalServerTime();
    long begin = serverDateTime - getRangeRealTime(chartInfo);
    long end = serverDateTime;

    List<CProfile> cProfileList = tableInfo.getCProfiles()
        .stream()
        .filter(f -> (!f.getCsType().isTimeStamp()))
        .toList();

    List<StackedColumn> sColumnList = new ArrayList<>();

    double range = (double) getRangeRealTime(chartInfo) / MAX_POINTS_PER_GRAPH;
    for (long dtBegin = begin; dtBegin <= end; dtBegin += Math.round(range)) {
      long dtEnd = dtBegin + Math.round(range) - 1;

      StackedColumn stackedColumn = new StackedColumn();
      stackedColumn.setKey(dtBegin);
      stackedColumn.setTail(dtEnd);

      List<List<Object>> rawData = dStore.getRawDataAll(queryInfo.getName(), dtBegin, dtEnd);

      rawData.forEach(row -> {
        Object[] rawObj = row.toArray();

        cProfileList.forEach(cProfile -> {
          String colName = cProfile.getColName();
          Object colValue = rawObj[cProfile.getColId()];

          if (colValue.toString().toLowerCase().contains(searchString.toLowerCase())) {
            stackedColumn.getKeyCount().merge(colName, 1, Integer::sum);
          }
        });
      });

      sColumnList.add(stackedColumn);
    }

    // Load data to chart
    sColumnList.stream()
        .map(StackedColumn::getKeyCount)
        .map(Map::keySet)
        .flatMap(Collection::stream)
        .forEach(series::add);

    if (sColumnList.isEmpty()) {
      log.info("No data found");
      return;
    }

    sColumnList.forEach(stackedColumn -> {
      Map<String, Integer> keyCount = stackedColumn.getKeyCount();

      series.forEach(series -> {
        stackedChart.loadSeriesColorInternal(series);

        double y;
        try {
          double k = getK(range, chartInfo);

          y = (double) keyCount.getOrDefault(series, 0) / k;

          stackedChart.addSeriesValue(stackedColumn.getKey(), y, series);
        } catch (Exception exception) {
          log.info(exception);
        }
      });
    });

  }

}
