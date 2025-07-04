package ru.dimension.ui.model.gantt;

import lombok.Data;

@Data
public class DrawingScale {

  private long percentPrev = 0;
  private int scaleToggle = 0;
  private double scale = 0.8;
}
