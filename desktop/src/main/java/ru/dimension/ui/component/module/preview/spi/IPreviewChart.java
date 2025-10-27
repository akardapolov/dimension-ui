package ru.dimension.ui.component.module.preview.spi;

import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;

public interface IPreviewChart {
  JXTaskPane asTaskPane();

  String getTitle();

  Runnable initializeUI();
  void loadData();
  void handleLegendChange(ChartLegendState state);
  void handleChartConfigState(ChartConfigState state);
  void setCollapsed(boolean collapsed);
}