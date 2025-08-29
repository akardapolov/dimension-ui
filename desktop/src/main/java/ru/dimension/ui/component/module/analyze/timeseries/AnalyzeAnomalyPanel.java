package ru.dimension.ui.component.module.analyze.timeseries;

import java.awt.Color;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.module.analyze.AnalyzeTimeSeriesPanel;
import ru.dimension.ui.model.data.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.AlgorithmType;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.TimeSeriesAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.anomaly.MatrixProfileAlgorithm;

@Log4j2
public class AnalyzeAnomalyPanel extends AnalyzeTimeSeriesPanel {

  public AnalyzeAnomalyPanel(JXTableCase tableSeries,
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

    if (algorithm instanceof MatrixProfileAlgorithm alg) {
      double[][] result = alg.analyze(value, begin, end);

      CategoryTableXYDatasetRealTime chartDataset = new CategoryTableXYDatasetRealTime();
      for (int i = 0; i < result[0].length; i++) {
        chartDataset.addSeriesValue(result[0][i], result[1][i], value);
      }

      jspSettingsChart.add(createChartPanel(algorithm, value, chartDataset, false), JSplitPane.BOTTOM);
    }
  }


  @Override
  protected AlgorithmType getAlgorithmType() {
    return AlgorithmType.ANOMALY;
  }
}
