package ru.dimension.ui.config.ui;

import ru.dimension.di.DimensionDI;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.view.handler.connection.ConnectionButtonPanelHandler;
import ru.dimension.ui.view.handler.connection.ConnectionSelectionHandler;
import ru.dimension.ui.view.handler.connection.ConnectionTemplateTableHandler;
import ru.dimension.ui.view.handler.core.ConfigSelectionContext;
import ru.dimension.ui.view.handler.profile.MultiSelectTaskHandler;
import ru.dimension.ui.view.handler.profile.ProfileButtonPanelHandler;
import ru.dimension.ui.view.handler.profile.ProfileSelectionHandler;
import ru.dimension.ui.view.handler.query.QueryButtonPanelHandler;
import ru.dimension.ui.view.handler.query.QueryMetadataHandler;
import ru.dimension.ui.view.handler.query.QueryMetricButtonPanelHandler;
import ru.dimension.ui.view.handler.query.QueryMetricHandler;
import ru.dimension.ui.view.handler.query.QuerySelectionHandler;
import ru.dimension.ui.view.handler.task.MultiSelectQueryHandler;
import ru.dimension.ui.view.handler.task.TaskButtonPanelHandler;
import ru.dimension.ui.view.handler.task.TaskSelectionHandler;
import ru.dimension.ui.view.structure.ConfigView;
import ru.dimension.ui.view.structure.config.ConfigViewImpl;

public final class UIHandlersConfig {

  private UIHandlersConfig() {
  }

  public static void configure(DimensionDI.Builder builder) {
    builder
        .bindNamed(ConfigView.class, "configView", ConfigViewImpl.class)

        .provideNamed(ConfigSelectionContext.class, "configSelectionContext", ServiceLocator.singleton(ConfigSelectionContext::new))

        .bindNamed(ProfileSelectionHandler.class, "profileSelectionHandler", ProfileSelectionHandler.class)
        .bindNamed(ProfileButtonPanelHandler.class, "profileButtonPanelHandler", ProfileButtonPanelHandler.class)
        .bindNamed(MultiSelectTaskHandler.class, "multiSelectTaskHandler", MultiSelectTaskHandler.class)

        .bindNamed(TaskSelectionHandler.class, "taskSelectionHandler", TaskSelectionHandler.class)
        .bindNamed(TaskButtonPanelHandler.class, "taskButtonPanelHandler", TaskButtonPanelHandler.class)
        .bindNamed(MultiSelectQueryHandler.class, "multiSelectQueryHandler", MultiSelectQueryHandler.class)

        .bindNamed(ConnectionSelectionHandler.class, "connectionSelectionHandler", ConnectionSelectionHandler.class)
        .bindNamed(ConnectionButtonPanelHandler.class, "connectionButtonPanelHandler", ConnectionButtonPanelHandler.class)
        .bindNamed(ConnectionTemplateTableHandler.class, "connectionTemplateTableHandler", ConnectionTemplateTableHandler.class)

        .bindNamed(QuerySelectionHandler.class, "querySelectionHandler", QuerySelectionHandler.class)
        .bindNamed(QueryButtonPanelHandler.class, "queryButtonPanelHandler", QueryButtonPanelHandler.class)
        .bindNamed(QueryMetadataHandler.class, "queryMetadataHandler", QueryMetadataHandler.class)
        .bindNamed(QueryMetricButtonPanelHandler.class, "queryMetricButtonPanelHandler", QueryMetricButtonPanelHandler.class)
        .bindNamed(QueryMetricHandler.class, "queryMetricHandler", QueryMetricHandler.class);
  }
}