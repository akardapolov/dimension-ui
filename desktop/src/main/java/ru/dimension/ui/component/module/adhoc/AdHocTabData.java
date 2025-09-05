package ru.dimension.ui.component.module.adhoc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;

@Data
public class AdHocTabData {
  private final String tabKey;
  private final Map<Integer, AdHocChartModule> charts = new ConcurrentHashMap<>();
}
