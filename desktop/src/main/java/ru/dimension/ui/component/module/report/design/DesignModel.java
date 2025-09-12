package ru.dimension.ui.component.module.report.design;

import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.module.ReportChartModule;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.ReportManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.report.QueryReportData;

@Data
public class DesignModel {
  private final Component component;
  private final ProfileManager profileManager;
  private final ConfigurationManager configurationManager;
  private final ReportManager reportManager;
  private final FilesHelper filesHelper;
  private final DStore dStore;

  private final ConcurrentMap<ProfileTaskQueryKey, QueryReportData> mapReportData = new ConcurrentHashMap<>();
  private final ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, ReportChartModule>> chartPanes = new ConcurrentHashMap<>();

  private ConcurrentMap<String, Entry<Date, Date>> designDateRanges = new ConcurrentHashMap<>();

  private String loadedDesignFolder = "";

  public boolean hasCharts() {
    return chartPanes.values().stream().anyMatch(innerMap -> !innerMap.isEmpty());
  }
}