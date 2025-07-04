package ru.dimension.ui.model.config;

import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.dimension.ui.model.sql.GatherDataMode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Query extends ConfigEntity {

  private String text;
  private String description;

  private GatherDataMode gatherDataMode;

  private List<Metric> metricList = Collections.emptyList();
}
