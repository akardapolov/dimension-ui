package ru.dimension.ui.component.module.zoom;

import lombok.Data;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.zoom.internal.ZoomDashboardDialog;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.state.SqlQueryState;

@Data
public class ZoomModel {

  private final MessageBroker.Component component;
  private final ProfileManager profileManager;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  private ZoomDashboardDialog dashboardDialog;
  private RenderMode renderMode = RenderMode.CURRENT;

  public ZoomModel(MessageBroker.Component component,
                   ProfileManager profileManager,
                   SqlQueryState sqlQueryState,
                   DStore dStore) {
    this.component = component;
    this.profileManager = profileManager;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;
  }
}