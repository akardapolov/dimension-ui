package ru.dimension.ui.component.module.analyze;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import ru.dimension.db.model.profile.CProfile;

public interface CustomAction {

  void setCustomSeriesFilter(CProfile cProfileFilter, List<String> filter);

  void setCustomSeriesFilter(CProfile cProfileFilter, List<String> filter, Map<String, Color> seriesColorMap);

  void setBeginEnd(long begin, long end);
}
