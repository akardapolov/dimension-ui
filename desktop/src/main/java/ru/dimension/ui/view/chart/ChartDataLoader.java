package ru.dimension.ui.view.chart;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.output.BlockKeyTail;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.chart.RangeBatchSize;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.gui.ChartInfo;

@Log4j2
public class ChartDataLoader implements HelperChart {

  protected final Metric metric;
  protected final ChartInfo chartInfo;
  private final StackedChart stackedChart;
  private final FunctionDataHandler dataHandler;
  private boolean isRealTime;

  private double range;
  private int batchSize;

  @Getter
  private final Set<String> series;

  private final Deque<BlockKeyTail> blockKeyTailsDeque = new ArrayDeque<>();

  private List<BlockKeyTail> prevBatch = new ArrayList<>();

  @Getter
  @Setter
  private long clientBegin;

  @Setter
  private Map.Entry<CProfile, List<String>> filter;

  public ChartDataLoader(Metric metric,
                         ChartInfo chartInfo,
                         StackedChart stackedChart,
                         FunctionDataHandler dataHandler,
                         boolean isRealTime) {
    this(metric, chartInfo, stackedChart, dataHandler, isRealTime, null);
  }

  public ChartDataLoader(Metric metric,
                         ChartInfo chartInfo,
                         StackedChart stackedChart,
                         FunctionDataHandler dataHandler,
                         boolean isRealTime,
                         Map.Entry<CProfile, List<String>> filter) {
    this.metric = metric;
    this.chartInfo = chartInfo;
    this.stackedChart = stackedChart;
    this.dataHandler = dataHandler;
    this.isRealTime = isRealTime;
    this.filter = filter;

    this.series = new LinkedHashSet<>();

    RangeBatchSize rangeBatchSize = getRangeBatchSize(chartInfo, isRealTime);
    this.batchSize = rangeBatchSize.getBatchSize();
    this.range = rangeBatchSize.getRange();
  }

  public void setSeries(Collection<String> seriesCollection) {
    this.series.clear();
    this.series.addAll(seriesCollection);
  }

  public void loadDataFromBdbToDeque(List<BlockKeyTail> blockKeyTails) {
    blockKeyTailsDeque.addAll(blockKeyTails);
  }

  public void loadDataFromDequeToChart(long beginRange,
                                       long endRange)
      throws BeginEndWrongOrderException, SqlColMetadataException {

    double k = getK(range, chartInfo);

    if (beginRange > endRange) {
      return;
    }

    // Fill if empty blockKeyTailsDeque
    if (blockKeyTailsDeque.isEmpty()) {
      if ((endRange - beginRange) > (range * GAP)) {
        fillEmptyChartLocal(beginRange, endRange);
      }
      return;
    }

    // Check first element
    BlockKeyTail blockKeyTailFirst = blockKeyTailsDeque.getFirst();
    long firstDelta = blockKeyTailFirst.getKey() - beginRange;

    BlockKeyTail blockKeyTailLast = blockKeyTailsDeque.getLast();
    long lastDelta = endRange - blockKeyTailLast.getKey();

    dataHandler.fillSeriesData(beginRange, endRange, series);

    // Fill first gap up to first and last elements
    if (firstDelta > (range * GAP)) {
      fillEmptyChartLocal(beginRange, blockKeyTailFirst.getKey());
    }

    if (lastDelta > (range * GAP)) {
      fillEmptyChartLocal(blockKeyTailLast.getTail(), endRange);
    }

    // Iterate over deque by batch size and clear using poll primitive
    List<BlockKeyTail> nextBatch;

    int totalElements = blockKeyTailsDeque.size();
    for (int start = 0; start < totalElements; start += batchSize) {
      List<BlockKeyTail> currBatch = new ArrayList<>();

      // Get current batch
      for (int i = 0; i < batchSize && !blockKeyTailsDeque.isEmpty(); i++) {
        currBatch.add(blockKeyTailsDeque.pollFirst());
      }

      // Save the nextBatch
      nextBatch = new ArrayList<>(getNextBatch(totalElements, start));

      // Process the current batch
      if (currBatch.size() == batchSize) {
        if (!prevBatch.isEmpty()) {
          long prevCurrDelta = currBatch.getFirst().getKey() - prevBatch.getLast().getTail();
          if (prevCurrDelta > (range * GAP)) {
            fillEmptyChartLocal(prevBatch.getLast().getTail(), currBatch.getFirst().getKey());
          }
        } else {
          long beginRangeCurrDelta = currBatch.getFirst().getKey() - beginRange;
          if (beginRangeCurrDelta > (range * GAP)) {
            fillEmptyChartLocal(beginRange, currBatch.getFirst().getKey());
          }
        }

        // Handle gaps between BlockKeyTail items within currBatch
        for (int i = 1; i < currBatch.size(); i++) {
          long currDelta = currBatch.get(i).getKey() - currBatch.get(i-1).getTail();
          if (currDelta > (range * GAP)) {
            // Fill gap between current item and previous item
            fillEmptyChartLocal(currBatch.get(i-1).getTail(), currBatch.get(i).getKey());

            // Process the previous item
            if (filter != null) {
              dataHandler.handleFunction(currBatch.get(i-1).getKey(),
                                         currBatch.get(i-1).getTail(),
                                         k, series,
                                         filter.getKey(), filter.getValue().toArray(new String[0]),
                                         CompareFunction.EQUAL,
                                         stackedChart);
            } else {
              dataHandler.handleFunction(currBatch.get(i-1).getKey(),
                                         currBatch.get(i-1).getTail(),
                                         true, currBatch.get(i-1).getTail(), k, series, stackedChart);
            }

            // Process the current item
            if (filter != null) {
              dataHandler.handleFunction(currBatch.get(i).getKey(),
                                         currBatch.get(i).getTail(),
                                         k, series,
                                         filter.getKey(), filter.getValue().toArray(new String[0]),
                                         CompareFunction.EQUAL,
                                         stackedChart);
            } else {
              dataHandler.handleFunction(currBatch.get(i).getKey(),
                                         currBatch.get(i).getTail(),
                                         true, currBatch.get(i).getKey(), k, series, stackedChart);
            }
          }
        }

        long overallDelta = currBatch.getLast().getTail() - currBatch.getFirst().getKey();
        if (overallDelta > (range * GAP)) {
          fillEmptyChartLocal(currBatch.getFirst().getKey(), currBatch.getLast().getKey());

          if (filter != null) {
            dataHandler.handleFunction(currBatch.getFirst().getKey(),
                                       currBatch.getFirst().getTail(),
                                       k, series,
                                       filter.getKey(), filter.getValue().toArray(new String[0]),
                                       CompareFunction.EQUAL,
                                       stackedChart);
            dataHandler.handleFunction(currBatch.getLast().getKey(),
                                       currBatch.getLast().getTail(),
                                       k, series,
                                       filter.getKey(), filter.getValue().toArray(new String[0]),
                                       CompareFunction.EQUAL,
                                       stackedChart);
          } else {
            dataHandler.handleFunction(currBatch.getFirst().getKey(),
                                       currBatch.getFirst().getTail(),
                                       true, currBatch.getFirst().getTail(), k, series, stackedChart);
            dataHandler.handleFunction(currBatch.getLast().getKey(),
                                       currBatch.getLast().getTail(),
                                       true, currBatch.getLast().getKey(), k, series, stackedChart);
          }
        } else {
          if (filter != null) {
            dataHandler.handleFunction(currBatch.getFirst().getKey(),
                                       currBatch.getLast().getTail(),
                                       k, series,
                                       filter.getKey(), filter.getValue().toArray(new String[0]),
                                       CompareFunction.EQUAL,
                                       stackedChart);
          } else {
            dataHandler.handleFunction(currBatch.getFirst().getKey(),
                                       currBatch.getLast().getTail(),
                                       true, currBatch.getLast().getTail(), k, series, stackedChart);
          }
        }
      } else { // currBatch.size() != batchSize
        if (nextBatch.isEmpty() & currBatch.size() == 0) {
          fillEmptyChartLocal(prevBatch.isEmpty() ? beginRange : prevBatch.getLast().getTail(), endRange);
        }

        if (currBatch.size() != 0) {
          if (!prevBatch.isEmpty() & nextBatch.isEmpty()) {
            log.warn("Wait the next cycle when data will be loading");
            long prevCurrDelta = prevBatch.getLast().getTail() - currBatch.getFirst().getKey();
            if (prevCurrDelta > (range * GAP)) {
              fillEmptyChartLocal(prevBatch.getLast().getTail(), currBatch.getFirst().getKey());
            }
          }
          if (prevBatch.isEmpty() & nextBatch.isEmpty()) {
            log.warn("Wait the next cycle when data will be loading");
            long endRangeCurrDelta = endRange - currBatch.getLast().getTail();
            if (endRangeCurrDelta > (range * GAP)) {
              fillEmptyChartLocal(currBatch.getLast().getTail(), endRange);
            }
          }

          if (!prevBatch.isEmpty() & prevBatch.size() < batchSize) {

            int total = prevBatch.size() + currBatch.size();

            Deque<BlockKeyTail> deque = new ArrayDeque<>();
            deque.addAll(prevBatch);
            deque.addAll(currBatch);

            for (int startL = 0; startL < total; startL += batchSize) {
              List<BlockKeyTail> currentBatch = new ArrayList<>();

              // Get current batch
              for (int i = 0; i < batchSize && !deque.isEmpty(); i++) {
                currentBatch.add(deque.pollFirst());
              }

              if (currentBatch.size() == batchSize) {
                if (filter != null) {
                  dataHandler.handleFunction(currentBatch.getFirst().getKey(),
                                             currentBatch.getLast().getTail(),
                                             k, series,
                                             filter.getKey(), filter.getValue().toArray(new String[0]),
                                             CompareFunction.EQUAL,
                                             stackedChart);
                } else {
                  dataHandler.handleFunction(currentBatch.getFirst().getKey(),
                                             currentBatch.getLast().getTail(),
                                             true, currentBatch.getLast().getTail(), k, series, stackedChart);
                }
              }
              currBatch.clear();
              currBatch.addAll(currentBatch);
            }
          }

          // Set clientBegin to the next range
          for (BlockKeyTail blockKeyTail : currBatch) {
            clientBegin = blockKeyTail.getTail() + 1;
          }

        }
      }

      prevBatch = new ArrayList<>(currBatch);
    }

    if (isRealTime) {
      stackedChart.deleteAllSeriesData(chartInfo.getRangeRealtime().getMinutes());
    }
  }

  private List<BlockKeyTail> getNextBatch(int totalElements,
                                          int start) {
    List<BlockKeyTail> nextBatch = new ArrayList<>();
    if (start + batchSize < totalElements) {
      nextBatch = new ArrayList<>();
      int peekLimit = 0;
      for (BlockKeyTail blockKeyTail : blockKeyTailsDeque) {
        if (peekLimit < batchSize) {
          nextBatch.add(blockKeyTail);
          peekLimit++;
        } else {
          break;
        }
      }
    }

    return nextBatch;
  }

  private void fillEmptyChartLocal(long beginRange,
                                   long endRange) {
    for (long dtBegin = beginRange + 1; dtBegin <= endRange; dtBegin += Math.round(range)) {
      long dtEnd = dtBegin + Math.round(range) - 1;
      fillEmptyChart(dtBegin, dtEnd);
    }
  }

  protected void fillEmptyChart(long beginRange,
                                long endRange) {
    if (ChartType.LINEAR.equals(metric.getChartType())) {
      if (beginRange > endRange) {
        try {
          stackedChart.addSeriesValue(beginRange, 0.0, metric.getYAxis().getColName());
        } catch (Exception exception) {
          log.info(exception);
        }
      }

      for (long x = beginRange; x <= endRange; x += Math.round(range)) {
        try {
          stackedChart.addSeriesValue(x, 0.0, metric.getYAxis().getColName());
        } catch (Exception exception) {
          log.info(exception);
        }
      }
    } else {
      if (beginRange > endRange) {
        series.forEach(seriesName -> {
          try {
            stackedChart.addSeriesValue(beginRange, 0.0, seriesName);
          } catch (Exception exception) {
            log.info(exception);
          }
        });
      }

      for (long x = beginRange; x <= endRange; x += Math.round(range)) {
        long finalX = x;
        series.forEach(seriesName -> {
          try {
            stackedChart.addSeriesValue(finalX, 0.0, seriesName);
          } catch (Exception exception) {
            log.info(exception);
          }
        });
      }
    }
  }
}