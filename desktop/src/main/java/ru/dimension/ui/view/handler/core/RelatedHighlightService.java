package ru.dimension.ui.view.handler.core;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Singleton
public class RelatedHighlightService {

  private static final Color HIGHLIGHT_BACKGROUND = new Color(255, 255, 180);
  private static final Color HIGHLIGHT_FOREGROUND = Color.BLACK;

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final JCheckBox checkboxConfig;
  private final ProfileManager profileManager;

  private ColorHighlighter profileHighlighter;
  private ColorHighlighter taskHighlighter;
  private ColorHighlighter connectionHighlighter;
  private ColorHighlighter queryHighlighter;

  @Inject
  public RelatedHighlightService(@Named("profileConfigCase") JXTableCase profileCase,
                                 @Named("taskConfigCase") JXTableCase taskCase,
                                 @Named("connectionConfigCase") JXTableCase connectionCase,
                                 @Named("queryConfigCase") JXTableCase queryCase,
                                 @Named("checkboxConfig") JCheckBox checkboxConfig,
                                 @Named("profileManager") ProfileManager profileManager) {
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.checkboxConfig = checkboxConfig;
    this.profileManager = profileManager;
  }

  public void clearAllHighlights() {
    removeHighlighter(profileCase.getJxTable(), profileHighlighter);
    removeHighlighter(taskCase.getJxTable(), taskHighlighter);
    removeHighlighter(connectionCase.getJxTable(), connectionHighlighter);
    removeHighlighter(queryCase.getJxTable(), queryHighlighter);
    profileHighlighter = null;
    taskHighlighter = null;
    connectionHighlighter = null;
    queryHighlighter = null;
  }

  public void highlightFromProfile(Integer profileId) {
    if (checkboxConfig.isSelected() || profileId == null) {
      clearAllHighlights();
      return;
    }

    ProfileInfo info = profileManager.getProfileInfoById(profileId);
    if (info == null) {
      clearAllHighlights();
      return;
    }

    Set<Integer> taskIds = new HashSet<>(info.getTaskInfoList());
    Set<Integer> connectionIds = new HashSet<>();
    Set<Integer> queryIds = new HashSet<>();

    for (Integer tId : taskIds) {
      TaskInfo ti = profileManager.getTaskInfoById(tId);
      if (ti != null) {
        connectionIds.add(ti.getConnectionId());
        queryIds.addAll(ti.getQueryInfoList());
      }
    }

    removeHighlighter(profileCase.getJxTable(), profileHighlighter);
    profileHighlighter = null;

    applyHighlighter(taskCase, taskIds, TaskRow.class);
    applyHighlighter(connectionCase, connectionIds, ConnectionRow.class);
    applyHighlighter(queryCase, queryIds, QueryRow.class);
  }

  public void highlightFromTask(Integer taskId) {
    if (checkboxConfig.isSelected() || taskId == null) {
      clearAllHighlights();
      return;
    }

    TaskInfo info = profileManager.getTaskInfoById(taskId);
    if (info == null) {
      clearAllHighlights();
      return;
    }

    Set<Integer> connectionIds = new HashSet<>();
    connectionIds.add(info.getConnectionId());

    Set<Integer> queryIds = new HashSet<>(info.getQueryInfoList());

    Set<Integer> profileIds = profileManager.getProfileInfoList().stream()
        .filter(p -> p.getTaskInfoList().contains(taskId))
        .map(ProfileInfo::getId)
        .collect(Collectors.toSet());

    removeHighlighter(taskCase.getJxTable(), taskHighlighter);
    taskHighlighter = null;

    applyHighlighter(profileCase, profileIds, ProfileRow.class);
    applyHighlighter(connectionCase, connectionIds, ConnectionRow.class);
    applyHighlighter(queryCase, queryIds, QueryRow.class);
  }

  public void highlightFromConnection(Integer connectionId) {
    if (checkboxConfig.isSelected() || connectionId == null) {
      clearAllHighlights();
      return;
    }

    Set<Integer> taskIds = profileManager.getTaskInfoList().stream()
        .filter(t -> t.getConnectionId() == connectionId)
        .map(TaskInfo::getId)
        .collect(Collectors.toSet());

    Set<Integer> queryIds = new HashSet<>();
    for (Integer tId : taskIds) {
      TaskInfo ti = profileManager.getTaskInfoById(tId);
      if (ti != null) {
        queryIds.addAll(ti.getQueryInfoList());
      }
    }

    Set<Integer> profileIds = profileManager.getProfileInfoList().stream()
        .filter(p -> p.getTaskInfoList().stream().anyMatch(taskIds::contains))
        .map(ProfileInfo::getId)
        .collect(Collectors.toSet());

    removeHighlighter(connectionCase.getJxTable(), connectionHighlighter);
    connectionHighlighter = null;

    applyHighlighter(profileCase, profileIds, ProfileRow.class);
    applyHighlighter(taskCase, taskIds, TaskRow.class);
    applyHighlighter(queryCase, queryIds, QueryRow.class);
  }

  public void highlightFromQuery(Integer queryId) {
    if (checkboxConfig.isSelected() || queryId == null) {
      clearAllHighlights();
      return;
    }

    Set<Integer> taskIds = profileManager.getTaskInfoList().stream()
        .filter(t -> t.getQueryInfoList().contains(queryId))
        .map(TaskInfo::getId)
        .collect(Collectors.toSet());

    Set<Integer> connectionIds = new HashSet<>();
    for (Integer tId : taskIds) {
      TaskInfo ti = profileManager.getTaskInfoById(tId);
      if (ti != null) {
        connectionIds.add(ti.getConnectionId());
      }
    }

    Set<Integer> profileIds = profileManager.getProfileInfoList().stream()
        .filter(p -> p.getTaskInfoList().stream().anyMatch(taskIds::contains))
        .map(ProfileInfo::getId)
        .collect(Collectors.toSet());

    removeHighlighter(queryCase.getJxTable(), queryHighlighter);
    queryHighlighter = null;

    applyHighlighter(profileCase, profileIds, ProfileRow.class);
    applyHighlighter(taskCase, taskIds, TaskRow.class);
    applyHighlighter(connectionCase, connectionIds, ConnectionRow.class);
  }

  @SuppressWarnings("unchecked")
  private <T> void applyHighlighter(JXTableCase tableCase, Set<Integer> ids, Class<T> rowClass) {
    JXTable table = tableCase.getJxTable();
    if (table == null || ids.isEmpty()) {
      if (rowClass == ProfileRow.class) {
        removeHighlighter(table, profileHighlighter);
        profileHighlighter = null;
      } else if (rowClass == TaskRow.class) {
        removeHighlighter(table, taskHighlighter);
        taskHighlighter = null;
      } else if (rowClass == ConnectionRow.class) {
        removeHighlighter(table, connectionHighlighter);
        connectionHighlighter = null;
      } else if (rowClass == QueryRow.class) {
        removeHighlighter(table, queryHighlighter);
        queryHighlighter = null;
      }
      return;
    }

    TTTable<T, JXTable> tt = tableCase.getTypedTable();

    HighlightPredicate predicate = (renderer, adapter) -> {
      if (rowClass == ConnectionRow.class) {
        String columnName = table.getColumnName(adapter.column);
        if ("Status".equals(columnName)) {
          return false;
        }
      }

      int modelRow = adapter.convertRowIndexToModel(adapter.row);
      T row = tt.model().itemAt(modelRow);
      if (row == null) return false;
      Integer rowId = extractId(row);
      return rowId != null && ids.contains(rowId);
    };

    ColorHighlighter highlighter = new ColorHighlighter(predicate, HIGHLIGHT_BACKGROUND, HIGHLIGHT_FOREGROUND);

    if (rowClass == ProfileRow.class) {
      removeHighlighter(table, profileHighlighter);
      profileHighlighter = highlighter;
    } else if (rowClass == TaskRow.class) {
      removeHighlighter(table, taskHighlighter);
      taskHighlighter = highlighter;
    } else if (rowClass == ConnectionRow.class) {
      removeHighlighter(table, connectionHighlighter);
      connectionHighlighter = highlighter;
    } else if (rowClass == QueryRow.class) {
      removeHighlighter(table, queryHighlighter);
      queryHighlighter = highlighter;
    }

    table.addHighlighter(highlighter);
    table.repaint();
  }

  private Integer extractId(Object row) {
    if (row instanceof ProfileRow r) return r.getId();
    if (row instanceof TaskRow r) return r.getId();
    if (row instanceof ConnectionRow r) return r.getId();
    if (row instanceof QueryRow r) return r.getId();
    return null;
  }

  private void removeHighlighter(JXTable table, ColorHighlighter highlighter) {
    if (table != null && highlighter != null) {
      table.removeHighlighter(highlighter);
      table.repaint();
    }
  }
}