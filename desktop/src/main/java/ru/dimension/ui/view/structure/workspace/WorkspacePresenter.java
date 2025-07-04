package ru.dimension.ui.view.structure.workspace;

import static ru.dimension.ui.model.sql.GatherDataMode.BY_CLIENT_HTTP;
import static ru.dimension.ui.model.sql.GatherDataMode.BY_CLIENT_JDBC;
import static ru.dimension.ui.model.sql.GatherDataMode.BY_SERVER_JDBC;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JButton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.collector.Collector;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.executor.TaskExecutor;
import ru.dimension.ui.executor.TaskExecutorPool;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.router.listener.ProfileStartStopListener;
import ru.dimension.ui.router.listener.WorkspaceListener;
import ru.dimension.ui.state.NavigatorState;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.structure.WorkspaceView;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ActionName;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.RunStatus;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.view.ProgressbarState;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.analyze.module.ChartListModule;
import ru.dimension.ui.view.chart.stacked.StackChartPanel;
import ru.dimension.ui.view.structure.workspace.profile.WorkspaceProfileView;

@Log4j2
@Singleton
public class WorkspacePresenter implements WorkspaceListener, ActionListener, ProfileStartStopListener {

  private final WorkspaceView workspaceView;
  private final NavigatorState navigatorState;
  private final EventListener eventListener;
  private final ProfileManager profileManager;
  private final TaskExecutorPool taskExecutorPool;
  private final ConnectionPoolManager connectionPoolManager;
  private final ScheduledExecutorService executorService;
  private final SqlQueryState sqlQueryState;
  private final Collector collector;
  private final HttpResponseFetcher httpResponseFetcher;
  private final DStore dStore;

  @Inject
  public WorkspacePresenter(@Named("workspaceView") WorkspaceView workspaceView,
                            @Named("navigatorState") NavigatorState navigatorState,
                            @Named("eventListener") EventListener eventListener,
                            @Named("profileManager") ProfileManager profileManager,
                            @Named("taskExecutorPool") TaskExecutorPool taskExecutorPool,
                            @Named("connectionPoolManager") ConnectionPoolManager connectionPoolManager,
                            @Named("executorService") ScheduledExecutorService executorService,
                            @Named("sqlQueryState") SqlQueryState sqlQueryState,
                            @Named("collector") Collector collector,
                            @Named("httpResponseFetcher") HttpResponseFetcher httpResponseFetcher,
                            @Named("localDB") DStore dStore) {
    this.workspaceView = workspaceView;
    this.navigatorState = navigatorState;
    this.eventListener = eventListener;
    this.profileManager = profileManager;
    this.taskExecutorPool = taskExecutorPool;
    this.connectionPoolManager = connectionPoolManager;
    this.executorService = executorService;
    this.sqlQueryState = sqlQueryState;
    this.collector = collector;
    this.httpResponseFetcher = httpResponseFetcher;
    this.dStore = dStore;

    this.eventListener.addProfileSelectOnNavigator(this);
    this.eventListener.addProfileStartStopListener(this);
  }

  @Override
  public void fireOnSelectProfileOnNavigator(int profileId) {
    log.info("Fire on select profile on Navigator for profileId (workspace): " + profileId);
    log.info("Check selected profile in navigatorState (workspace): " + navigatorState.getSelectedProfile());

    executorService.submit(() -> {
      eventListener.fireProgressbarVisible(ProgressbarState.SHOW);
      try {
        eventListener.clearListener(WorkspaceProfileView.class);
        eventListener.clearListener(StackChartPanel.class);
        eventListener.clearListener(ChartListModule.class);

        WorkspaceProfileView workspaceProfileView = workspaceView.addWorkspaceProfileView(profileId);
        eventListener.addProfileStartStopListener(workspaceProfileView);
      } catch (Exception e) {
        log.catching(e);
      } finally {
        eventListener.fireProgressbarVisible(ProgressbarState.HIDE);
      }
    });
  }

  @Override
  public void fireOnStartOnWorkspaceProfileView(int profileId) {
    log.info("Fire on start for profileId (workspace -> ProfileView): " + profileId);

  }

  @Override
  public void fireOnStopOnWorkspaceProfileView(int profileId) {
    log.info("Fire on stop for profileId (workspace -> ProfileView): " + profileId);

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    JButton button = (JButton) e.getSource();
    String actionName = button.getActionCommand();

    executorService.submit(() -> {
      eventListener.fireProgressbarVisible(ProgressbarState.SHOW);
      try {
        actionPerformed(actionName);
      } catch (Exception exception) {
        log.catching(exception);

        ActionName actionNameMessage = ActionName.START;
        if (ActionName.STOP.name().equals(actionName)) {
          actionNameMessage = ActionName.STOP;
        }

        String title = "Error for " + actionNameMessage.name() + " action";
        DialogHelper.showErrorDialog(null, exception.getMessage(), title, exception);

        throw new RuntimeException(exception);
      } finally {
        eventListener.fireProgressbarVisible(ProgressbarState.HIDE);
      }
    });

    log.info("Button " + actionName + " has fired..");
  }

  private void actionPerformed(String actionName) {
    int profileId = navigatorState.getSelectedProfile();

    if (ActionName.START.name().equals(actionName)) {
      profileManager.setProfileInfoStatusById(profileId, RunStatus.RUNNING);
      eventListener.fireOnStartOnWorkspaceProfileView(profileId);

      profileManager.getProfileInfoById(profileId)
          .getTaskInfoList()
          .forEach(taskId -> {
            TaskInfo taskInfo = profileManager.getTaskInfoById(taskId);
            ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(taskInfo.getConnectionId());

            List<QueryInfo> queryInfoList = profileManager.getQueryInfoList().stream()
                .filter(f -> taskInfo.getQueryInfoList().stream().anyMatch(qId -> qId == f.getId()))
                .toList();

            List<TableInfo> tableInfoList = profileManager.getTableInfoList().stream()
                .filter(f -> queryInfoList.stream().anyMatch(qId -> qId.getName().equalsIgnoreCase(f.getTableName())))
                .toList();

            queryInfoList.forEach(queryInfo -> {
              queryInfo.setDbType(connectionInfo.getDbType());

              ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(profileId, taskId, queryInfo.getId());

              try {
                TableInfo tableInfo = tableInfoList
                    .stream()
                    .filter(f -> f.getTableName().equals(queryInfo.getName())).findAny()
                    .orElseThrow(() -> new NotFoundException("Table not found by name: " + queryInfo.getName()));

                if (BY_CLIENT_JDBC.equals(queryInfo.getGatherDataMode())) {
                  connectionPoolManager.createDataSource(connectionInfo);
                  Connection connection = connectionPoolManager.getConnection(connectionInfo);
                  collector.fillMetadataJdbc(profileTaskQueryKey, queryInfo, tableInfo, connection);
                } else if (BY_SERVER_JDBC.equals(queryInfo.getGatherDataMode())) {
                  connectionPoolManager.createDataSource(connectionInfo);
                  Connection connection = connectionPoolManager.getConnection(connectionInfo);
                  collector.fillMetadataJdbc(profileTaskQueryKey, queryInfo, tableInfo, connection);
                } else if (BY_CLIENT_HTTP.equals(queryInfo.getGatherDataMode())) {
                  collector.fillMetadataHttp(connectionInfo, queryInfo, tableInfo);

                  long lastTimestampLocalDb = dStore.getLast(tableInfo.getTableName(), Long.MIN_VALUE, Long.MIN_VALUE);

                  if (lastTimestampLocalDb == 0) {
                    lastTimestampLocalDb = System.currentTimeMillis();
                  }
                  sqlQueryState.initializeLastTimestamp(profileTaskQueryKey, lastTimestampLocalDb);
                }

                profileManager.updateQuery(queryInfo);
              } catch (Exception e) {
                profileManager.setProfileInfoStatusById(profileId, RunStatus.NOT_RUNNING);
                sqlQueryState.clear(profileTaskQueryKey);
                eventListener.fireOnStopOnWorkspaceProfileView(profileId);
                throw new RuntimeException(e);
              }
            });
          });

      profileManager.getProfileInfoById(profileId)
          .getTaskInfoList()
          .forEach(taskId -> {
            TaskInfo taskInfo = profileManager.getTaskInfoById(taskId);

            List<QueryInfo> queryInfoList = profileManager.getQueryInfoList().stream()
                .filter(f -> taskInfo.getQueryInfoList().stream().anyMatch(qId -> qId == f.getId()))
                .toList();

            List<TableInfo> tableInfoList = profileManager.getTableInfoList().stream()
                .filter(f -> queryInfoList.stream().anyMatch(qId -> qId.getName().equalsIgnoreCase(f.getTableName())))
                .toList();

            queryInfoList.forEach(queryInfo -> {

              TableInfo tableInfo = tableInfoList
                  .stream()
                  .filter(f -> f.getTableName().equals(queryInfo.getName())).findAny()
                  .orElseThrow(() -> new NotFoundException("Table info not found.."));

              tableInfo.getSProfile().getCsTypeMap()
                  .entrySet()
                  .stream()
                  .filter(f -> f.getValue().isTimeStamp())
                  .findAny()
                  .ifPresentOrElse(csTypeEntry -> log.info("Found timestamp field: " + csTypeEntry.getKey()),
                                   () -> {
                                     profileManager.setProfileInfoStatusById(profileId, RunStatus.NOT_RUNNING);
                                     eventListener.fireOnStopOnWorkspaceProfileView(profileId);
                                     throw new NotFoundException(
                                         "Not found timestamp field for query: " + queryInfo.getName());
                                   });

              if (tableInfo.getSProfile().getCsTypeMap().isEmpty()) {
                profileManager.setProfileInfoStatusById(profileId, RunStatus.NOT_RUNNING);
                eventListener.fireOnStopOnWorkspaceProfileView(profileId);

                throw new NotFoundException("Metadata for query: " + queryInfo.getName() + " not found..");
              }
            });
          });

      ProfileInfo profileInfo = profileManager.getProfileInfoById(profileId);

      profileInfo.getTaskInfoList()
          .forEach(taskId -> {
            TaskInfo taskInfo = profileManager.getTaskInfoById(taskId);
            ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(taskInfo.getConnectionId());

            List<QueryInfo> queryInfoList = profileManager.getQueryInfoList().stream()
                .filter(f -> taskInfo.getQueryInfoList().stream().anyMatch(qId -> qId == f.getId()))
                .toList();

            List<TableInfo> tableInfoList = profileManager.getTableInfoList().stream()
                .filter(f -> queryInfoList.stream().anyMatch(qId -> qId.getName().equalsIgnoreCase(f.getTableName())))
                .toList();

            queryInfoList.forEach(queryInfo -> {
              TableInfo tableInfo = tableInfoList.stream()
                  .filter(f -> f.getTableName().equals(queryInfo.getName()))
                  .findAny()
                  .orElseThrow(() -> new NotFoundException("Table info not found.."));

              ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(
                  profileId, taskInfo.getId(), queryInfo.getId());

              TaskExecutor taskExecutor =
                  new TaskExecutor(profileInfo, taskInfo, queryInfo, tableInfo, connectionInfo,
                                   connectionPoolManager,
                                   sqlQueryState,
                                   eventListener,
                                   httpResponseFetcher,
                                   dStore);

              taskExecutorPool.add(profileTaskQueryKey, taskExecutor);
            });
          });

      collector.start(profileInfo);

    } else if (ActionName.STOP.name().equals(actionName)) {
      ProfileInfo profileInfo = profileManager.getProfileInfoById(profileId);
      List<ProfileTaskQueryKey> profileTaskQueryKeys = profileManager.getProfileTaskQueryKeyList(profileId);

      if (eventListener.isProfileOnDashboardRunning(profileId)) {
        String message = String.format("Charts running on the Dashboard tab for the %s", profileInfo.getName());
        DialogHelper.showMessageDialog(null, message, "Information");
      }

      profileManager.setProfileInfoStatusById(profileId, RunStatus.NOT_RUNNING);
      eventListener.fireOnStopOnWorkspaceProfileView(profileId);

      collector.stop(profileInfo);

      profileInfo.getTaskInfoList()
          .forEach(taskId -> profileManager.getQueryInfoList(profileId, taskId)
              .forEach(queryInfo -> {
                ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(profileId, taskId, queryInfo.getId());
                taskExecutorPool.remove(profileTaskQueryKey);
              }));
    }
  }
}
