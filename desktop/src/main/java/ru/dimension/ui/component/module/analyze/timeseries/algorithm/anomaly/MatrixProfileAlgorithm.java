package ru.dimension.ui.component.module.analyze.timeseries.algorithm.anomaly;

import com.github.eugene.kamenev.tsmp4j.algo.mp.stamp.STAMP;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.AlgorithmType;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.TimeSeriesAlgorithm;
import ru.dimension.ui.model.data.CategoryTableXYDatasetRealTime;

@Log4j2
public class MatrixProfileAlgorithm extends TimeSeriesAlgorithm<double[][]> {

  private CategoryTableXYDatasetRealTime inputDataset;

  public MatrixProfileAlgorithm(String name,
                                Map<String, String> parameters,
                                CategoryTableXYDatasetRealTime inputDataset) {
    super(name, AlgorithmType.ANOMALY, parameters);

    this.inputDataset = inputDataset;
  }

  @Override
  public double[][] analyze(String value,
                            Long begin,
                            Long end) {
    log.info("Run analyze by value: " + value);
    this.begin = begin;
    this.end = end;

    double[][] dataIn = getData(value, inputDataset);

    var limit = dataIn[1].length;
    var windowSize = Integer.parseInt(getParameters().get("Window"));
    var stamp = new STAMP(windowSize, limit);

    for (double datum : dataIn[1]) {
      stamp.update(datum);
    }

    var mp = stamp.get();

    double[] mpArr = mp.profile();
    double[][] dataOut = new double[2][mpArr.length];

    int ts = dataIn[0].length - 1;
    for (int i = mpArr.length - 1; i >= 0; i--) {
      dataOut[0][i] = dataIn[0][ts];

      if (!Double.isInfinite(mpArr[i])) {
        dataOut[1][i] = mpArr[i];
      }

      ts--;
    }

    return dataOut;
  }
}
