package ru.dimension.ui.component.module.chart.preview;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Map;
import ru.dimension.db.model.profile.CProfile;

public record DetailChartContext(Map<String, Color> seriesColorMap,
                                 Map<CProfile, LinkedHashSet<String>> topMapSelected) {

  public DetailChartContext(Map<String, Color> seriesColorMap,
                            Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    this.seriesColorMap = (seriesColorMap == null) ? Map.of() : Map.copyOf(seriesColorMap);
    this.topMapSelected = (topMapSelected == null) ? Map.of() : Map.copyOf(topMapSelected);
  }
}