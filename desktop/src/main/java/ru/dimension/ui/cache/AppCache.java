package ru.dimension.ui.cache;

import java.util.List;
import java.util.Map;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.info.gui.RangeInfo;

public interface AppCache {

  /**
   * Put methods for config
   */
  void putProfileInfo(ProfileInfo profileInfo);

  void putTaskInfo(TaskInfo taskInfo);

  void putConnectionInfo(ConnectionInfo connectionInfo);

  void putQueryInfo(QueryInfo queryInfo);

  void putTableInfo(TableInfo tableInfo);

  void putChartInfo(ChartInfo chartInfo);

  /**
   * Get methods for config
   */
  void deleteProfileInfo(int profileId);

  void deleteTaskInfo(int taskId);

  void deleteConnectionInfo(int connectionId);

  void deleteQueryInfo(int queryId);

  void deleteTableInfo(String tableName);

  void deleteChartInfo(int chartId);

  /**
   * Delete methods for config
   */
  ProfileInfo getProfileInfo(int profileId);

  TaskInfo getTaskInfo(int taskId);

  ConnectionInfo getConnectionInfo(int connectionId);

  QueryInfo getQueryInfo(int queryId);

  TableInfo getTableInfo(String tableName);

  ChartInfo getChartInfo(int chartId);

  /**
   * Get all config maps
   */
  Map<Integer, ProfileInfo> getProfileInfoMap();

  Map<Integer, TaskInfo> getTaskInfoMap();

  Map<Integer, ConnectionInfo> getConnectionInfo();

  Map<Integer, QueryInfo> getQueryInfo();

  Map<String, TableInfo> getTableInfo();

  Map<Integer, ChartInfo> getChartInfo();

  /**
   * Clear config maps
   */
  void clearProfileInfo();

  void clearTaskInfo();

  void clearConnectionInfo();

  void clearQueryInfo();

  void clearTableInfo();

  void clearChartInfo();

  /**
   * Put and Get methods for time range
   */
  void putRangeInfo(ProfileTaskQueryKey profileTaskQueryKey,
                    RangeInfo rangeInfo);

  List<RangeInfo> getRangeInfo(ProfileTaskQueryKey profileTaskQueryKey);
}
