package ru.dimension.ui.model.validation;

public enum ValidationSeverity {
  OK, WARNING, ERROR;

  public boolean worseThan(ValidationSeverity other) {
    return this.ordinal() > other.ordinal();
  }
}