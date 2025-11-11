package ru.dimension.ui.component.module.analyze.timeseries;

import java.awt.Color;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.module.analyze.AnalyzeTimeSeriesPanel;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.AlgorithmType;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.TimeSeriesAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.forecast.ARIMAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.model.ForecastData;
import ru.dimension.ui.model.data.CategoryTableXYDatasetRealTime;

@Log4j2
public class AnalyzeForecastPanel extends AnalyzeTimeSeriesPanel {

  public AnalyzeForecastPanel(Map<String, Color> seriesColorMap,
                              CategoryTableXYDatasetRealTime chartDataset) {
    super(seriesColorMap, chartDataset);
  }

  @Override
  protected void processAlgorithm(TimeSeriesAlgorithm algorithm,
                                  String value) {
    if (value == null) {
      displayChart(null, null);
      JOptionPane.showMessageDialog(this, "Not selected value in table",
                                    "Warning", JOptionPane.WARNING_MESSAGE);
      return;
    }

    if (algorithm instanceof ARIMAlgorithm forecastingAlgorithm) {
      ForecastData result = forecastingAlgorithm.analyze(value, begin, end);
      CategoryTableXYDatasetRealTime chartDataset = createDataset(result, value);

      JPanel chartPanel = createChartPanel(algorithm, value, chartDataset, true);
      displayChart(value, chartPanel);
    } else {
      displayChart(null, null);
    }
  }

  private CategoryTableXYDatasetRealTime createDataset(ForecastData result, String value) {
    CategoryTableXYDatasetRealTime chartDataset = new CategoryTableXYDatasetRealTime();
    double[][] data = result.getData();
    double[][] forecast = result.getForecast();

    for (int i = 0; i < data[0].length; i++) {
      chartDataset.addSeriesValue(data[0][i], data[1][i], value);
    }

    for (int i = 0; i < forecast[0].length; i++) {
      chartDataset.addSeriesValue(forecast[0][i], forecast[1][i], "Forecast");
    }
    return chartDataset;
  }

  @Override
  protected AlgorithmType getAlgorithmType() {
    return AlgorithmType.FORECAST;
  }
}