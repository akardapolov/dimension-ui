package ru.dimension.ui.component.chart;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TimeZone;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.model.OrderBy;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.chart.RangeBatchSize;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.component.chart.function.AverageHandler;
import ru.dimension.ui.component.chart.function.CountHandler;
import ru.dimension.ui.component.chart.function.SumHandler;

public interface HelperChart {
  int MAX_POINTS_PER_GRAPH = 300;

  double GAP = 2; // if gap more than this value - then show one on chart

  int THRESHOLD_SERIES = 15;
  int SHOW_SERIES = 10;
  int MAX_SERIES = 1000;

  default FunctionDataHandler initFunctionDataHandler(Metric metric,
                                                      QueryInfo queryInfo,
                                                      DStore dStore) {
    return switch (metric.getMetricFunction()) {
      case COUNT -> new CountHandler(metric, queryInfo, dStore);
      case SUM -> new SumHandler(metric, queryInfo, dStore);
      case AVG -> new AverageHandler(metric, queryInfo, dStore);
      case NONE -> null;
    };
  }

  default String getHourMinSec(long duration) {
    if (duration < 0) {
      return "Local time lagging behind than server one at: " + getDurationAbs(Math.abs(duration));
    } else if (duration > 0) {
      return "Local time ahead of server one at: " + getDurationAbs(duration);
    } else {
      return "Local and server time are synchronous";
    }
  }

  default String getDurationAbs(long duration) {
    Duration d = Duration.ofMillis(duration);

    long HH = d.toHours();
    long MM = d.toMinutesPart();
    long SS = d.toSecondsPart();
    long MS = d.toMillisPart();
    return String.format("%02d hour %02d minute %02d seconds %03d milliseconds", HH, MM, SS, MS);
  }

  default long getRange(ChartInfo chartInfo, boolean isRealTime) {
    if (isRealTime) {
      return getRangeRealTime(chartInfo);
    } else {
      return getRangeHistory(chartInfo);
    }
  }

  default long getRangeRealTime(ChartInfo chartInfo) {
    return ((long) chartInfo.getRangeRealtime().getMinutes() * 60 * 1000);
  }

  default long getRangeHistory(ChartInfo chartInfo) {
    if (RangeHistory.DAY.equals(chartInfo.getRangeHistory())) {
      return 24L * 60 * 60 * 1000;
    }
    if (RangeHistory.WEEK.equals(chartInfo.getRangeHistory())) {
      return 7L * 24 * 60 * 60 * 1000;
    }
    if (RangeHistory.MONTH.equals(chartInfo.getRangeHistory())) {
      return 30L * 24 * 60 * 60 * 1000;
    } else {
      return (chartInfo.getCustomEnd() - chartInfo.getCustomBegin());
    }
  }

  default RangeBatchSize getRangeBatchSize(ChartInfo chartInfo, boolean isRealTime) {
    double range;
    int batchSize;

    range = (double) getRange(chartInfo, isRealTime) / MAX_POINTS_PER_GRAPH;

    if (range / 1000 < chartInfo.getPullTimeoutClient()) {
      range = (double) chartInfo.getPullTimeoutClient() * 1000;
    }

    batchSize = Math.toIntExact(Math.round((range / 1000) / chartInfo.getPullTimeoutClient()));

    return new RangeBatchSize(range, batchSize);
  }

  default ChartRange getChartRange(ChartInfo chartInfo) {

    ChartRange chartRange = new ChartRange();

    LocalDateTime startOfDay = LocalDate.now().plusDays(1).atStartOfDay();
    long end = startOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;

    if (RangeHistory.CUSTOM.equals(chartInfo.getRangeHistory())) {
      chartRange.setBegin(chartInfo.getCustomBegin());
      chartRange.setEnd(chartInfo.getCustomEnd());
    } else {
      chartRange.setEnd(end);
      chartRange.setBegin(chartRange.getEnd() - getRangeHistory(chartInfo) + 1);
    }

    return chartRange;
  }

  default ChartRange getChartRangeAdHoc(ChartInfo chartInfo) {

    ChartRange chartRange = new ChartRange();

    if (RangeHistory.CUSTOM.equals(chartInfo.getRangeHistory())) {
      chartRange.setBegin(chartInfo.getCustomBegin());
      chartRange.setEnd(chartInfo.getCustomEnd());
    } else {
      chartRange.setEnd(chartInfo.getCustomEnd());
      chartRange.setBegin(chartRange.getEnd() - getRangeHistory(chartInfo) + 1);
    }

    return chartRange;
  }

  default ChartRange getChartRangeExact(ChartInfo chartInfo) {
    ChartRange chartRange = new ChartRange();

    if (chartInfo.getCustomBegin() != 0 & chartInfo.getCustomEnd() != 0) {
      chartRange.setBegin(chartInfo.getCustomBegin());
      chartRange.setEnd(chartInfo.getCustomEnd());
    }

    return chartRange;
  }

  default LocalDateTime toLocalDateTimeOfEpochMilli(long ofEpochMilli) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(ofEpochMilli), TimeZone.getDefault().toZoneId());
  }

  default double getK(double range,
                      ChartInfo chartInfo) {
    double k;
    if (range / 1000 < chartInfo.getPullTimeoutClient()) {
      k = chartInfo.getPullTimeoutClient();
    } else {
      k = range / 1000;
    }
    return k;
  }

  default List<String> getDistinct(DStore dStore,
                                   String tableName,
                                   CProfile cProfile,
                                   ChartRange chartRange,
                                   int limit) throws BeginEndWrongOrderException {

    return dStore.getDistinct(tableName,
                              cProfile,
                              OrderBy.DESC,
                              null,
                              limit,
                              chartRange.getBegin(),
                              chartRange.getEnd());
  }

  default ChartRange getChartRange(DStore dStore,
                                   String tableName,
                                   ChartInfo chartInfo) {
    ChartRange chartRange = new ChartRange();

    if (RangeHistory.CUSTOM.equals(chartInfo.getRangeHistory())) {
      chartRange.setBegin(chartInfo.getCustomBegin());
      chartRange.setEnd(chartInfo.getCustomEnd());
    } else {
      long last = dStore.getLast(tableName, Long.MIN_VALUE, Long.MAX_VALUE);

      if (Long.MIN_VALUE == last) {
        throw new RuntimeException("No data found in table: " + tableName);
      }

      LocalDateTime dateTime = Instant.ofEpochMilli(last)
          .atZone(ZoneId.systemDefault())
          .toLocalDateTime()
          .plusDays(1)
          .truncatedTo(ChronoUnit.DAYS);

      long end = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
      long begin = end - getRangeHistory(chartInfo) + 1;

      chartRange.setBegin(begin);
      chartRange.setEnd(end);
    }

    return chartRange;
  }
}
