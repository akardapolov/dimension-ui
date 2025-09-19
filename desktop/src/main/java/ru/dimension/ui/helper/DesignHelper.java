package ru.dimension.ui.helper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import ru.dimension.ui.model.ProfileTaskQueryKey;

public final class DesignHelper {

  private DesignHelper() {
  }

  public static final String DATE_FORMAT_PATTERN = "dd-MM-yyyy HH:mm:ss";
  public static final String FORMAT_FILE_USED_PATTERN = "yyMMddHHmmss";

  public static DateTimeFormatter getDateFormatFormatter() {
    return DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN, Locale.ENGLISH);
  }

  public static DateTimeFormatter getFileFormatFormatter() {
    return DateTimeFormatter.ofPattern(FORMAT_FILE_USED_PATTERN, Locale.ENGLISH);
  }

  public static String formatDesignName(LocalDateTime dateTime) {
    return "Design - " + dateTime.format(getDateFormatFormatter());
  }

  public static String formatFolderName(LocalDateTime dateTime) {
    return "design_" + dateTime.format(getFileFormatFormatter());
  }

  public static LocalDateTime parseFolderDate(String folderName) {
    String dateStr = folderName.substring(folderName.indexOf("_") + 1);
    return LocalDateTime.parse(dateStr, getFileFormatFormatter());
  }

  public static LocalDateTime parseDesignDate(String designName) {
    String dateStr = designName.substring(designName.indexOf("-") + 1).trim();
    return LocalDateTime.parse(dateStr, getDateFormatFormatter());
  }

  public static ProfileTaskQueryKey stringToKey(String keyString) {
    String[] stringKey = keyString.split("_");
    int profileId = Integer.parseInt(stringKey[0]);
    int taskId = Integer.parseInt(stringKey[1]);
    int queryId = Integer.parseInt(stringKey[2]);
    return new ProfileTaskQueryKey(profileId, taskId, queryId);
  }

  public static String keyToString(ProfileTaskQueryKey key) {
    return key.getProfileId() + "_" +
        key.getTaskId() + "_" +
        key.getQueryId();
  }
}