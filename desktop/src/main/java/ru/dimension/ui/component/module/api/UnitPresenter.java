package ru.dimension.ui.component.module.api;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Map;
import ru.dimension.db.model.profile.CProfile;

public interface UnitPresenter {
  void initializePresenter();
  void initializeCharts();
  void updateChart();
  void handleLegendChangeAll(Boolean showLegend);
  void handleFilterChange(Map<CProfile, LinkedHashSet<String>> topMapSelected,
                          Map<String, Color> seriesColorMap);
}