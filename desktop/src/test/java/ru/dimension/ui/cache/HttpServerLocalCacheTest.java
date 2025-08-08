package ru.dimension.ui.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.component.chart.RangeUtils;
import ru.dimension.ui.component.chart.RangeUtils.TimeRange;
import ru.dimension.ui.HandlerMock;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.gui.RangeInfo;


@Log4j2
public class HttpServerLocalCacheTest extends HandlerMock {

  @Test
  public void put_and_get_test() {
    assertNotNull(appCacheLazy.get());

    int profileId = 1;
    int taskId = 1;
    int queryId = 1;
    RangeInfo rangeInfo1 = new RangeInfo(1, 1, 2, "A");
    RangeInfo rangeInfo2 = new RangeInfo(2, 3, 4, "B");

    ProfileTaskQueryKey profileTaskQueryKey =
        new ProfileTaskQueryKey(profileId, taskId, queryId);

    appCacheLazy.get().putRangeInfo(profileTaskQueryKey, rangeInfo1);
    appCacheLazy.get().putRangeInfo(profileTaskQueryKey, rangeInfo2);

    assertEquals(2,
                 appCacheLazy.get().getRangeInfo(profileTaskQueryKey).size());
  }

  @Test
  public void timeTest() {
    long currentTime = System.currentTimeMillis();
    List<TimeRange> ranges = RangeUtils.calculateRanges(currentTime, 5); // 5-минутный диапазон

    int i = 0;
    for (TimeRange range : ranges) {
      System.out.println("Range: " +
                             RangeUtils.toLocalDateTime(range.getStart()) + " - " +
                             RangeUtils.toLocalDateTime(range.getEnd()));
      i++;
    }

    System.out.println("Count: " + i);
  }

}
