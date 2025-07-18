package ru.dimension.ui.model.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;

@EqualsAndHashCode(callSuper = true)
@Data
public class CProfileReport extends CProfile {

  private String comment;
  private MetricFunction metricFunction;
  private ChartType chartType;
}
