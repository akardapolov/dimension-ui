package ru.dimension.ui.model.view.tab;

public enum MainTabPane {
  WORKSPACE("Workspace"),
  DASHBOARD("Dashboard"),
  REPORT("Report"),
  ADHOC("Ad-Hoc");

  private final String name;

  MainTabPane(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
