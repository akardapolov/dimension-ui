package ru.dimension.ui.view.table;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;
import ru.dimension.ui.view.table.row.Rows.MetadataRow;
import ru.dimension.ui.view.table.row.Rows.MetricRow;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;
import ru.dimension.ui.view.table.row.Rows.TaskLinkRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;
import ru.dimension.ui.view.table.row.Rows.TimestampColumnRow;

public class ModelRowMapper {

  public static List<ProfileRow> mapProfiles(List<ProfileInfo> infos) {
    return infos.stream()
        .map(p -> new ProfileRow(p.getId(), p.getName()))
        .collect(Collectors.toList());
  }

  public static List<TaskRow> mapTasks(List<TaskInfo> infos) {
    return infos.stream()
        .map(t -> new TaskRow(t.getId(), t.getName()))
        .collect(Collectors.toList());
  }

  public static List<QueryRow> mapQueries(List<QueryInfo> infos) {
    return infos.stream()
        .map(q -> new QueryRow(q.getId(), q.getName()))
        .collect(Collectors.toList());
  }

  public static List<ColumnRow> mapColumns(TableInfo tableInfo, List<CProfile> selectedCols) {
    if (tableInfo == null || tableInfo.getCProfiles() == null) return List.of();

    return tableInfo.getCProfiles().stream()
        .filter(cProfile -> !cProfile.getCsType().isTimeStamp())
        .filter(Objects::nonNull)
        .map(cp -> {
          boolean isSelected = selectedCols.stream()
              .anyMatch(s -> s.getColId() == cp.getColId());
          return new ColumnRow(cp, isSelected);
        })
        .collect(Collectors.toList());
  }

  public static List<MetricRow> mapMetrics(List<Metric> availableMetrics, List<Metric> selectedMetrics) {
    if (availableMetrics == null) return List.of();

    return availableMetrics.stream()
        .filter(Objects::nonNull)
        .map(m -> {
          boolean isSelected = selectedMetrics.stream()
              .anyMatch(s -> s.getId() == m.getId());
          return new MetricRow(m, isSelected);
        })
        .collect(Collectors.toList());
  }

  public static List<MetadataRow> mapMetadata(TableInfo tableInfo) {
    if (tableInfo == null || tableInfo.getCProfiles() == null) return List.of();

    return tableInfo.getCProfiles().stream()
        .filter(f -> !f.getCsType().isTimeStamp())
        .map(cProfile -> {
          boolean isDimension = tableInfo.getDimensionColumnList() != null &&
              tableInfo.getDimensionColumnList().contains(cProfile.getColName());
          return new MetadataRow(cProfile, isDimension);
        })
        .collect(Collectors.toList());
  }

  public static List<TaskLinkRow> mapTaskLinks(List<TaskInfo> taskInfoList,
                                               Map<Integer, String> connectionNameMap) {
    return taskInfoList.stream()
        .map(task -> new TaskLinkRow(
            task.getId(),
            task.getName(),
            connectionNameMap.getOrDefault(task.getConnectionId(), "")
        ))
        .collect(Collectors.toList());
  }

  public static List<TimestampColumnRow> mapTimestampColumns(List<CProfile> timestampColumns) {
    if (timestampColumns == null) return List.of();

    return timestampColumns.stream()
        .filter(Objects::nonNull)
        .map(TimestampColumnRow::new)
        .collect(Collectors.toList());
  }
}