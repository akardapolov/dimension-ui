package ru.dimension.ui.component.module.analyze.timeseries.algorithm.forecast;

import com.workday.insights.timeseries.arima.Arima;
import com.workday.insights.timeseries.arima.struct.ArimaParams;
import com.workday.insights.timeseries.arima.struct.ForecastResult;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.AlgorithmType;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.TimeSeriesAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.model.ForecastData;

@Log4j2
public class ARIMAlgorithm extends TimeSeriesAlgorithm<ForecastData> {

  private CategoryTableXYDatasetRealTime inputDataset;

  public ARIMAlgorithm(String name,
                      Map<String, String> parameters,
                      CategoryTableXYDatasetRealTime inputDataset) {
    super(name, AlgorithmType.FORECAST, parameters);
    this.inputDataset = inputDataset;
  }

  @Override
  public ForecastData analyze(String value,
                              Long begin,
                              Long end) {
    log.info("Run analyze by value: " + value);
    this.begin = begin;
    this.end = end;

    ForecastData forecastData = new ForecastData();
    double[][] data = getData(value, inputDataset);
    forecastData.setData(data);
    forecastData.setForecast(createForecastData(data));
    return forecastData;
  }

  private double[][] createForecastData(double[][] data) {
    double step = data[0][1] - data[0][0];
    double xLast = data[0][data[1].length - 1];

    String linearTrend = getParameters().get("Linear trend");
    double[] x;

    if (linearTrend.equalsIgnoreCase("true")) {
      x = data[1];
    } else {
      //Simple differencing.  A more robust solution might be needed for real-world applications.
      x = difference(data[1], 1);
    }

    int p = Integer.parseInt(getParameters().get("Order of AR"));
    int q = Integer.parseInt(getParameters().get("Order of MA"));
    int d = 0; // Assuming no differencing if "Linear trend" is handled separately. Adjust as needed.
    int P = 0; //Seasonal parameters are not provided in the original code, default to 0
    int D = 0;
    int Q = 0;
    int m = 1; //Seasonal period, default to 1 (no seasonality)

    int steps = Integer.parseInt(getParameters().get("Steps"));
    try {
      ForecastResult forecastResult = Arima.forecast_arima(x, steps, new ArimaParams(p, d, q, P, D, Q, m));
      double[] forecast = forecastResult.getForecast();
      double[][] forecastData = new double[2][steps];

      forecastData[0][0] = xLast;
      forecastData[1][0] = forecast[0];

      for (int i = 1; i < forecast.length; i++) {
        forecastData[0][i] = forecastData[0][i - 1] + step;
        forecastData[1][i] = forecast[i];
      }
      return forecastData;
    } catch (Exception e) {
      log.error("Error during ARIMA forecasting: ", e);
      // Handle the exception appropriately, e.g., return an empty array or throw a custom exception.
      return new double[2][0]; // Return an empty array indicating failure.
    }
  }

  //Simple differencing function.  Replace with a more robust solution if needed.
  private double[] difference(double[] data, int lag) {
    if (data.length <= lag) return new double[0];
    double[] diffData = new double[data.length - lag];
    for (int i = lag; i < data.length; i++) {
      diffData[i - lag] = data[i] - data[i - lag];
    }
    return diffData;
  }
}
