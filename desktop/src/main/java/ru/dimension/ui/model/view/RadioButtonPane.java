package ru.dimension.ui.model.view;

public enum RadioButtonPane {
  ONE_SEC("1 sec"),
  THREE_SEC("3 sec"),
  FIVE_SEC("5 sec"),
  TEN_SEC("10 sec"),
  THIRTY_SEC("30 sec");

  private final String name;

  RadioButtonPane(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
