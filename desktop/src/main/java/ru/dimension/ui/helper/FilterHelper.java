package ru.dimension.ui.helper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.filter.CompositeFilter;
import ru.dimension.db.model.filter.FilterCondition;
import ru.dimension.db.model.filter.LogicalOperator;
import ru.dimension.db.model.profile.CProfile;

public final class FilterHelper {
  private FilterHelper() {}

  public static CompositeFilter toCompositeFilter(
      Map<CProfile, LinkedHashSet<String>> topMapSelected) {

    if (topMapSelected == null || topMapSelected.isEmpty()) {
      return null;
    }

    List<FilterCondition> conditions = topMapSelected.entrySet()
        .stream()
        .filter(f -> !f.getValue().isEmpty())
        .map(e -> new FilterCondition(
            e.getKey(),
            e.getValue().toArray(String[]::new),
            CompareFunction.EQUAL))
        .toList();

    return new CompositeFilter(conditions, LogicalOperator.AND);
  }
}
