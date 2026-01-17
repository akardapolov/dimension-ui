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
   * @param mock    The instance of HandlerMock containing the mock objects/UI components.
   * @param tempDir The temporary directory for test file operations.
   */
  public static void configure(DimensionDI.Builder builder, HandlerMock mock, File tempDir) {

    // --- 1. Core Helpers ---
    // Override FilesHelper to use the JUnit @TempDir
    builder.provide(FilesHelper.class,
                    ServiceLocator.singleton(() -> new FilesHelper(tempDir.getAbsolutePath())));

    // --- 2. Button Panels (Mocks) ---
    // Injecting pre-created button panels from HandlerMock to control clicks in tests
    builder.provideNamed(ButtonPanel.class, "queryButtonPanel",       ServiceLocator.singleton(mock::getButtonQueryPanelMock));
    builder.provideNamed(ButtonPanel.class, "profileButtonPanel",     ServiceLocator.singleton(mock::getButtonProfilePanelMock));
    builder.provideNamed(ButtonPanel.class, "taskButtonPanel",        ServiceLocator.singleton(mock::getButtonTaskPanelMock));
    builder.provideNamed(ButtonPanel.class, "connectionButtonPanel",  ServiceLocator.singleton(mock::getButtonConnectionPanelMock));
    builder.provideNamed(ButtonPanel.class, "metricQueryButtonPanel", ServiceLocator.singleton(mock::getButtonMetricQueryPanelMock));

    // --- 3. Main Configuration Views ---
    // Override the composite tab view that holds the tables below
    builder.provideNamed(ConfigTab.class, "configTab", ServiceLocator.singleton(mock::getConfigTab));

    // --- 4. Configuration Tables ---
    // Injecting tables used in the ConfigTab to inspect their contents in tests
    builder.provideNamed(JXTableCase.class, "profileConfigCase",    ServiceLocator.singleton(mock::getProfileCase));
    builder.provideNamed(JXTableCase.class, "taskConfigCase",       ServiceLocator.singleton(mock::getTaskCase));
    builder.provideNamed(JXTableCase.class, "connectionConfigCase", ServiceLocator.singleton(mock::getConnectionCase));
    builder.provideNamed(JXTableCase.class, "queryConfigCase",      ServiceLocator.singleton(mock::getQueryCase));

    // --- 5. Task Execution Tables ---
    // Tables related to task lists and selections
    builder.provideNamed(JXTableCase.class, "taskListCase",         ServiceLocator.singleton(mock::getTaskListCase));
    builder.provideNamed(JXTableCase.class, "selectedTaskCase",     ServiceLocator.singleton(mock::getSelectedTaskCase));
    builder.provideNamed(JXTableCase.class, "templateListTaskCase", ServiceLocator.singleton(mock::getTemplateListTaskCase));
  }
}