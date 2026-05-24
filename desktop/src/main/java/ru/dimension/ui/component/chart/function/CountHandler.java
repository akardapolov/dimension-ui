package ru.dimension.ui.component.chart.function;

import java.util.IntSummaryStatistics;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.GroupFunction;
import ru.dimension.db.model.filter.CompositeFilter;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.chart.StackedChart;
import ru.dimension.ui.helper.FilterHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;

@Log4j2
public class CountHandler extends FunctionHandler {

  private Metric metric;
  private Map<CProfile, LinkedHashSet<String>> topMapSelected;

  public CountHandler(ProfileTaskQueryKey profileTaskQueryKey,
                      Metric metric,
                      QueryInfo queryInfo,
                      DStore dStore) {
    super(profileTaskQueryKey, metric, queryInfo, dStore);
    this.metric = metric;
  }

  @Override
  public void fillSeriesData(long begin, long end, Set<String> series) {
    try {
      List<StackedColumn> sColumnList = handleFunctionComplex(begin, end);
      fillSeries(sColumnList, series);
    } catch (SqlColMetadataException | BeginEndWrongOrderException e) {
      log.error(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    this.topMapSelected = topMapSelected;
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

      long x = isClientRealTime
          ? (sColumnList.isEmpty() ? finalX : sColumnList.getFirst().getKey())
          : finalX;

      fillSeries(sColumnList, series);

      boolean hasPercentile = sColumnList.stream()
          .anyMatch(c -> c.getKeyPercentile() != null
              && !c.getKeyPercentile().isEmpty());

      if (hasPercentile) {
        Map<String, Double> percentileData = sColumnList.stream()
            .map(StackedColumn::getKeyPercentile)
            .filter(Objects::nonNull)
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Double::sum));

        series.forEach(seriesName -> {
          stackedChart.loadSeriesColorInternal(
              profileTaskQueryKey.getColorProfileName(), seriesName);
          double y = percentileData.getOrDefault(seriesName, 0.0) / yK;
          try {
            stackedChart.addSeriesValue(x, y, seriesName);
          } catch (Exception ex) {
            log.info(ex);
          }
        });

      } else {
        Map<String, IntSummaryStatistics> batchData = sColumnList.stream()
            .map(StackedColumn::getKeyCount)
            .flatMap(sc -> sc.entrySet().stream())
            .collect(Collectors.groupingBy(
                entry -> Objects.requireNonNullElse(entry.getKey(), ""),
                Collectors.summarizingInt(Map.Entry::getValue)));

        series.forEach(seriesName -> {
          Optional<IntSummaryStatistics> batch =
              Optional.ofNullable(batchData.get(seriesName));
          stackedChart.loadSeriesColorInternal(
              profileTaskQueryKey.getColorProfileName(), seriesName);
          try {
            double y = batch.isPresent()
                ? (sColumnList.isEmpty()
                    ? 0D
                    : (double) batch.get().getSum() / yK)
                : 0D;
            stackedChart.addSeriesValue(x, y, seriesName);
          } catch (Exception ex) {
            log.info(ex);
          }
        });
      }

    } catch (SqlColMetadataException | BeginEndWrongOrderException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void handleFunction(long begin,
                             long end,
                             double yK,
                             Set<String> series,
                             Map<CProfile, LinkedHashSet<String>> topMapSelected,
                             StackedChart stackedChart) {
    try {
      CompositeFilter compositeFilter = FilterHelper.toCompositeFilter(topMapSelected);

      List<StackedColumn> sColumnList =
          getStackedWithPercentile(GroupFunction.COUNT, compositeFilter, begin, end);

      boolean hasPercentile = sColumnList.stream()
          .anyMatch(c -> c.getKeyPercentile() != null
              && !c.getKeyPercentile().isEmpty());

      if (hasPercentile) {
        Map<String, Double> percentileData = sColumnList.stream()
            .map(StackedColumn::getKeyPercentile)
            .filter(Objects::nonNull)
            .flatMap(m -> m.entrySet().stream())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Double::sum));

        series.forEach(seriesName -> {
          stackedChart.loadSeriesColorInternal(
              profileTaskQueryKey.getColorProfileName(), seriesName);
          try {
            stackedChart.addSeriesValue(begin,
                                        percentileData.getOrDefault(seriesName, 0.0) / yK,
                                        seriesName);
          } catch (Exception ex) {
            log.info(ex);
          }
        });

      } else {
        Map<String, IntSummaryStatistics> batchData = sColumnList.stream()
            .map(StackedColumn::getKeyCount)
            .flatMap(sc -> sc.entrySet().stream())
            .collect(Collectors.groupingBy(
                entry -> Objects.requireNonNullElse(entry.getKey(), ""),
                Collectors.summarizingInt(Map.Entry::getValue)));

        series.forEach(seriesName -> {
          Optional<IntSummaryStatistics> batch =
              Optional.ofNullable(batchData.get(seriesName));
          stackedChart.loadSeriesColorInternal(
              profileTaskQueryKey.getColorProfileName(), seriesName);
          try {
            double y = batch.isPresent()
                ? (sColumnList.isEmpty()
                    ? 0D
                    : (double) batch.get().getSum() / yK)
                : 0D;
            stackedChart.addSeriesValue(begin, y, seriesName);
          } catch (Exception ex) {
            log.info(ex);
          }
        });
      }

    } catch (SqlColMetadataException | BeginEndWrongOrderException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<StackedColumn> handleFunctionComplex(long begin, long end)
      throws BeginEndWrongOrderException, SqlColMetadataException {

    CompositeFilter compositeFilter = topMapSelected != null
        ? FilterHelper.toCompositeFilter(topMapSelected)
        : null;

    return getStackedWithPercentile(GroupFunction.COUNT, compositeFilter, begin, end);
  }
}
