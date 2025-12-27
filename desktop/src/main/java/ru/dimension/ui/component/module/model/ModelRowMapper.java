package ru.dimension.ui.component.module.model;

import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.module.model.row.Rows.*;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Responsible for converting Domain objects into UI Row objects.
 */
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
}