package ru.dimension.ui.model.gantt;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GanttColumn {

  @EqualsAndHashCode.Include
  private String key;

  private Map<String, Double> gantt = new HashMap<>();
}