package ru.dimension.ui.component.module.preview.spi;

import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.view.RangeHistory;

public interface IHistoryPreviewChart extends IPreviewChart {
  void handleHistoryRangeUI(RangeHistory range);
  void handleHistoryRange(RangeHistory range);
  void handleHistoryCustomRange(ChartRange range);
}