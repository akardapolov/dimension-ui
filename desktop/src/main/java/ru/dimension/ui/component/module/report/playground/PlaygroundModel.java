package ru.dimension.ui.component.module.report.playground;

import java.util.HashMap;
import java.util.Map;
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
public class PlaygroundModel {
  private final Component component;
  private final ProfileManager profileManager;
  private final ConfigurationManager configurationManager;
  private final ReportManager reportManager;
  private final FilesHelper filesHelper;
  private final DStore dStore;

  private final Map<ProfileTaskQueryKey, QueryReportData> mapReportData = new HashMap<>();
  private final ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, ReportChartModule>> chartPanes = new ConcurrentHashMap<>();

  public boolean hasCharts() {
    return chartPanes.values().stream().anyMatch(innerMap -> !innerMap.isEmpty());
  }
}