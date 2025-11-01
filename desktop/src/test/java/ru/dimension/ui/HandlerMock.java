package ru.dimension.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import ru.dimension.db.core.DStore;
import ru.dimension.di.DimensionDI;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.cache.AppCache;
import ru.dimension.ui.cache.impl.AppCacheImpl;
import ru.dimension.ui.config.core.CoreConfig;
import ru.dimension.ui.config.core.HandlersAndManagersConfig;
import ru.dimension.ui.config.core.RoutingSecurityStateConfig;
import ru.dimension.ui.config.ui.UIBaseConfig;
import ru.dimension.ui.config.ui.UIComponentsConfig;
import ru.dimension.ui.config.UITestOverridesConfig;
import ru.dimension.ui.config.ui.ViewAndPresenterConfig;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.impl.ConfigurationManagerImpl;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.security.EncryptDecrypt;
import ru.dimension.ui.view.handler.connection.ConnectionButtonPanelHandler;
import ru.dimension.ui.view.handler.profile.ProfileButtonPanelHandler;
import ru.dimension.ui.view.handler.query.QueryButtonPanelHandler;
import ru.dimension.ui.view.handler.task.TaskButtonPanelHandler;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.connection.ConnectionPanel;
import ru.dimension.ui.view.panel.config.profile.ProfilePanel;
import ru.dimension.ui.view.panel.config.query.QueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;
import ru.dimension.ui.view.structure.ConfigView;
import ru.dimension.ui.view.structure.config.ConfigViewImpl;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.warehouse.LocalDB;

@Log4j2
@Getter
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class HandlerMock {

  @TempDir
  static File configurationDir;

  protected ObjectMapper objectMapper;

  // --- Lazily-loaded dependencies for tests ---
  protected Supplier<QueryButtonPanelHandler> queryButtonPanelHandlerLazy;
  protected Supplier<QueryPanel> queryPanelLazy;
  protected Supplier<ConfigurationManagerImpl> configurationManagerLazy;
  protected Supplier<FilesHelper> filesHelperLazy;
  protected Supplier<LocalDB> localDBLazy;
  protected Supplier<EncryptDecrypt> encryptDecryptLazy;
  protected Supplier<ProfileButtonPanelHandler> profileButtonPanelHandlerLazy;
  protected Supplier<ProfilePanel> profilePanelLazy;
  protected Supplier<TaskButtonPanelHandler> taskButtonPanelHandlerLazy;
  protected Supplier<TaskPanel> taskPanelLazy;
  protected Supplier<ConnectionButtonPanelHandler> connectionButtonPanelHandlerLazy;
  protected Supplier<ConnectionPanel> connectionPanelLazy;
  protected Supplier<AppCacheImpl> appCacheLazy;
  protected Supplier<ConfigViewImpl> configViewLazy;

  // --- Mock UI Components ---
  protected final ButtonPanel buttonQueryPanelMock = new ButtonPanel();
  protected final ButtonPanel buttonProfilePanelMock = new ButtonPanel();
  protected final ButtonPanel buttonTaskPanelMock = new ButtonPanel();
  protected final ButtonPanel buttonConnectionPanelMock = new ButtonPanel();
  protected final ButtonPanel buttonMetricQueryPanelMock = new ButtonPanel();
  protected final ConfigTab configTab = new ConfigTab();
  protected final JXTableCase profileCase = GUIHelper.getJXTableCase(5, new String[]{QueryColumnNames.ID.getColName(), QueryColumnNames.NAME.getColName()});
  protected final JXTableCase taskCase = GUIHelper.getJXTableCase(5, new String[]{QueryColumnNames.ID.getColName(), QueryColumnNames.NAME.getColName()});
  protected final JXTableCase connectionCase = GUIHelper.getJXTableCase(5, new String[]{QueryColumnNames.ID.getColName(), QueryColumnNames.NAME.getColName()});
  protected final JXTableCase queryCase = GUIHelper.getJXTableCase(5, new String[]{QueryColumnNames.ID.getColName(), QueryColumnNames.NAME.getColName()});

  @BeforeAll
  public void setUp() {
    // 1. Create a new DI builder for this test run
    DimensionDI.Builder builder = DimensionDI.builder()
        .scanPackages("ru.dimension.ui");

    // 2. Apply all the standard production configuration modules
    CoreConfig.configure(builder);
    HandlersAndManagersConfig.configure(builder);
    RoutingSecurityStateConfig.configure(builder);
    ViewAndPresenterConfig.configure(builder);
    UIBaseConfig.configure(builder);
    UIComponentsConfig.configure(builder);

    // 3. Apply our test-specific overrides. This will replace any production
    //    bindings with our mocks (e.g., ButtonPanel, FilesHelper).
    UITestOverridesConfig.configure(builder, this, configurationDir);

    // 4. Build the single, complete DI container for the test
    builder.buildAndInit();

    // 5. Initialize lazy suppliers to fetch dependencies from the container
    queryButtonPanelHandlerLazy = () -> ServiceLocator.get(QueryButtonPanelHandler.class);
    queryPanelLazy = () -> ServiceLocator.get(QueryPanel.class, "queryConfigPanel");
    configurationManagerLazy = () -> (ConfigurationManagerImpl) ServiceLocator.get(ConfigurationManager.class, "configurationManager");
    filesHelperLazy = () -> ServiceLocator.get(FilesHelper.class);
    localDBLazy = () -> (LocalDB) ServiceLocator.get(DStore.class, "localDB");
    encryptDecryptLazy = () -> ServiceLocator.get(EncryptDecrypt.class);
    profileButtonPanelHandlerLazy = () -> ServiceLocator.get(ProfileButtonPanelHandler.class);
    profilePanelLazy = () -> ServiceLocator.get(ProfilePanel.class, "profileConfigPanel");
    taskButtonPanelHandlerLazy = () -> ServiceLocator.get(TaskButtonPanelHandler.class);
    taskPanelLazy = () -> ServiceLocator.get(TaskPanel.class, "taskConfigPanel");
    connectionButtonPanelHandlerLazy = () -> ServiceLocator.get(ConnectionButtonPanelHandler.class);
    connectionPanelLazy = () -> ServiceLocator.get(ConnectionPanel.class, "connectionConfigPanel");
    appCacheLazy = () -> (AppCacheImpl) ServiceLocator.get(AppCache.class, "appCache");
    configViewLazy = () -> (ConfigViewImpl) ServiceLocator.get(ConfigView.class, "configView");

    objectMapper = new ObjectMapper();
  }

  protected void createQueryTest(Query query) {
    buttonQueryPanelMock.getBtnNew().doClick();
    queryPanelLazy.get().getMainQueryPanel().getQueryName().setText(query.getName());
    queryPanelLazy.get().getMainQueryPanel().getQueryDescription().setText(query.getDescription());
    buttonQueryPanelMock.getBtnSave().doClick();
  }

  protected void createConnectionTest(Connection connection) {
    buttonConnectionPanelMock.getBtnNew().doClick();
    connectionPanelLazy.get().getConnTypeTab().setSelectedTab(ConnectionTypeTabPane.JDBC);
    connectionPanelLazy.get().getJTextFieldConnectionName().setText(connection.getName());
    connectionPanelLazy.get().getJTextFieldConnectionURL().setText(connection.getUrl());
    connectionPanelLazy.get().getJTextFieldConnectionUserName().setText(connection.getUserName());
    connectionPanelLazy.get().getJTextFieldConnectionPassword().setText(connection.getPassword());
    connectionPanelLazy.get().getJTextFieldConnectionJar().setText(connection.getJar());
    connectionPanelLazy.get().getJTextFieldConnectionDriver().setText(connection.getDriver());
    buttonConnectionPanelMock.getBtnSave().doClick();
  }

  protected void createTaskTest(Task task) {
    buttonTaskPanelMock.getBtnNew().doClick();
    taskPanelLazy.get().getJTextFieldTask().setText(task.getName());
    taskPanelLazy.get().getJTextFieldDescription().setText(task.getDescription());
    buttonTaskPanelMock.getBtnSave().doClick();
  }

  protected String getTestData(String fileName) throws IOException {
    return Files.readString(Paths.get("src", "test", "resources", "config", fileName));
  }

  protected void disposeWindows() {
    ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    scheduledExecutor.scheduleAtFixedRate(() -> {
      Window[] windows = Window.getWindows();
      if (windows.length > 0) {
        log.info("Disposing {} window(s)...", windows.length);
        for (Window window : windows) {
          try {
            window.dispose();
          } catch (Exception e) {
            log.catching(e);
          }
        }
      }
    }, 2, 1, TimeUnit.SECONDS);
  }

  @AfterAll
  public void tearDown() {
    // Ensure lazy suppliers have been resolved before using them
    if (localDBLazy != null && localDBLazy.get() != null) {
      localDBLazy.get().closeBackendDb();
    }
    if (filesHelperLazy != null && filesHelperLazy.get() != null) {
      filesHelperLazy.get().cleanDir();
    }
  }
}