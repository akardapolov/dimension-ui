package ru.dimension.ui.model.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.function.GroupFunction;

@EqualsAndHashCode(callSuper = true)
@Data
public class CProfileReport extends CProfile {

  private String comment;
  private GroupFunction groupFunction;
  private ChartType chartType;
}
