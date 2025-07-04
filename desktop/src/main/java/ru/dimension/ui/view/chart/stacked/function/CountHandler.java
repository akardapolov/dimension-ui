package ru.dimension.ui.view.chart.stacked.function;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.GroupFunction;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.exception.SeriesExceedException;
import ru.dimension.ui.view.chart.StackedChart;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.view.chart.stacked.FunctionHandler;

@Log4j2
public class CountHandler extends FunctionHandler {

  private Metric metric;
  private Entry<CProfile, List<String>> filter;

  public CountHandler(Metric metric,
                      QueryInfo queryInfo,
                      DStore dStore) {
    super(metric, queryInfo, dStore);

    this.metric = metric;
  }

  @Override
  public void fillSeriesData(long begin,
                             long end,
                             Set<String> series) {
    List<StackedColumn> sColumnList;
    try {
      sColumnList = handleFunctionComplex(begin, end);

      fillSeries(sColumnList, series);
    } catch (SqlColMetadataException | BeginEndWrongOrderException e) {
      log.error(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setFilter(Entry<CProfile, List<String>> filter) {
    this.filter = filter;
  }

  @Override
  public void handleFunction(long begin,
                             long end,
                             boolean isClientRealTime,
                             long finalX,
                             double yK,
                             Set<String> series,
                             StackedChart stackedChart) {

    try {
      List<StackedColumn> sColumnList = handleFunctionComplex(begin, end);

      long x;

      if (isClientRealTime) {
        x = sColumnList.isEmpty() ? finalX : sColumnList.get(0).getKey();
      } else {
        x = finalX;
      }

      checkSeriesCount(series);
      fillSeries(sColumnList, series);
      checkSeriesCount(series);

      Map<String, IntSummaryStatistics> batchData = sColumnList.stream()
          .toList()
          .stream()
          .map(StackedColumn::getKeyCount)
          .flatMap(sc -> sc.entrySet().stream())
          .collect(Collectors.groupingBy(entry -> Objects.requireNonNullElse(entry.getKey(), ""),
                                         Collectors.summarizingInt(Map.Entry::getValue)));
      series.forEach(seriesName -> {
        Optional<IntSummaryStatistics> batch = Optional.ofNullable(batchData.get(seriesName));
        stackedChart.loadSeriesColorInternal(seriesName);

        try {
          if (batch.isPresent()) {
            double y =
                sColumnList.size() == 0 ? 0D : ((double) batch.map(IntSummaryStatistics::getSum).orElse(0L) / (yK));
            stackedChart.addSeriesValue(x, y, seriesName);
          } else {
            stackedChart.addSeriesValue(x, 0D, seriesName);
          }

        } catch (Exception exception) {
          log.info(exception);
        }
      });

    } catch (SqlColMetadataException | BeginEndWrongOrderException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void handleFunction(long begin,
                             long end,
                             double yK,
                             Set<String> series,
                             CProfile cProfileFilter,
                             String[] filterData,
                             CompareFunction compareFunction,
                             StackedChart stackedChart) {
    try {
      List<StackedColumn> sColumnList = dStore.getStacked(queryInfo.getName(),
                                                          metric.getYAxis(),
                                                          GroupFunction.COUNT,
                                                          cProfileFilter,
                                                          filterData,
                                                          compareFunction,
                                                          begin,
                                                          end);

      Map<String, IntSummaryStatistics> batchData = sColumnList.stream()
          .toList()
          .stream()
          .map(StackedColumn::getKeyCount)
          .flatMap(sc -> sc.entrySet().stream())
          .collect(Collectors.groupingBy(entry -> Objects.requireNonNullElse(entry.getKey(), ""),
                                         Collectors.summarizingInt(Map.Entry::getValue)));
      series.forEach(seriesName -> {
        Optional<IntSummaryStatistics> batch = Optional.ofNullable(batchData.get(seriesName));
        stackedChart.loadSeriesColorInternal(seriesName);

        try {
          if (batch.isPresent()) {
            double y =
                sColumnList.size() == 0 ? 0D : ((double) batch.map(IntSummaryStatistics::getSum).orElse(0L) / (yK));
            stackedChart.addSeriesValue(begin, y, seriesName);
          } else {
            stackedChart.addSeriesValue(begin, 0D, seriesName);
          }

        } catch (Exception exception) {
          log.info(exception);
        }
      });

    } catch (SqlColMetadataException | BeginEndWrongOrderException e) {
      throw new RuntimeException(e);
    }
  }

  private void checkSeriesCount(Set<String> series) {
    if (series.size() > 50) {
      throw new SeriesExceedException("Column data series exceeds 50. Not supported to show stacked data..");
    }
  }

  @Override
  public List<StackedColumn> handleFunctionComplex(long begin,
                                                   long end)
      throws BeginEndWrongOrderException, SqlColMetadataException {

    if (filter == null) {
      return dStore.getStacked(queryInfo.getName(), metric.getYAxis(), GroupFunction.COUNT, begin, end);
    } else {
      return dStore.getStacked(queryInfo.getName(),
                               metric.getYAxis(),
                               GroupFunction.COUNT,
                               filter.getKey(),
                               filter.getValue().toArray(new String[0]),
                               CompareFunction.EQUAL,
                               begin,
                               end);
    }
  }
}
