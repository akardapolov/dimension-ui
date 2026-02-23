package ru.dimension.ui.component.module.preview.zoom;

public enum ViewMode {
  PLAIN("Plain"),
  TILES("Tiles");

  private final String displayName;

  ViewMode(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}