package ru.dimension.ui.helper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Data
public class ReportHelper {

  private String formatPattern = "dd-MM-yyyy HH:mm:ss";
  private String formatFusedPattern = "yyMMddHHmmss";
  private boolean madeChanges;

  public ReportHelper() {
    this.madeChanges = false;
  }

  public String getDateFormat(long date) {
    return Instant.ofEpochMilli(date)
        .atZone(ZoneId.systemDefault())
        .format(getDateTimeFormatter())
        .toString();
  }

  public String getDateFormatFused(long date) {
    return Instant.ofEpochMilli(date)
        .atZone(ZoneId.systemDefault())
        .format(getDateTimeFormatterFused())
        .toString();
  }

  public DateTimeFormatter getDateTimeFormatter() {
    return DateTimeFormatter.ofPattern(formatPattern, Locale.ENGLISH);
  }

  public DateTimeFormatter getDateTimeFormatterFused() {
    return DateTimeFormatter.ofPattern(formatFusedPattern, Locale.ENGLISH);
  }

  public String getFormatPattern() {
    return formatPattern;
  }
}
