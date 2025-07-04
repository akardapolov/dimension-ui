package ru.dimension.ui.view.chart;

import org.jfree.chart.util.IDetailPanel;

public interface DetailChart {

  void addChartListenerReleaseMouse(IDetailPanel iDetailPanel);

  void removeChartListenerReleaseMouse(IDetailPanel iDetailPanel);
}
