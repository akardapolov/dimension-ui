package ru.dimension.ui.manager.impl;

import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.helper.GsonHelper;
import ru.dimension.ui.manager.ReportManager;
import ru.dimension.ui.model.report.DesignReportData;

@Log4j2
@Singleton
public class ReportManagerImpl implements ReportManager {

  private final GsonHelper gsonHelper;

  @Inject
  public ReportManagerImpl(GsonHelper gsonHelper) {
    this.gsonHelper = gsonHelper;
  }

  @Override
  public void deleteDesign(String configName) throws IOException {
    gsonHelper.deleteDesign(configName);
  }

  @Override
  public void saveDesign(DesignReportData designData) throws IOException {
    gsonHelper.saveDesign(designData);
  }

  @Override
  public DesignReportData loadDesign(String designName) throws IOException {
    return gsonHelper.loadDesign(designName);
  }
}
