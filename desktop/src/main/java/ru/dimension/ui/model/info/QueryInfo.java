package ru.dimension.ui.model.info;

import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.DBType;
import ru.dimension.ui.model.sql.GatherDataMode;

@Data
@Accessors(chain = true)
@ToString
public class QueryInfo {

  private int id;
  private String name;
  private String text;
  private String description;

  private GatherDataMode gatherDataMode;

  private List<Metric> metricList = Collections.emptyList();

  private DBType dbType;
  private long deltaLocalServerTime;
}
