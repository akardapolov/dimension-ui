package ru.dimension.ui.model.view;

public enum RangeRealTime {
  FIVE_MIN("5M"),
  TEN_MIN("10M"),
  THIRTY_MIN("30M"),
  SIXTY_MIN("60M");

  private final String name;

  RangeRealTime(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public int getMinutes() {
    return switch (this) {
      case FIVE_MIN -> 5;
      case TEN_MIN -> 10;
      case THIRTY_MIN -> 30;
      case SIXTY_MIN -> 60;
    };
  }
}
