package ru.dimension.ui.manager;

import java.io.IOException;
import ru.dimension.ui.model.report.DesignReportData;

public interface ReportManager {

  DesignReportData loadDesign(String designName) throws IOException;

  void saveDesign(DesignReportData designData) throws IOException;

  void deleteDesign(String configName) throws IOException;
}
