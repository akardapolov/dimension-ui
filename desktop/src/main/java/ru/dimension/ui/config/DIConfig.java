package ru.dimension.ui.config;

import ru.dimension.ui.config.core.CoreConfig;
import ru.dimension.ui.config.core.HandlersAndManagersConfig;
import ru.dimension.ui.config.core.RoutingSecurityStateConfig;
import ru.dimension.ui.config.ui.UIBaseConfig;
import ru.dimension.ui.config.ui.UIComponentsConfig;
import ru.dimension.ui.config.ui.ViewAndPresenterConfig;

/**
 * Central configuration for Dimension-DI.
 * Initializes all application dependencies.
 */
public final class DIConfig {

  private DIConfig() {
  }

  public static void init() {
    ru.dimension.di.DimensionDI.Builder builder = ru.dimension.di.DimensionDI.builder()
        // Scan the entire UI package for classes with @Inject constructors
        .scanPackages("ru.dimension.ui");

    CoreConfig.configure(builder);
    HandlersAndManagersConfig.configure(builder);
    RoutingSecurityStateConfig.configure(builder);
    ViewAndPresenterConfig.configure(builder);
    UIBaseConfig.configure(builder);
    UIComponentsConfig.configure(builder);

    builder.buildAndInit();
  }
}