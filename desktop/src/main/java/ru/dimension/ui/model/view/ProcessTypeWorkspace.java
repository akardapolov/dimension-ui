package ru.dimension.ui.model.view;

public enum ProcessTypeWorkspace {

  VISUALIZE("Visualize"),
  ANALYZE("Analyze"),
  SEARCH("Search");

  private final String name;

  ProcessTypeWorkspace(String name) {
    this.name = name;
  }

  public String getName() {
    String nameVert =
        "<html> <p style=\"writing-mode: tb-rl; text-orientation: upright;\">" + this.name + "</p></html>";

    return nameVert;
  }
}
