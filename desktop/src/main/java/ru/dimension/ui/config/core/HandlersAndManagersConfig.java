package ru.dimension.ui.config.core;

import ru.dimension.di.DimensionDI;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.ReportManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.manager.impl.AdHocDatabaseManagerImpl;
import ru.dimension.ui.manager.impl.ConfigurationManagerImpl;
import ru.dimension.ui.manager.impl.ConnectionPoolManagerImpl;
import ru.dimension.ui.manager.impl.ProfileManagerImpl;
import ru.dimension.ui.manager.impl.ReportManagerImpl;
import ru.dimension.ui.manager.impl.TemplateManagerImpl;
import ru.dimension.ui.view.handler.connection.ConnectionButtonPanelHandler;
import ru.dimension.ui.view.handler.connection.ConnectionSelectionHandler;
import ru.dimension.ui.view.handler.connection.ConnectionTemplateTableHandler;
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

public final class HandlersAndManagersConfig {

  private HandlersAndManagersConfig() {
  }

  public static void configure(DimensionDI.Builder builder) {
    builder
        // Handlers (alias scanned components to named keys)
        .provideNamed(ProfileSelectionHandler.class, "profileSelectionHandler",
                      () -> ServiceLocator.get(ProfileSelectionHandler.class))
        .provideNamed(TaskSelectionHandler.class, "taskSelectionHandler",
                      () -> ServiceLocator.get(TaskSelectionHandler.class))
        .provideNamed(ConnectionSelectionHandler.class, "connectionSelectionHandler",
                      () -> ServiceLocator.get(ConnectionSelectionHandler.class))
        .provideNamed(QuerySelectionHandler.class, "querySelectionHandler",
                      () -> ServiceLocator.get(QuerySelectionHandler.class))
        .provideNamed(QueryMetadataHandler.class, "queryMetadataHandler",
                      () -> ServiceLocator.get(QueryMetadataHandler.class))
        .provideNamed(ProfileButtonPanelHandler.class, "profileButtonPanelHandler",
                      () -> ServiceLocator.get(ProfileButtonPanelHandler.class))
        .provideNamed(TaskButtonPanelHandler.class, "taskButtonPanelHandler",
                      () -> ServiceLocator.get(TaskButtonPanelHandler.class))
        .provideNamed(ConnectionButtonPanelHandler.class, "connectionButtonPanelHandler",
                      () -> ServiceLocator.get(ConnectionButtonPanelHandler.class))
        .provideNamed(QueryButtonPanelHandler.class, "queryButtonPanelHandler",
                      () -> ServiceLocator.get(QueryButtonPanelHandler.class))
        .provideNamed(QueryMetricButtonPanelHandler.class, "queryMetricButtonPanelHandler",
                      () -> ServiceLocator.get(QueryMetricButtonPanelHandler.class))
        .provideNamed(QueryMetricHandler.class, "queryMetricHandler",
                      () -> ServiceLocator.get(QueryMetricHandler.class))
        .provideNamed(MultiSelectTaskHandler.class, "multiSelectTaskHandler",
                      () -> ServiceLocator.get(MultiSelectTaskHandler.class))
        .provideNamed(MultiSelectQueryHandler.class, "multiSelectQueryHandler",
                      () -> ServiceLocator.get(MultiSelectQueryHandler.class))
        .provideNamed(ConnectionTemplateTableHandler.class, "connectionTemplateTableHandler",
                      () -> ServiceLocator.get(ConnectionTemplateTableHandler.class))

        // Managers
        .bindNamed(ProfileManager.class, "profileManager", ProfileManagerImpl.class)
        .bindNamed(ConfigurationManager.class, "configurationManager", ConfigurationManagerImpl.class)
        .bindNamed(ConnectionPoolManager.class, "connectionPoolManager", ConnectionPoolManagerImpl.class)
        .bindNamed(AdHocDatabaseManager.class, "adHocDatabaseManager", AdHocDatabaseManagerImpl.class)
        .bindNamed(TemplateManager.class, "templateManager", TemplateManagerImpl.class)
        .bindNamed(ReportManager.class, "reportManager", ReportManagerImpl.class);
  }
}