package ru.dimension.ui.model.report;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DesignReportData {
  private final String folderName;
  private final long dateFrom;
  private final long dateTo;

  private final Map<String, QueryReportData> mapReportData = new HashMap<>();
}
