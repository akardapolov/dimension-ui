package ru.dimension.ui.view.panel.report.pdf;

import lombok.Data;

@Data
public class PathPdfInfo {

  private String dirDesignName;

  public PathPdfInfo(String dirDesignName) {
    this.dirDesignName = dirDesignName;
  }

  public String getReportPdfPath() {
    return System.getProperty("user.dir") + System.getProperty("file.separator")
        + "report-data" + System.getProperty("file.separator")
        + "design" + System.getProperty("file.separator") + dirDesignName
        + System.getProperty("file.separator") + "report_" + getDateTimeFolder() + ".pdf";
  }

  public String getDateTimeFolder() {
    return dirDesignName.substring(dirDesignName.indexOf("_") + 1);
  }
}
