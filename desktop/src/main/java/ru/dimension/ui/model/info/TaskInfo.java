package ru.dimension.ui.model.info;

import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TaskInfo {

  private int id;
  private String name;
  private String description;
  private int pullTimeout;
  private int connectionId;

  private List<Integer> queryInfoList;
  private List<String> tableInfoList;
  private List<Integer> chartInfoList;
}
