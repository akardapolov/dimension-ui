package ru.dimension.ui.helper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.date.DateLocale;

@Log4j2
public class DateHelper {
  public static String formatPattern = "dd-MM-yyyy HH:mm:ss";

  private DateHelper() {}

  public static String getDateFormatted(DateLocale dateLocale, long epochMilli) {
    return Instant.ofEpochMilli(epochMilli)
        .atZone(ZoneId.systemDefault())
        .format(getDateTimeFormatterByLocale(dateLocale));
  }

  public static DateTimeFormatter getDateTimeFormatterByLocale(DateLocale dateLocale) {
    return switch (dateLocale) {
      case RU -> DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss", Locale.forLanguageTag("ru-RU"));
      default -> DateTimeFormatter.ofPattern("MMMM dd yyyy HH:mm:ss", Locale.ENGLISH);
    };
  }

  public static long getNowMilli(ZoneId zoneId) {
    LocalDateTime now = LocalDateTime.now();
    return now.atZone(zoneId).toInstant().toEpochMilli();
  }

  /**
   * Key - begin date
   * Value - end date
   * @return Map.Entry<Date, Date>
   */
  public static Map.Entry<Date, Date> getRangeDate() {
    LocalDateTime startOfDay = LocalDate.now().plusDays(1).atStartOfDay();
    long end = startOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
    long begin = end - (24L * 60 * 60 * 1000) + 1;

    Date beginDate = new Date(begin);
    Date endDate = new Date(end);

    return Map.entry(beginDate, endDate);
  }

  public static String getDateFormattedRange(DateLocale dateLocale, long begin, long end, boolean splitOn) {
    if (begin > end) {
      long temp = begin;
      begin = end;
      end = temp;
    }

    ZoneId zone = ZoneId.systemDefault();
    ZonedDateTime startZdt = Instant.ofEpochMilli(begin).atZone(zone);
    ZonedDateTime endZdt = Instant.ofEpochMilli(end).atZone(zone);

    String startStr;
    String endStr;

    if (startZdt.getYear() != endZdt.getYear() ||
        startZdt.getMonth() != endZdt.getMonth() ||
        startZdt.getDayOfMonth() != endZdt.getDayOfMonth()) {
      startStr = formatFull(dateLocale, begin);
      endStr = formatFull(dateLocale, end);
    } else {
      String commonDate = formatDateOnly(dateLocale, startZdt);
      if (startZdt.getHour() != endZdt.getHour()) {
        startStr = commonDate + " " + formatTime(startZdt);
        endStr = formatTime(endZdt);
      } else if (startZdt.getMinute() != endZdt.getMinute()) {
        String commonHour = String.format("%02d", startZdt.getHour());
        startStr = commonDate + " " + commonHour + " " + formatMinSec(startZdt);
        endStr = formatMinSec(endZdt);
      } else if (startZdt.getSecond() != endZdt.getSecond()) {
        String commonHourMinute = String.format("%02d:%02d", startZdt.getHour(), startZdt.getMinute());
        startStr = commonDate + " " + commonHourMinute + ":" + String.format("%02d", startZdt.getSecond());
        endStr = String.format("%02d", endZdt.getSecond());
      } else {
        String commonHourMinute = String.format("%02d:%02d", startZdt.getHour(), startZdt.getMinute());
        startStr = commonDate + " " + commonHourMinute + ":" + String.format("%02d", startZdt.getSecond());
        endStr = String.format("%02d", startZdt.getSecond());
      }
    }

    if (splitOn) {
      return startStr + " -\n" + endStr;
    } else {
      return startStr + " - " + endStr;
    }
  }

  private static String formatFull(DateLocale dateLocale, long epochMilli) {
    return Instant.ofEpochMilli(epochMilli)
        .atZone(ZoneId.systemDefault())
        .format(getDateTimeFormatterByLocale(dateLocale));
  }

  private static String formatDateOnly(DateLocale dateLocale, ZonedDateTime zdt) {
    DateTimeFormatter formatter = getDateOnlyFormatterByLocale(dateLocale);
    return zdt.format(formatter);
  }

  private static String formatMinSec(ZonedDateTime zdt) {
    return String.format("%02d:%02d", zdt.getMinute(), zdt.getSecond());
  }

  private static DateTimeFormatter getDateOnlyFormatterByLocale(DateLocale dateLocale) {
    return switch (dateLocale) {
      case RU -> DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("ru-RU"));
      default -> DateTimeFormatter.ofPattern("MMMM dd yyyy", Locale.ENGLISH);
    };
  }

  private static String formatTime(ZonedDateTime zdt) {
    return zdt.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
  }
}
