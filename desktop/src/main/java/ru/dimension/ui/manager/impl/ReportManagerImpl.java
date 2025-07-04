package ru.dimension.ui.manager.impl;

import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.helper.GsonHelper;
import ru.dimension.ui.manager.ReportManager;
import ru.dimension.ui.model.report.QueryReportData;

@Log4j2
@Singleton
public class ReportManagerImpl implements ReportManager {

  private final GsonHelper gsonHelper;

  @Inject
  public ReportManagerImpl(GsonHelper gsonHelper) {
    this.gsonHelper = gsonHelper;
  }


  @Override
  public void addConfig(Map<String, QueryReportData> config,
                        String formattedDate) {
    try {
      gsonHelper.add(config, formattedDate);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<String, QueryReportData> getConfig(String dirName,
                                                String fileName) {
    return gsonHelper.getConfig(dirName, fileName);
  }

  @Override
  public void deleteDesign(String configName) throws IOException {
    gsonHelper.deleteDesign(configName);
  }


}
