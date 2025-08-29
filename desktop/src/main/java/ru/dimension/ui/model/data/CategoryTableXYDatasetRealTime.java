package ru.dimension.ui.model.data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jfree.data.xy.CategoryTableXYDataset;

@Log4j2
public class CategoryTableXYDatasetRealTime extends CategoryTableXYDataset {

  @Getter
  @Setter
  private HashMap<Integer, String> seriesNames = new HashMap<>();

  public CategoryTableXYDatasetRealTime() {}

  /**
   * Add series value to time series stacked chart
   *
   * @param x          time
   * @param y          value
   * @param seriesName Category name
   */
  public void addSeriesValue(double x,
                             double y,
                             String seriesName) {
    if (!seriesNames.containsValue(seriesName)) {
      Integer key = seriesNames.isEmpty() ? 1 : Collections.max(seriesNames.keySet()) + 1;
      seriesNames.put(key, seriesName);
    }

    add(x, y, seriesName, true);
  }

  public void saveSeriesValues(int series,
                               String seriesName) {
    seriesNames.put(series, seriesName);
  }

  public void deleteValuesFromDataset(int holdRange) {
    int imax = getItemCount();

    if (imax <= 0) {
      return;
    }

    List<Long> listToDelete = new ArrayList<>();

    Double xEndValue = (Double) getX(0, imax - 1);

    long begin = xEndValue.longValue() - ((long) holdRange * 60 * 1000);

    LocalDateTime dateBeginExpected = LocalDateTime.ofInstant(Instant.ofEpochMilli(begin),
                                                              TimeZone.getDefault().toZoneId());

    for (int i = 0; i < (imax - 1); i++) {
      try {
        Double xValue = (Double) getX(0, i);
        LocalDateTime dateBeginCurrent = LocalDateTime.ofInstant(Instant.ofEpochMilli(xValue.longValue()),
                                                                 TimeZone.getDefault().toZoneId());
        if (dateBeginCurrent.isBefore(dateBeginExpected)) {
          listToDelete.add(xValue.longValue());
        } else {
          break;
        }
      } catch (Exception e) {
        log.catching(e);
      }
    }

    listToDelete.forEach(xValue -> getSeriesNames().forEach((index, seriesName) -> remove(xValue, seriesName)));

    log.info("Deleted " + listToDelete.size() + " values from dataset");
  }
}
