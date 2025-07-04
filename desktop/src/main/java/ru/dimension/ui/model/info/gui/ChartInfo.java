package ru.dimension.ui.model.info.gui;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;

@Data
@Accessors(chain = true)
@ToString
public class ChartInfo {

  private int id;
  private RangeRealTime rangeRealtime;
  private RangeHistory rangeHistory;
  private int pullTimeoutClient;
  private int pullTimeoutServer = 1;

  private long customBegin;
  private long customEnd;

  public ChartInfo copy() {
    return new ChartInfo()
        .setId(this.id)
        .setRangeRealtime(this.rangeRealtime)
        .setRangeHistory(this.rangeHistory)
        .setPullTimeoutClient(this.pullTimeoutClient)
        .setPullTimeoutServer(this.pullTimeoutServer)
        .setCustomBegin(this.customBegin)
        .setCustomEnd(this.customEnd);
  }
}
