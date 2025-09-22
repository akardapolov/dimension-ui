package ru.dimension.ui.component.module.charts;

import java.beans.PropertyChangeListener;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.model.ChartCardState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.ChartModule;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.state.SqlQueryState;

@Data
public class ChartsModel {

  private final ProfileManager profileManager;

  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  private ChartLegendState chartLegendState = ChartLegendState.SHOW;
  private ChartCardState chartCardState = ChartCardState.EXPAND_ALL;

  private final ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, ChartModule>> chartPanes = new ConcurrentHashMap<>();

  private final ConcurrentMap<ChartModule, PropertyChangeListener> collapseListeners = new ConcurrentHashMap<>();
  private boolean programmaticChange = false;

  public ChartsModel(ProfileManager profileManager,
                     SqlQueryState sqlQueryState,
                     DStore dStore) {
    this.profileManager = profileManager;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;
  }
}
