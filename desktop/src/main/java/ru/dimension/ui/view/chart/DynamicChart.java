package ru.dimension.ui.view.chart;

import java.awt.Color;
import java.util.Map;
import org.jfree.chart.ChartPanel;

public interface DynamicChart {

  void initialize();

  ChartPanel getChartPanel();

  Map<String, Color> getSeriesColorMap();

  void setChartTitle(String titleText);

  void loadSeriesColorInternal(String seriesName);

  void setDateAxis(long begin, long end);

  void addSeriesValue(double x,
                      double y,
                      String seriesName);

  void deleteSeriesValue(double x,
                         String seriesName);

  void deleteAllSeriesData(int holdRange);

  double getEndXValue();

  void setNotifyPlot(boolean notify);

  boolean isNotifyPlot();
}
