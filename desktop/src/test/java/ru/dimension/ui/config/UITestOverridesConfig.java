package ru.dimension.ui.config;

import java.io.File;
import ru.dimension.di.DimensionDI;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.HandlerMock;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.tab.ConfigTab;

/**
 * Provides test-specific overrides for the Dependency Injection container.
 * This module replaces production components with mocks or test-harnessed instances.
 */
public final class UITestOverridesConfig {

  private UITestOverridesConfig() {}

  /**
   * Applies test-specific bindings to the DI builder.
   *
   * @param builder The DI builder to configure.
   * @param mockInstance The instance of HandlerMock containing the mock objects.
   * @param tempDir The temporary directory for test file operations.
   */
  public static void configure(DimensionDI.Builder builder, HandlerMock mockInstance, File tempDir) {
    // --- Core Overrides ---

    // Override FilesHelper to use the temporary directory for tests
    builder.provide(FilesHelper.class, ServiceLocator.singleton(
        () -> new FilesHelper(tempDir.getAbsolutePath())
    ));

    // --- UI Component Overrides ---

    // Override ButtonPanels to use the mock instances from HandlerMock
    builder.provideNamed(ButtonPanel.class, "queryButtonPanel",
                         ServiceLocator.singleton(mockInstance::getButtonQueryPanelMock));
    builder.provideNamed(ButtonPanel.class, "profileButtonPanel",
                         ServiceLocator.singleton(mockInstance::getButtonProfilePanelMock));
    builder.provideNamed(ButtonPanel.class, "taskButtonPanel",
                         ServiceLocator.singleton(mockInstance::getButtonTaskPanelMock));
    builder.provideNamed(ButtonPanel.class, "connectionButtonPanel",
                         ServiceLocator.singleton(mockInstance::getButtonConnectionPanelMock));
    builder.provideNamed(ButtonPanel.class, "metricQueryButtonPanel",
                         ServiceLocator.singleton(mockInstance::getButtonMetricQueryPanelMock));

    // Override the main configuration tab container
    builder.provideNamed(ConfigTab.class, "jTabbedPaneConfig",
                         ServiceLocator.singleton(mockInstance::getConfigTab));

    // Override the tables used in the config panels
    builder.provideNamed(JXTableCase.class, "profileConfigCase",
                         ServiceLocator.singleton(mockInstance::getProfileCase));
    builder.provideNamed(JXTableCase.class, "taskConfigCase",
                         ServiceLocator.singleton(mockInstance::getTaskCase));
    builder.provideNamed(JXTableCase.class, "connectionConfigCase",
                         ServiceLocator.singleton(mockInstance::getConnectionCase));
    builder.provideNamed(JXTableCase.class, "queryConfigCase",
                         ServiceLocator.singleton(mockInstance::getQueryCase));
  }
}