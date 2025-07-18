package ru.dimension.ui.component.module.adhoc.charts;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.module.adhoc.AdHocChartModule;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.view.analyze.model.ChartCardState;
import ru.dimension.ui.view.analyze.model.ChartLegendState;

@Data
public class AdHocChartsModel {

  private final ProfileManager profileManager;
  private final ConnectionPoolManager connectionPoolManager;
  private final AdHocDatabaseManager adHocDatabaseManager;

  private ChartLegendState chartLegendState = ChartLegendState.SHOW;
  private ChartCardState chartCardState = ChartCardState.EXPAND_ALL;

  private final ConcurrentMap<AdHocKey, ConcurrentMap<CProfile, AdHocChartModule>> chartPanes = new ConcurrentHashMap<>();

  public AdHocChartsModel(ProfileManager profileManager,
                          ConnectionPoolManager connectionPoolManager,
                          AdHocDatabaseManager adHocDatabaseManager) {
    this.profileManager = profileManager;
    this.connectionPoolManager = connectionPoolManager;
    this.adHocDatabaseManager = adHocDatabaseManager;
  }
}