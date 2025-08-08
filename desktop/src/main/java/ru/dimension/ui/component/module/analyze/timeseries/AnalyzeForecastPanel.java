package ru.dimension.ui.component.module.analyze.timeseries;

import java.awt.Color;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.module.analyze.AnalyzeTimeSeriesPanel;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.forecast.ARIMAlgorithm;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.AlgorithmType;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.TimeSeriesAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.model.ForecastData;

@Log4j2
public class AnalyzeForecastPanel extends AnalyzeTimeSeriesPanel {

  public AnalyzeForecastPanel(JXTableCase tableSeries,
                              Map<String, Color> seriesColorMap,
                              CategoryTableXYDatasetRealTime chartDataset) {
    super(tableSeries, seriesColorMap, chartDataset);
  }

  @Override
  protected void processAlgorithm(TimeSeriesAlgorithm algorithm,
                                  String value) {

    if (value == null) {
      JOptionPane.showMessageDialog(this, "Not selected value in table",
                                    "Warning", JOptionPane.WARNING_MESSAGE);
      return;
    }

    if (algorithm instanceof ARIMAlgorithm forecastingAlgorithm) {
      ForecastData result = forecastingAlgorithm.analyze(value, begin, end);
      CategoryTableXYDatasetRealTime chartDataset = createDataset(result, value);
      jspSettingsChart.add(createChartPanel(algorithm, value, chartDataset, true), JSplitPane.BOTTOM);
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
