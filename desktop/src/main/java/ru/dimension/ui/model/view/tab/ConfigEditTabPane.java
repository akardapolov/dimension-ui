package ru.dimension.ui.model.view.tab;

public enum ConfigEditTabPane {
  PROFILE("Profile"),
  TASK("Task"),
  CONNECTION("Connection"),
  QUERY("Query");

  private final String name;

  ConfigEditTabPane(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

}
