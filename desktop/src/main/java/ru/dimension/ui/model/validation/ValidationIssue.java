package ru.dimension.ui.model.validation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ValidationIssue {

  private final ValidationSection section;
  private final String fieldKey;
  private final ValidationSeverity severity;
  private final String message;
  private final String suggestedValue;
}