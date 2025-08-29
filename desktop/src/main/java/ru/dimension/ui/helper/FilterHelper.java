package ru.dimension.ui.helper;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.filter.CompositeFilter;
import ru.dimension.db.model.filter.FilterCondition;
import ru.dimension.db.model.filter.LogicalOperator;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.model.config.Metric;

@Log4j2
public final class FilterHelper {
  private FilterHelper() {}

  public static CompositeFilter toCompositeFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected) {

    if (topMapSelected == null || topMapSelected.isEmpty()) {
      return null;
    }

    List<FilterCondition> conditions = topMapSelected.entrySet()
        .stream()
        .filter(filter -> !filter.getValue().isEmpty())
        .filter(entry -> hasValidValues(entry.getKey(), entry.getValue()))
        .map(entry -> {

          // TODO Fast fix for wrong set filter value as metric.getYAxis().getColName()
          // TODO Bug somewhere in ServerRealtimeSCP.java, ClientRealtimeSCP.java, HistorySCP.java
          logBugForInvalidValues(entry.getKey(), entry.getValue());

          return new FilterCondition(
              entry.getKey(),
              entry.getValue()
                  .stream()
                  .filter(filter -> !filter.equalsIgnoreCase(entry.getKey().getColName()))
                  .toArray(String[]::new),
              CompareFunction.EQUAL);
        })
        .toList();

    return new CompositeFilter(conditions, LogicalOperator.AND);
  }

  private static void logBugForInvalidValues(CProfile profile, LinkedHashSet<String> values) {
    List<String> invalidValues = values.stream()
        .filter(value -> value.equalsIgnoreCase(profile.getColName()))
        .collect(Collectors.toList());

    if (!invalidValues.isEmpty()) {
      log.warn("BUG_DETECTED: Invalid filter values found that match column name. " +
                   "Profile: {}, Invalid Values: {}, All Values: {}", profile, invalidValues, values);
    }
  }

  private static boolean hasValidValues(CProfile key, LinkedHashSet<String> values) {
    return values.stream().anyMatch(value -> !value.equalsIgnoreCase(key.getColName()));
  }

  // TODO Fast fix for wrong set filter value as metric.getYAxis().getColName()
  // TODO Bug somewhere in ServerRealtimeSCP.java, ClientRealtimeSCP.java, HistorySCP.java
  public static Map<CProfile, LinkedHashSet<String>> sanitizeTopMapSelected(
      Map<CProfile, LinkedHashSet<String>> topMapSelected,
      Metric metric) {

    if (topMapSelected == null || topMapSelected.isEmpty()) {
      return topMapSelected;
    }

    Map<CProfile, LinkedHashSet<String>> sanitized = new HashMap<>();

    topMapSelected.forEach((profile, values) -> {
      LinkedHashSet<String> validValues = values.stream()
          .filter(v -> !v.equals(metric.getYAxis().getColName()))
          .collect(Collectors.toCollection(LinkedHashSet::new));
      sanitized.put(profile, validValues);
    });

    return sanitized;
  }
}
