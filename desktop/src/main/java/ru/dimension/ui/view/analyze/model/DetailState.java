package ru.dimension.ui.view.analyze.model;

public enum DetailState {
  SHOW("Show"),
  HIDE("Hide");

  private final String name;

  DetailState(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public DetailState toggle() {
    return this == SHOW ? HIDE : SHOW;
  }
}