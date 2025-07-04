package ru.dimension.ui.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;

@Log4j2
public class DatasetRemoveTest {

  @Test
  public void removeDatasetTest() {
    CategoryTableXYDatasetRealTime dataset = new CategoryTableXYDatasetRealTime();
    dataset.addSeriesValue(0, 1, "Series Name 1");
    dataset.addSeriesValue(1, 2, "Series Name 1");
    dataset.addSeriesValue(2, 3, "Series Name 1");

    dataset.addSeriesValue(0, 1, "Series Name 2");
    dataset.addSeriesValue(1, 2, "Series Name 2");
    dataset.addSeriesValue(2, 3, "Series Name 2");

    List<Long> listToDelete = new ArrayList<>();

    Double xEndValue = (Double) dataset.getX(0, dataset.getItemCount() - 1);

    long begin = xEndValue.longValue() - 1;

    LocalDateTime dateBeginExpected = LocalDateTime.ofInstant(Instant.ofEpochMilli(begin),
                                                              TimeZone.getDefault().toZoneId());

    int itemCountBefore = dataset.getItemCount();

    for (int i = 0; i < (dataset.getItemCount() - 1); i++) {
      try {
        Double xValue = (Double) dataset.getX(0, i);
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

    listToDelete.forEach(xValue -> {
      dataset.getSeriesNames().forEach((seriesIndex, seriesName) -> dataset.remove(xValue, seriesName));
    });

    int itemCountAfter = dataset.getItemCount();

    assertEquals(3, itemCountBefore);
    assertEquals(2, itemCountAfter);
  }
}
