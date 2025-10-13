package ru.dimension.ui.helper;

import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ConnectionInfo;

@Log4j2
public final class KeyHelper {

  private static final int VALUE_MAX_CHARS = 40; // conservative char limit per cell

  private static final int TD_PROFILE_WIDTH_PX = 240;
  private static final int TD_TASK_WIDTH_PX    = 260;
  private static final int TD_QUERY_WIDTH_PX   = 240;
  private static final int TD_COLUMN_WIDTH_PX  = 260;

  private KeyHelper() {}

  public static AdHocKey getAdHocKey(ConnectionInfo connectionInfo, String tableName, CProfile cProfile) {
    return new AdHocKey(connectionInfo.getId(), tableName, cProfile.getColId());
  }

  public static String getGlobalKey(ConnectionInfo connectionInfo, String tableName) {
    return connectionInfo.getId() + "_" + tableName;
  }

  public static String getGlobalKey(int id, String tableName) {
    return id + "_" + tableName;
  }

  public static String getKey(ProfileManager profileManager,
                              ProfileTaskQueryKey key,
                              CProfile cProfile) {
    String profileName;
    String taskName;
    String queryName;
    String columnName;

    try {
      var profileInfo = profileManager.getProfileInfoById(key.getProfileId());
      profileName = profileInfo != null && profileInfo.getName() != null ? profileInfo.getName() : ("#" + key.getProfileId());
    } catch (Exception e) {
      log.debug("Failed to load profile info for id={}", key.getProfileId(), e);
      profileName = "#" + key.getProfileId();
    }

    try {
      var taskInfo = profileManager.getTaskInfoById(key.getTaskId());
      taskName = taskInfo != null && taskInfo.getName() != null ? taskInfo.getName() : ("#" + key.getTaskId());
    } catch (Exception e) {
      log.debug("Failed to load task info for id={}", key.getTaskId(), e);
      taskName = "#" + key.getTaskId();
    }

    try {
      var queryInfo = profileManager.getQueryInfoById(key.getQueryId());
      queryName = queryInfo != null && queryInfo.getName() != null ? queryInfo.getName() : ("#" + key.getQueryId());
    } catch (Exception e) {
      log.debug("Failed to load query info for id={}", key.getQueryId(), e);
      queryName = "#" + key.getQueryId();
    }

    if (cProfile != null) {
      columnName = cProfile.getColName() != null ? cProfile.getColName() : ("#" + cProfile.getColId());
    } else {
      columnName = "";
    }

    // Truncate just to keep tooltips/labels reasonable; no padding (HTML collapses spaces anyway)
    profileName = truncateWithEllipsis(profileName, VALUE_MAX_CHARS);
    taskName    = truncateWithEllipsis(taskName,    VALUE_MAX_CHARS);
    queryName   = truncateWithEllipsis(queryName,   VALUE_MAX_CHARS);
    columnName  = truncateWithEllipsis(columnName,  VALUE_MAX_CHARS);

    return titleHtml(profileName, taskName, queryName, columnName);
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
    return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
  }

  private static String td(String label, String value, int widthPx) {
    return String.format("<td width='%d'><b>%s</b> %s</td>", widthPx, esc(label), esc(value));
  }

  private static String titleHtml(String profile, String task, String query, String column) {
    return "<html><table cellspacing='0' cellpadding='0'><tr>"
        + td("Profile:", profile, TD_PROFILE_WIDTH_PX)
        + td("Task:",    task,    TD_TASK_WIDTH_PX)
        + td("Query:",   query,   TD_QUERY_WIDTH_PX)
        + td("Column:",  column,  TD_COLUMN_WIDTH_PX)
        + "</tr></table></html>";
  }
}