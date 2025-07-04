package ru.dimension.ui.model.view;

public enum ProcessType {
  REAL_TIME("Real-time"),
  REAL_TIME_ANALYZE("Analyze"),
  HISTORY("History"),
  HISTORY_ANALYZE("Analyze"),
  ADHOC("Ad-hoc"),
  ADHOC_ANALYZE("Ad-hoc analyze"),
  SEARCH("Search");

  private final String name;

  ProcessType(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
