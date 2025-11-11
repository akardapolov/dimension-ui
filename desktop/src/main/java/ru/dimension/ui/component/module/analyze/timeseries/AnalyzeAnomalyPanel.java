package ru.dimension.ui.component.module.analyze.timeseries;

import java.awt.Color;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.module.analyze.AnalyzeTimeSeriesPanel;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.AlgorithmType;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.TimeSeriesAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.anomaly.MatrixProfileAlgorithm;
import ru.dimension.ui.model.data.CategoryTableXYDatasetRealTime;

@Log4j2
public class AnalyzeAnomalyPanel extends AnalyzeTimeSeriesPanel {

  public AnalyzeAnomalyPanel(Map<String, Color> seriesColorMap,
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

    if (algorithm instanceof MatrixProfileAlgorithm alg) {
      double[][] result = alg.analyze(value, begin, end);

      CategoryTableXYDatasetRealTime chartDataset = new CategoryTableXYDatasetRealTime();
      for (int i = 0; i < result[0].length; i++) {
        chartDataset.addSeriesValue(result[0][i], result[1][i], value);
      }

      JPanel chartPanel = createChartPanel(algorithm, value, chartDataset, false);
      displayChart(value, chartPanel);
    } else {
      displayChart(null, null);
    }
  }

  @Override
  protected AlgorithmType getAlgorithmType() {
    return AlgorithmType.ANOMALY;
  }
}