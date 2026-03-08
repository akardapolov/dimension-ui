package ru.dimension.ui.chart.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TestDataSet {
  private final String description;
  private final String tableName;
  @Builder.Default
  private final List<TestRecord> records = List.of();
  @Builder.Default
  private final List<TestRecord> initialRecords = List.of();
  @Builder.Default
  private final List<TestRecord> incrementalRecords = List.of();
}