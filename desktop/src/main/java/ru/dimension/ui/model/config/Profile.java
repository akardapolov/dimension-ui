package ru.dimension.ui.model.config;

import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Profile extends ConfigEntity {

  private String description;
  private List<Integer> taskList = Collections.emptyList();
}
