package ru.dimension.ui.helper;

import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ConnectionInfo;

@Log4j2
public final class KeyHelper {

  private static final int VALUE_MAX_CHARS = 30;
  private static final int COLUMN_MAX_CHARS = 30;

  private static final int TD_PROFILE_WIDTH_PX = 240;
  private static final int TD_TASK_WIDTH_PX    = 240;
  private static final int TD_QUERY_WIDTH_PX   = 240;
  private static final int TD_COLUMN_WIDTH_PX  = 300;

  private KeyHelper() {}

  public record TitleInfo(String shortTitle, String fullTitle) {}

  public static AdHocKey getAdHocKey(ConnectionInfo connectionInfo, String tableName, CProfile cProfile) {
    return new AdHocKey(connectionInfo.getId(), tableName, cProfile.getColId());
  }

  public static String getGlobalKey(ConnectionInfo connectionInfo, String tableName) {
    return connectionInfo.getId() + "_" + tableName;
  }

  public static String getGlobalKey(int id, String tableName) {
    return id + "_" + tableName;
  }

  public static TitleInfo getTitle(ProfileManager profileManager,
                                   ProfileTaskQueryKey key,
                                   CProfile cProfile) {
    String profileName = getProfileName(profileManager, key);
    String taskName = getTaskName(profileManager, key);
    String queryName = getQueryName(profileManager, key);

    String columnNameFull;
    if (cProfile != null) {
      columnNameFull = cProfile.getColName() != null ? cProfile.getColName() : ("#" + cProfile.getColId());
    } else {
      columnNameFull = "";
    }

    ColumnNameFormatter.FormattedName formattedColumn =
        ColumnNameFormatter.format(columnNameFull, COLUMN_MAX_CHARS);

    String shortProfile = truncateWithEllipsis(profileName, VALUE_MAX_CHARS);
    String shortTask = truncateWithEllipsis(taskName, VALUE_MAX_CHARS);
    String shortQuery = truncateWithEllipsis(queryName, VALUE_MAX_CHARS);
    String shortColumn = formattedColumn.getShortName();

    String fullProfile = profileName;
    String fullTask = taskName;
    String fullQuery = queryName;
    String fullColumn = formattedColumn.getFullName();

    String shortTitle = titleHtml(shortProfile, shortTask, shortQuery, shortColumn);
    String fullTitle = tooltipHtml(fullProfile, fullTask, fullQuery, fullColumn);

    return new TitleInfo(shortTitle, fullTitle);
  }

  public static String getKey(ProfileManager profileManager,
                              ProfileTaskQueryKey key,
                              CProfile cProfile) {
    return getTitle(profileManager, key, cProfile).shortTitle();
  }

  private static String getProfileName(ProfileManager profileManager, ProfileTaskQueryKey key) {
    try {
      var profileInfo = profileManager.getProfileInfoById(key.getProfileId());
      return profileInfo != null && profileInfo.getName() != null
          ? profileInfo.getName()
          : ("#" + key.getProfileId());
    } catch (Exception e) {
      log.debug("Failed to load profile info for id={}", key.getProfileId(), e);
      return "#" + key.getProfileId();
    }
  }

  private static String getTaskName(ProfileManager profileManager, ProfileTaskQueryKey key) {
    try {
      var taskInfo = profileManager.getTaskInfoById(key.getTaskId());
      return taskInfo != null && taskInfo.getName() != null
          ? taskInfo.getName()
          : ("#" + key.getTaskId());
    } catch (Exception e) {
      log.debug("Failed to load task info for id={}", key.getTaskId(), e);
      return "#" + key.getTaskId();
    }
  }

  private static String getQueryName(ProfileManager profileManager, ProfileTaskQueryKey key) {
    try {
      var queryInfo = profileManager.getQueryInfoById(key.getQueryId());
      return queryInfo != null && queryInfo.getName() != null
          ? queryInfo.getName()
          : ("#" + key.getQueryId());
    } catch (Exception e) {
      log.debug("Failed to load query info for id={}", key.getQueryId(), e);
      return "#" + key.getQueryId();
    }
  }

  private static String truncateWithEllipsis(String text, int maxCodePoints) {
    if (text == null) return "";
    if (maxCodePoints <= 0) return "..";
    final int len = text.length();
    if (text.codePointCount(0, len) <= maxCodePoints) {
      return text;
    }
    StringBuilder sb = new StringBuilder(Math.min(len, maxCodePoints) + 2);
    int count = 0;
    for (int i = 0; i < len && count < maxCodePoints; ) {
      int cp = text.codePointAt(i);
      sb.appendCodePoint(cp);
      i += Character.charCount(cp);
      count++;
    }
    return sb.append("..").toString();
  }

  private static String esc(String s) {
    if (s == null) return "";
    return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
  }

  private static String td(String label, String value, int widthPx) {
    return String.format(
        "<td width='%d' style='white-space: nowrap;'><b>%s</b>&nbsp;%s</td>",
        widthPx, esc(label), esc(value)
    );
  }

  private static String titleHtml(String profile, String task, String query, String column) {
    return "<html><table cellspacing='0' cellpadding='0'><tr>"
        + td("Profile:", profile, TD_PROFILE_WIDTH_PX)
        + td("Task:",    task,    TD_TASK_WIDTH_PX)
        + td("Query:",   query,   TD_QUERY_WIDTH_PX)
        + td("Column:",  column,  TD_COLUMN_WIDTH_PX)
        + "</tr></table></html>";
  }

  private static String tooltipHtml(String profile, String task, String query, String column) {
    return "<html>"
        + "<b>Profile:</b> " + esc(profile) + "<br>"
        + "<b>Task:</b> " + esc(task) + "<br>"
        + "<b>Query:</b> " + esc(query) + "<br>"
        + "<b>Column:</b> " + esc(column)
        + "</html>";
  }
}