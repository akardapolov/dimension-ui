package ru.dimension.ui.component.module.adhoc.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.router.event.EventListener;

@Data
@Log4j2
public class AdHocModelModel {
  private final ProfileManager profileManager;
  private final ConfigurationManager configurationManager;
  private final Map<Integer, Connection> connectionMap;
  private final EventListener eventListener;
  private final ConnectionPoolManager connectionPoolManager;
  private final AdHocDatabaseManager adHocDatabaseManager;

  private final Map<Integer, Map<String, Set<Integer>>> selectionState = new HashMap<>();

  public AdHocModelModel(ProfileManager profileManager,
                         ConfigurationManager configurationManager,
                         EventListener eventListener,
                         ConnectionPoolManager connectionPoolManager,
                         AdHocDatabaseManager adHocDatabaseManager) {
    this.profileManager = profileManager;
    this.configurationManager = configurationManager;
    this.eventListener = eventListener;
    this.connectionPoolManager = connectionPoolManager;
    this.adHocDatabaseManager = adHocDatabaseManager;
    this.connectionMap = new HashMap<>();
  }

  public void setTableOrViewSelected(int connectionId, String schemaDotTableOrView, boolean selected) {
    Map<String, Set<Integer>> tableMap = selectionState.computeIfAbsent(connectionId, k -> new HashMap<>());

    if (selected) {
      tableMap.computeIfAbsent(schemaDotTableOrView, k -> new HashSet<>());
    } else {
      tableMap.remove(schemaDotTableOrView);
    }
  }

  public boolean isTableOrViewSelected(int connectionId, String schemaDotTableOrView) {
    Map<String, Set<Integer>> tableMap = selectionState.get(connectionId);
    return tableMap != null && tableMap.containsKey(schemaDotTableOrView);
  }

  public void setColumnSelected(int connectionId, String schemaDotTableOrView, int columnId, boolean selected) {
    Map<String, Set<Integer>> tableMap = selectionState.computeIfAbsent(connectionId, k -> new HashMap<>());
    Set<Integer> columns = tableMap.computeIfAbsent(schemaDotTableOrView, k -> new HashSet<>());

    if (selected) {
      columns.add(columnId);
    } else {
      columns.remove(columnId);
    }
  }

  public boolean isColumnSelected(int connectionId, String schemaDotTableOrView, int columnId) {
    Map<String, Set<Integer>> tableMap = selectionState.get(connectionId);
    if (tableMap == null) return false;

    Set<Integer> columns = tableMap.get(schemaDotTableOrView);
    return columns != null && columns.contains(columnId);
  }

  public Set<Integer> getSelectedColumns(int connectionId, String schemaDotTableOrView) {
    Map<String, Set<Integer>> tableMap = selectionState.get(connectionId);
    if (tableMap == null) return new HashSet<>();

    Set<Integer> columns = tableMap.get(schemaDotTableOrView);
    return columns != null ? columns : new HashSet<>();
  }
}