package ru.dimension.ui.model.data;

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
