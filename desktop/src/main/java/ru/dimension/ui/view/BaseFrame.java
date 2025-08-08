package ru.dimension.ui.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.collector.Collector;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.component.AdHocComponent;
import ru.dimension.ui.component.DashboardComponent;
import ru.dimension.ui.component.WorkspaceComponent;
import ru.dimension.ui.executor.TaskExecutorPool;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.structure.ConfigView;
import ru.dimension.ui.view.structure.ProgressbarView;
import ru.dimension.ui.view.structure.ReportView;
import ru.dimension.ui.view.structure.TemplateView;
import ru.dimension.ui.view.structure.ToolbarView;

@Log4j2
@Singleton
public class BaseFrame extends JFrame {
  private static final int WORKSPACE_TAB_INDEX = 0;
  private static final int DASHBOARD_TAB_INDEX = 1;
  private static final int REPORT_TAB_INDEX = 2;
  private static final int AD_HOC_TAB_INDEX = 3;

  private final JTabbedPane mainTabPane;

  private final ToolbarView toolbarView;
  private final ConfigView configView;
  private final TemplateView templateView;
  private final ReportView reportView;
  private final ProgressbarView progressbarView;

  private final EventListener eventListener;
  private final ProfileManager profileManager;
  private final TaskExecutorPool taskExecutorPool;
  private final ConfigurationManager configurationManager;
  private final ConnectionPoolManager connectionPoolManager;
  private final AdHocDatabaseManager adHocDatabaseManager;
  private final HttpResponseFetcher httpResponseFetcher;
  private final SqlQueryState sqlQueryState;
  private final Collector collector;
  private final DStore dStore;

  @Override
  public void remove(Component comp) {
    super.remove(comp);
  }

  @Inject
  public BaseFrame(@Named("mainTabPane") JTabbedPane mainTabPane,
                   @Named("reportView") ReportView reportView,
                   @Named("toolbarView") ToolbarView toolbarView,
                   @Named("configView") ConfigView configView,
                   @Named("templateView") TemplateView templateView,
                   @Named("progressbarView") ProgressbarView progressbarView,
                   @Named("profileManager") ProfileManager profileManager,
                   @Named("taskExecutorPool") TaskExecutorPool taskExecutorPool,
                   @Named("sqlQueryState") SqlQueryState sqlQueryState,
                   @Named("eventListener") EventListener eventListener,
                   @Named("configurationManager") ConfigurationManager configurationManager,
                   @Named("connectionPoolManager") ConnectionPoolManager connectionPoolManager,
                   @Named("httpResponseFetcher") HttpResponseFetcher httpResponseFetcher,
                   @Named("adHocDatabaseManager") AdHocDatabaseManager adHocDatabaseManager,
                   @Named("collector") Collector collector,
                   @Named("localDB") DStore dStore) throws HeadlessException {
    this.mainTabPane = mainTabPane;

    this.reportView = reportView;
    this.reportView.bindPresenter();

    this.toolbarView = toolbarView;
    this.toolbarView.bindPresenter();

    this.configView = configView;
    this.configView.bindPresenter();

    this.templateView = templateView;
    this.templateView.bindPresenter();

    this.progressbarView = progressbarView;
    this.progressbarView.bindPresenter();

    this.profileManager = profileManager;
    this.taskExecutorPool = taskExecutorPool;

    this.sqlQueryState = sqlQueryState;
    this.eventListener = eventListener;
    this.configurationManager = configurationManager;
    this.connectionPoolManager = connectionPoolManager;
    this.adHocDatabaseManager = adHocDatabaseManager;
    this.httpResponseFetcher = httpResponseFetcher;
    this.collector = collector;
    this.dStore = dStore;

    this.setTitle("Dimension UI");
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setSize(new Dimension(400, 300));
    this.setExtendedState(JFrame.MAXIMIZED_BOTH); //todo window max size
    this.setVisible(true);

    initializeTabComponents();

    this.addMainArea((Container) this.toolbarView, BorderLayout.NORTH);
    this.addMainArea(this.mainTabPane, BorderLayout.CENTER);

    setupWindowClosingHandler();
  }

  private void initializeTabComponents() {
    initializeWorkspaceTab();
    initializeDashboardTab();
    initializeReportTab();
    initializeAdHocTab();
  }

  private void initializeWorkspaceTab() {
    WorkspaceComponent component = createWorkspaceComponent();
    JPanel panel = createComponentPanel(component.getMainSplitPane());
    addTab(panel, WORKSPACE_TAB_INDEX);
  }

  private void initializeDashboardTab() {
    DashboardComponent component = createDashboardComponent();
    JPanel panel = createComponentPanel(component.getMainSplitPane());
    addTab(panel, DASHBOARD_TAB_INDEX);
  }

  private void initializeReportTab() {
    addTab((Container) reportView, REPORT_TAB_INDEX);
  }

  private void initializeAdHocTab() {
    AdHocComponent component = createAdHocComponent();
    JPanel panel = createComponentPanel(component.getMainSplitPane());
    addTab(panel, AD_HOC_TAB_INDEX);
  }

  private WorkspaceComponent createWorkspaceComponent() {
    WorkspaceComponent component = new WorkspaceComponent(eventListener, profileManager,
                                                          taskExecutorPool, connectionPoolManager,
                                                          httpResponseFetcher, sqlQueryState,
                                                          collector, dStore);
    eventListener.addProfileStartStopListener(component);
    return component;
  }

  private DashboardComponent createDashboardComponent() {
    DashboardComponent component = new DashboardComponent(profileManager, eventListener, sqlQueryState, dStore);
    eventListener.addProfileStartStopListener(component);
    return component;
  }

  private AdHocComponent createAdHocComponent() {
    return new AdHocComponent(
        profileManager,
        configurationManager,
        eventListener,
        connectionPoolManager,
        adHocDatabaseManager
    );
  }

  private JPanel createComponentPanel(JComponent component) {
    JPanel panel = new JPanel();
    PGHelper.cellXYRemainder(panel, component, false);
    return panel;
  }

  private void addTab(Container container, int index) {
    fillJTabbedPane(container, index);
  }

  private void fillJTabbedPane(Container container,
                               int index) {
    this.mainTabPane.setComponentAt(index, container);
  }

  public void addMainArea(Container container,
                          String constraints) {
    this.add(container, constraints);
  }

  private void setupWindowClosingHandler() {
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        shutdownApplication();
      }
    });
  }

  private void shutdownApplication() {
    stopAllRunningTasks();
    cleanupResources();
    System.exit(0);
  }


  private void stopAllRunningTasks() {
    profileManager.getProfileInfoList().forEach(profileInfo ->
                                                    profileInfo.getTaskInfoList().forEach(taskId -> {
                                                      TaskInfo taskInfo = profileManager.getTaskInfoById(taskId);
                                                      log.info("Stopping all queries for task: {}", taskInfo.getName());

                                                      taskInfo.getQueryInfoList().forEach(queryId -> {
                                                        ProfileTaskQueryKey key = new ProfileTaskQueryKey(
                                                            profileInfo.getId(),
                                                            taskInfo.getId(),
                                                            queryId
                                                        );
                                                        taskExecutorPool.stop(key);
                                                        taskExecutorPool.remove(key);
                                                      });
                                                    })
    );
  }

  private void cleanupResources() {
    dStore.syncBackendDb();
    dStore.closeBackendDb();
  }
}
