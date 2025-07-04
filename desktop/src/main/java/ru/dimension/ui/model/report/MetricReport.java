package ru.dimension.ui.model.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.dimension.ui.model.config.Metric;

@EqualsAndHashCode(callSuper = true)
@Data
public class MetricReport extends Metric {

  private String comment;
}
