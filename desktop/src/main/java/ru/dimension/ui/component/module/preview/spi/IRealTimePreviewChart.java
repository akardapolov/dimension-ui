package ru.dimension.ui.component.module.preview.spi;

import ru.dimension.ui.model.view.RangeRealTime;

public interface IRealTimePreviewChart extends IPreviewChart {
  void handleRealTimeRangeUI(RangeRealTime range);
  void handleRealTimeRange(RangeRealTime range);
  boolean isReadyRealTimeUpdate();
}