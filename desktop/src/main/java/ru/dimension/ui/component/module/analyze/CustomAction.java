package ru.dimension.ui.component.module.analyze;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Map;
import ru.dimension.db.model.profile.CProfile;

public interface CustomAction {

  void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected);

  void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected, Map<String, Color> seriesColorMap);

  void setBeginEnd(long begin, long end);
}
