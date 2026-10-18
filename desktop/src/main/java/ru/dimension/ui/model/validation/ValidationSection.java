package ru.dimension.ui.model.validation;

public enum ValidationSection {
  PROFILE("Profile"),
  TASK("Task"),
  CONNECTION("Connection"),
  QUERIES("Queries");

  private final String title;

  ValidationSection(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }
}