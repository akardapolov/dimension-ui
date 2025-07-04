package ru.dimension.ui.view.analyze;

import java.util.List;
import ru.dimension.db.model.profile.CProfile;

public interface CustomAction {

  void setCustomSeriesFilter(CProfile cProfileFilter, List<String> filter);
  void setBeginEnd(long begin, long end);
}
