package ru.dimension.ui.model.config;

import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Task extends ConfigEntity {

  private String description;

  /**
   * Timeout pull data from a remote source, in seconds
   **/
  private int pullTimeout;

  private int connectionId;
  private List<Integer> queryList = Collections.emptyList();
}
