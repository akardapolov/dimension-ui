package ru.dimension.ui.component.module.preview.charts;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.PreviewChartModule;
import ru.dimension.ui.component.module.preview.chart.ChartDetailDialog;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.state.SqlQueryState;

@Data
public class PreviewChartsModel {

  private final ProfileManager profileManager;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  private ChartLegendState chartLegendState = ChartLegendState.SHOW;
  private ChartCardState chartCardState = ChartCardState.EXPAND_ALL;

  private final ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, PreviewChartModule>> chartPanes = new ConcurrentHashMap<>();

  private ChartDetailDialog chartDetailDialog;

  public PreviewChartsModel(ProfileManager profileManager,
                            SqlQueryState sqlQueryState,
                            DStore dStore) {
    this.profileManager = profileManager;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;
  }
}
