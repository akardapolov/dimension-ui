package ru.dimension.ui.model.chart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RangeBatchSize {

  private double range;
  private int batchSize;
}
