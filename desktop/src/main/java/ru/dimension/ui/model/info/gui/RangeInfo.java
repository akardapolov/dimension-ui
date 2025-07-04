package ru.dimension.ui.model.info.gui;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RangeInfo {

  private long createdAt;
  private long begin;
  private long end;
  private String sourceTab;
}
