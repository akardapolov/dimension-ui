package ru.dimension.ui.model.view.handler;

public enum LifeCycleStatus {
  NONE("None"),
  NEW("New"),
  COPY("Copy"),
  EDIT("Edit");

  private final String description;

  LifeCycleStatus(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }

}
