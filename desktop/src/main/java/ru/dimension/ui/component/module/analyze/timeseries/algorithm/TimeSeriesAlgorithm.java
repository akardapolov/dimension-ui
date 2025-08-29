package ru.dimension.ui.component.module.analyze.timeseries.algorithm;

import java.util.LinkedHashMap;
import java.util.Map;
import ru.dimension.ui.model.data.CategoryTableXYDatasetRealTime;

public abstract class TimeSeriesAlgorithm<T> {

  private String name;
  private AlgorithmType type;
  private Map<String, String> parameters;

  protected Long begin;
  protected Long end;

  public TimeSeriesAlgorithm(String name,
                             AlgorithmType type,
                             Map<String, String> parameters) {
    this.name = name;
    this.type = type;
    this.parameters = parameters;
  }

  public abstract T analyze(String value,
                            Long begin,
                            Long end);

  public String getName() {
    return name;
  }

  public AlgorithmType getType() {
    return type;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  protected double[][] getData(String value,
                               CategoryTableXYDatasetRealTime inputDataset) {
    Map<Double, Double> map = new LinkedHashMap<>();

    for (int i = 0; i < inputDataset.getSeriesCount(); i++) {
      if (inputDataset.getSeriesKey(i).equals(value)) {
        for (int j = 0; j < inputDataset.getItemCount(); j++) {
          double x = inputDataset.getXValue(i, j);
          double y = inputDataset.getYValue(i, j);

          if (begin == null && end == null) {
            map.put(x, y);
          } else {
            if (x >= begin && x <= end) {
              map.put(x, y);
            }
          }
        }
      }
    }

    double[][] data = new double[2][map.size()];

    int index = 0;
    for (Map.Entry<Double, Double> entry : map.entrySet()) {
      data[0][index] = entry.getKey();
      data[1][index] = entry.getValue();
      index++;
    }

    return data;
  }
}
