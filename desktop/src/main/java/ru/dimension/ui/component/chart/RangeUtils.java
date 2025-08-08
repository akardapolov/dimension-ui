package ru.dimension.ui.component.chart;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.model.date.DateLocale;

public class RangeUtils {

  private static final int MIN5_SUBRANGE = 1000;  // 1 секунда
  private static final int MIN10_SUBRANGE = 2000; // 2 секунды
  private static final int MIN30_SUBRANGE = 6000; // 6 секунд
  private static final int MIN60_SUBRANGE = 12000; // 12 секунд

  public static List<TimeRange> calculateRanges(long serverTimeMillis, int rangeMinutes) {
    List<TimeRange> ranges = new ArrayList<>();

    // Вычисляем начало диапазона (currentTime - rangeMinutes)
    long rangeStart = serverTimeMillis - (rangeMinutes * 60 * 1000L);

    // Округляем начало диапазона до целой минуты вниз
    rangeStart = (rangeStart / 60000) * 60000;

    // Получаем размер поддиапазона для текущего диапазона
    int subRangeSize = getSubRangeSize(rangeMinutes);

    // Генерируем поддиапазоны по минутам
    long currentSubRange = rangeStart;
    while (currentSubRange < serverTimeMillis) {
      long nextSubRange = currentSubRange + subRangeSize;
      ranges.add(new TimeRange(currentSubRange, nextSubRange - 1));
      currentSubRange = nextSubRange;
    }

    return ranges;
  }

  public static class TimeRange {
    private final long start;
    private final long end;

    public TimeRange(long start, long end) {
      this.start = start;
      this.end = end;
    }

    public long getStart() {
      return start;
    }

    public long getEnd() {
      return end;
    }

    @Override
    public String toString() {
      return "ChartRange{" +
          "begin=" + DateHelper.getDateFormatted(DateLocale.RU, start) +
          ", end=" + DateHelper.getDateFormatted(DateLocale.RU, end) +
          '}';
    }

    protected String getDate(long l) {
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
      Date date = new Date(l);
      return dateFormat.format(date);
    }
  }

  public static int getSubRangeSize(int rangeMinutes) {
    return switch (rangeMinutes) {
      case 5 -> MIN5_SUBRANGE;
      case 10 -> MIN10_SUBRANGE;
      case 30 -> MIN30_SUBRANGE;
      case 60 -> MIN60_SUBRANGE;
      default -> throw new IllegalArgumentException("Unsupported range: " + rangeMinutes);
    };
  }

  public static LocalDateTime toLocalDateTime(long ofEpochMilli) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(ofEpochMilli), TimeZone.getDefault().toZoneId());
  }
}