package ru.dimension.ui.component.chart.function;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.exception.TableNameEmptyException;
import ru.dimension.db.model.GranularityFunction;
import ru.dimension.db.model.GroupFunction;
import ru.dimension.db.model.PercentileFunction;
import ru.dimension.db.model.filter.CompositeFilter;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.ui.component.chart.FunctionDataHandler;
import ru.dimension.ui.component.chart.StackedChart;
import ru.dimension.ui.exception.SeriesExceedException;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.TimeRangeFunction;
import ru.dimension.ui.model.info.QueryInfo;

@Log4j2
public abstract class FunctionHandler implements FunctionDataHandler {

  protected ProfileTaskQueryKey profileTaskQueryKey;
  protected Metric metric;
  protected QueryInfo queryInfo;
  protected DStore dStore;

  @Getter
  private TProfile tProfile;

  public FunctionHandler(ProfileTaskQueryKey profileTaskQueryKey,
                         Metric metric,
                         QueryInfo queryInfo,
                         DStore dStore) {
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.metric = metric;
    this.queryInfo = queryInfo;
    this.dStore = dStore;
    initTProfile(queryInfo);
  }

  private void initTProfile(QueryInfo queryInfo) {
    try {
      tProfile = this.dStore.getTProfile(queryInfo.getName());
    } catch (TableNameEmptyException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  protected List<StackedColumn> getStackedWithPercentile(GroupFunction groupFunction,
                                                         CompositeFilter compositeFilter,
                                                         long begin,
                                                         long end)
      throws SqlColMetadataException, BeginEndWrongOrderException {

    ru.dimension.ui.model.function.PercentileFunction uiPf = metric.getPercentileFunction();

    if (uiPf == null
        || uiPf == ru.dimension.ui.model.function.PercentileFunction.NONE) {
      return dStore.getStacked(queryInfo.getName(), metric.getYAxis(),
                               groupFunction, compositeFilter, begin, end);
    }

    PercentileFunction dbPf = mapPercentile(uiPf);
    GranularityFunction dbGf = mapGranularity(metric.getTimeRangeFunction());

    return dStore.getStacked(queryInfo.getName(), metric.getYAxis(),
                             groupFunction, dbPf, dbGf,
                             compositeFilter, begin, end);
  }

  protected Map<String, Double> getActiveDoubleMap(StackedColumn column,
                                                   GroupFunction groupFunction) {
    if (column.getKeyPercentile() != null && !column.getKeyPercentile().isEmpty()) {
      return column.getKeyPercentile();
    }
    return switch (groupFunction) {
      case AVG -> column.getKeyAvg() != null ? column.getKeyAvg() : Collections.emptyMap();
      case SUM -> column.getKeySum() != null ? column.getKeySum() : Collections.emptyMap();
      default  -> Collections.emptyMap();
    };
  }

  protected void fillSeries(List<StackedColumn> sColumnList,
                            Set<String> series) {
    boolean hasPercentile = sColumnList.stream()
        .anyMatch(c -> c.getKeyPercentile() != null
            && !c.getKeyPercentile().isEmpty());

    if (hasPercentile) {
      sColumnList.stream()
          .map(StackedColumn::getKeyPercentile)
          .filter(Objects::nonNull)
          .flatMap(m -> m.keySet().stream())
          .filter(Objects::nonNull)
          .forEach(series::add);
    } else {
      Set<String> newSeries = sColumnList.stream()
          .map(StackedColumn::getKeyCount)
          .flatMap(map -> map.keySet().stream())
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
      series.addAll(newSeries);
    }

    if (series.size() > THRESHOLD_SERIES) {
      throw new SeriesExceedException(
          "Column data series exceeds " + THRESHOLD_SERIES
          + ". Not supported to show stacked data.");
    }
  }

  protected void handleFunction(long begin,
                                long end,
                                boolean isClientRealTime,
                                long finalX,
                                double yK,
                                StackedChart stackedChart,
                                GroupFunction groupFunction) {
    try {
      List<StackedColumn> stackedColumns =
          getStackedWithPercentile(groupFunction, null, begin, end);

      long x;
      double y = getY(groupFunction, stackedColumns);

      if (isClientRealTime) {
        x = stackedColumns.isEmpty() ? finalX : stackedColumns.getLast().getTail();
      } else {
        x = finalX;
      }

      stackedChart.addSeriesValue(x, y / yK, metric.getYAxis().getColName());

    } catch (Exception e) {
      log.info(e);
    }
  }

  protected double getY(GroupFunction groupFunction,
                        List<StackedColumn> stackedColumns) {
    double y = 0;

    Optional<StackedColumn> stackedColumn = stackedColumns.stream().findAny();
    if (stackedColumn.isPresent()) {
      Map<String, Double> keyValues =
          getActiveDoubleMap(stackedColumn.get(), groupFunction);

      if (!keyValues.isEmpty()) {
        String colName = metric.getYAxis().getColName();
        Optional<Double> value = keyValues.entrySet().stream()
            .filter(f -> f.getKey().equalsIgnoreCase(colName))
            .map(Map.Entry::getValue)
            .findAny();

        if (value.isPresent()) {
          y = value.get();
        }
      }
    }

    return y;
  }

  private PercentileFunction mapPercentile(
      ru.dimension.ui.model.function.PercentileFunction uiPf) {
    return switch (uiPf) {
      case P50  -> PercentileFunction.P50;
      case P90  -> PercentileFunction.P90;
      case P95  -> PercentileFunction.P95;
      case P99  -> PercentileFunction.P99;
      default   -> PercentileFunction.NONE;
    };
  }

  private GranularityFunction mapGranularity(TimeRangeFunction trf) {
    if (trf == null) return GranularityFunction.AUTO;
    return switch (trf) {
      case MINUTE -> GranularityFunction.MINUTE;
      case HOUR   -> GranularityFunction.HOUR;
      case DAY    -> GranularityFunction.DAY;
      case MONTH  -> GranularityFunction.MONTH;
      default     -> GranularityFunction.AUTO;
    };
  }
}
