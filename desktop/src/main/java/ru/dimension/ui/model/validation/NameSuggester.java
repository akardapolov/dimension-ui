package ru.dimension.ui.model.validation;

import java.util.Set;

public final class NameSuggester {

  private NameSuggester() {
  }

  public static String suggestUnique(String base, Set<String> existingNamesLowerCase) {
    String trimmedBase = base == null ? "" : base.trim();
    if (trimmedBase.isEmpty()) {
      trimmedBase = "template";
    }
    if (!existingNamesLowerCase.contains(trimmedBase.toLowerCase())) {
      return trimmedBase;
    }
    int i = 2;
    while (existingNamesLowerCase.contains((trimmedBase + "-" + i).toLowerCase())) {
      i++;
    }
    return trimmedBase + "-" + i;
  }
}