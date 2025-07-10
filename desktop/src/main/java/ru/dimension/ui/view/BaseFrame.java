package ru.dimension.ui.view;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.dashboard.DashboardComponent;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.executor.TaskExecutorPool;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.structure.AdHocView;
import ru.dimension.ui.view.structure.ConfigView;
import ru.dimension.ui.view.structure.NavigatorView;
import ru.dimension.ui.view.structure.ProgressbarView;
import ru.dimension.ui.view.structure.ReportView;
import ru.dimension.ui.view.structure.TemplateView;
import ru.dimension.ui.view.structure.ToolbarView;
import ru.dimension.ui.view.structure.WorkspaceView;

@Log4j2
@Singleton
public class BaseFrame extends JFrame {

  private final JSplitPane workspaceSplitPane;
  private final JTabbedPane mainTabPane;

  private final NavigatorView navigatorView;
  private final WorkspaceView workspaceView;
  private final AdHocView adHocView;
  private final ToolbarView toolbarView;
  private final ConfigView configView;
  private final TemplateView templateView;
  private final ReportView reportView;
  private final ProgressbarView progressbarView;

  private final ProfileManager profileManager;
  private final TaskExecutorPool taskExecutorPool;
  private final EventListener eventListener;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  @Override
  public void remove(Component comp) {
    super.remove(comp);
  }

  @Inject
  public BaseFrame(@Named("mainTabPane") JTabbedPane mainTabPane,
                   @Named("workspaceSplitPane") JSplitPane workspaceSplitPane,
                   @Named("reportView") ReportView reportView,
                   @Named("adHocView") AdHocView adHocView,
                   @Named("navigatorView") NavigatorView navigatorView,
                   @Named("workspaceView") WorkspaceView workspaceView,
                   @Named("toolbarView") ToolbarView toolbarView,
                   @Named("configView") ConfigView configView,
                   @Named("templateView") TemplateView templateView,
                   @Named("progressbarView") ProgressbarView progressbarView,
                   @Named("profileManager") ProfileManager profileManager,
                   @Named("taskExecutorPool") TaskExecutorPool taskExecutorPool,
                   @Named("sqlQueryState") SqlQueryState sqlQueryState,
                   @Named("eventListener") EventListener eventListener,
                   @Named("localDB") DStore dStore) throws HeadlessException {
    this.mainTabPane = mainTabPane;
    this.workspaceSplitPane = workspaceSplitPane;

    this.reportView = reportView;
    this.reportView.bindPresenter();

    this.adHocView = adHocView;
    this.adHocView.bindPresenter();

    this.navigatorView = navigatorView;
    this.navigatorView.bindPresenter();

    this.workspaceView = workspaceView;
    this.workspaceView.bindPresenter();

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
    this.dStore = dStore;

    this.setTitle("Dimension UI");
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setSize(new Dimension(400, 300));
    this.setExtendedState(JFrame.MAXIMIZED_BOTH); //todo window max size
    this.setVisible(true);

    this.fillWorkspaceSplitPane((Container) navigatorView, JSplitPane.LEFT);
    this.fillWorkspaceSplitPane((Container) workspaceView, JSplitPane.RIGHT);

    this.fillJTabbedPane(this.workspaceSplitPane, 0);

    DashboardComponent dashboardComponent = new DashboardComponent(profileManager, eventListener, sqlQueryState, dStore);
    this.eventListener.addProfileStartStopListener(dashboardComponent);

    JPanel dashboardPanel = new JPanel();
    PGHelper.cellXYRemainder(dashboardPanel, dashboardComponent.getMainSplitPane(), false);

    this.fillJTabbedPane(dashboardPanel, 1);

    this.fillJTabbedPane((Container) reportView, 2);
    this.fillJTabbedPane((Container) adHocView, 3);

    this.addMainArea((Container) this.toolbarView, BorderLayout.NORTH);
    this.addMainArea(this.mainTabPane, BorderLayout.CENTER);

    EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
    queue.push(new EventQueueProxy());
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        profileManager.getProfileInfoList()
            .forEach(profileInfo -> profileInfo.getTaskInfoList()
                .forEach(taskId -> {
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

        dStore.syncBackendDb();
        dStore.closeBackendDb();

        System.exit(0);
      }
    });
  }

  private void fillJTabbedPane(Container container,
                               int index) {
    this.mainTabPane.setComponentAt(index, container);
  }

  public void addMainArea(Container container,
                          String constraints) {
    this.add(container, constraints);
  }

  public void fillWorkspaceSplitPane(Container container,
                                     String constraints) {
    this.workspaceSplitPane.add(container, constraints);
  }

  public void repaintWorkspaceSplitPane() {
    this.workspaceSplitPane.revalidate();
    this.workspaceSplitPane.repaint();
  }

  static class EventQueueProxy extends EventQueue {

    protected void dispatchEvent(AWTEvent newEvent) {
      try {
        super.dispatchEvent(newEvent);
      } catch (Throwable t) {
        log.catching(t);
        String message = t.getMessage();

        if (message == null || message.length() == 0) {
          message = "Fatal: " + t.getClass();
        }

        JOptionPane.showMessageDialog(null, message,
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }
}
