package ru.dimension.ui.manager;

import java.io.IOException;
import java.util.Map;
import ru.dimension.ui.model.report.QueryReportData;

public interface ReportManager {

  void addConfig(Map<String, QueryReportData> config,
                 String formattedDate);

  Map<String, QueryReportData> getConfig(String dirName,
                                         String fileName);

  void deleteDesign(String configName) throws IOException;
}
