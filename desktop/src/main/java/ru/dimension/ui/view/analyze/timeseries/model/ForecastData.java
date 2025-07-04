package ru.dimension.ui.view.analyze.timeseries.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ForecastData {

  private double[][] data;
  private double[][] forecast;
}
