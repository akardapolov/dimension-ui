package ru.dimension.ui.component.module.preview.zoom;

public enum RenderMode {
  CURRENT("Current"),
  HYBRID("Hybrid");

  private final String displayName;

  RenderMode(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}