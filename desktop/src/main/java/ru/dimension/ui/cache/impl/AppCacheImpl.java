package ru.dimension.ui.cache.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.cache.AppCache;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.info.gui.RangeInfo;

@Log4j2
@Singleton
public class AppCacheImpl implements AppCache {

  private final Map<Integer, ProfileInfo> profileInfoMap = new ConcurrentHashMap<>();
  private final Map<Integer, TaskInfo> taskInfoMap = new ConcurrentHashMap<>();
  private final Map<Integer, ConnectionInfo> connectionInfoMap = new ConcurrentHashMap<>();
  private final Map<Integer, QueryInfo> queryInfoMap = new ConcurrentHashMap<>();
  private final Map<String, TableInfo> tableInfoMap = new ConcurrentHashMap<>();
  private final Map<Integer, ChartInfo> chartInfoMap = new ConcurrentHashMap<>();

  private final Map<ProfileTaskQueryKey, List<RangeInfo>> rangeInfoMap = new ConcurrentHashMap<>();

  @Inject
  public AppCacheImpl() {
  }

  /**
   * Put methods for config
   */
  @Override
  public void putProfileInfo(ProfileInfo profileInfo) {
    profileInfoMap.put(profileInfo.getId(), profileInfo);
  }

  @Override
  public void putTaskInfo(TaskInfo taskInfo) {
    taskInfoMap.put(taskInfo.getId(), taskInfo);
  }

  @Override
  public void putConnectionInfo(ConnectionInfo connectionInfo) {
    connectionInfoMap.put(connectionInfo.getId(), connectionInfo);
  }

  @Override
  public void putQueryInfo(QueryInfo queryInfo) {
    queryInfoMap.put(queryInfo.getId(), queryInfo);
  }

  @Override
  public void putTableInfo(TableInfo tableInfo) {
    tableInfoMap.put(tableInfo.getTableName(), tableInfo);
  }

  @Override
  public void putChartInfo(ChartInfo chartInfo) {
    chartInfoMap.put(chartInfo.getId(), chartInfo);
  }

  @Override
  public void deleteProfileInfo(int profileId) {
    profileInfoMap.remove(profileId);
  }

  @Override
  public void deleteTaskInfo(int taskId) {
    taskInfoMap.remove(taskId);
  }

  @Override
  public void deleteConnectionInfo(int connectionId) {
    connectionInfoMap.remove(connectionId);
  }

  @Override
  public void deleteQueryInfo(int queryId) {
    queryInfoMap.remove(queryId);
  }

  @Override
  public void deleteTableInfo(String tableName) {
    tableInfoMap.remove(tableName);
  }

  @Override
  public void deleteChartInfo(int chartId) {
    chartInfoMap.remove(chartId);
  }

  /**
   * Get methods for config
   */
  @Override
  public ProfileInfo getProfileInfo(int profileId) {
    return profileInfoMap.get(profileId);
  }

  @Override
  public TaskInfo getTaskInfo(int taskId) {
    return taskInfoMap.get(taskId);
  }

  @Override
  public ConnectionInfo getConnectionInfo(int connectionId) {
    return connectionInfoMap.get(connectionId);
  }

  @Override
  public QueryInfo getQueryInfo(int queryId) {
    return queryInfoMap.get(queryId);
  }

  @Override
  public TableInfo getTableInfo(String tableName) {
    return tableInfoMap.get(tableName);
  }

  @Override
  public ChartInfo getChartInfo(int chartId) {
    return chartInfoMap.get(chartId);
  }

  /**
   * Put and Get methods for time range
   */
  @Override
  public void putRangeInfo(ProfileTaskQueryKey profileTaskQueryKey,
                           RangeInfo rangeInfo) {
    if (!rangeInfoMap.containsKey(profileTaskQueryKey)) {
      rangeInfoMap.put(profileTaskQueryKey, new ArrayList<>());
    }

    rangeInfoMap.get(profileTaskQueryKey).add(rangeInfo);
  }

  @Override
  public List<RangeInfo> getRangeInfo(ProfileTaskQueryKey profileTaskQueryKey) {
    return Optional.ofNullable(rangeInfoMap.get(profileTaskQueryKey))
        .orElse(Collections.emptyList());
  }

  /**
   * Get all config maps
   */
  @Override
  public Map<Integer, ProfileInfo> getProfileInfoMap() {
    return profileInfoMap;
  }

  @Override
  public Map<Integer, TaskInfo> getTaskInfoMap() {
    return taskInfoMap;
  }

  @Override
  public Map<Integer, ConnectionInfo> getConnectionInfo() {
    return connectionInfoMap;
  }

  @Override
  public Map<Integer, QueryInfo> getQueryInfo() {
    return queryInfoMap;
  }

  @Override
  public Map<String, TableInfo> getTableInfo() {
    return tableInfoMap;
  }

  @Override
  public Map<Integer, ChartInfo> getChartInfo() {
    return chartInfoMap;
  }

  @Override
  public void clearProfileInfo() {
    profileInfoMap.clear();
  }

  @Override
  public void clearTaskInfo() {
    taskInfoMap.clear();
  }

  @Override
  public void clearConnectionInfo() {
    connectionInfoMap.clear();
  }

  @Override
  public void clearQueryInfo() {
    queryInfoMap.clear();
  }

  @Override
  public void clearTableInfo() {
    tableInfoMap.clear();
  }

  @Override
  public void clearChartInfo() {
    chartInfoMap.clear();
  }
}

