package ru.dimension.ui.chart.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TestRecord {
  private final long dt;
  private final double valueRaw;
  private final double valueEnum;
  private final double valueHistogram;
}