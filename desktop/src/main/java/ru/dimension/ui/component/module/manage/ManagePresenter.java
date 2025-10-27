package ru.dimension.ui.component.module.manage;

import static ru.dimension.ui.model.sql.GatherDataMode.BY_CLIENT_HTTP;
import static ru.dimension.ui.model.sql.GatherDataMode.BY_CLIENT_JDBC;
import static ru.dimension.ui.model.sql.GatherDataMode.BY_SERVER_JDBC;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.swing.JButton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.collector.Collector;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.PreviewModule;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.executor.TaskExecutor;
import ru.dimension.ui.executor.TaskExecutorPool;
import ru.dimension.ui.helper.DialogHelper;
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
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class ManagePresenter implements ActionListener, MessageAction {
  private final MessageBroker.Component component;

  private final ManageView view;
  private final EventListener eventListener;
  private final ProfileManager profileManager;
  private final TaskExecutorPool taskExecutorPool;
  private final ConnectionPoolManager connectionPoolManager;
  private final Collector collector;
  private final SqlQueryState sqlQueryState;
  private final HttpResponseFetcher httpResponseFetcher;
  private final DStore dStore;

  private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

  private final MessageBroker broker = MessageBroker.getInstance();

  private ProfileTaskQueryKey key = new ProfileTaskQueryKey();

  private PreviewModule previewModule;

  public ManagePresenter(MessageBroker.Component component,
                         ManageView view,
                         EventListener eventListener,
                         ProfileManager profileManager,
                         TaskExecutorPool taskExecutorPool,
                         ConnectionPoolManager connectionPoolManager,
                         HttpResponseFetcher httpResponseFetcher,
                         SqlQueryState sqlQueryState,
                         Collector collector,
                         DStore dStore) {
    this.component = component;
    this.view = view;

    this.eventListener = eventListener;
    this.profileManager = profileManager;
    this.taskExecutorPool = taskExecutorPool;
    this.connectionPoolManager = connectionPoolManager;
    this.collector = collector;
    this.sqlQueryState = sqlQueryState;
    this.httpResponseFetcher = httpResponseFetcher;
    this.dStore = dStore;

    setupListeners();
  }

  private void setupListeners() {
    view.getProfileActionPanel().getStart().addActionListener(this);
    view.getProfileActionPanel().getStop().addActionListener(this);
    view.getProfileActionPanel().getPreview().addActionListener(this);
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

    log.info("Button {} has fired..", actionName);
  }

  private void actionPerformed(String actionName) {
    if (ActionName.START.name().equals(actionName)) {
      profileManager.setProfileInfoStatusById(key.getProfileId(), RunStatus.RUNNING);
      eventListener.fireOnStartOnWorkspaceProfileView(key.getProfileId());

      profileManager.getProfileInfoById(key.getProfileId())
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

              ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(key.getProfileId(), taskId, queryInfo.getId());

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
                profileManager.setProfileInfoStatusById(key.getProfileId(), RunStatus.NOT_RUNNING);
                sqlQueryState.clear(profileTaskQueryKey);
                eventListener.fireOnStopOnWorkspaceProfileView(key.getProfileId());
                throw new RuntimeException(e);
              }
            });
          });

      profileManager.getProfileInfoById(key.getProfileId())
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
                  .ifPresentOrElse(csTypeEntry -> log.info("Found timestamp field: {}", csTypeEntry.getKey()),
                                   () -> {
                                     profileManager.setProfileInfoStatusById(key.getProfileId(), RunStatus.NOT_RUNNING);
                                     eventListener.fireOnStopOnWorkspaceProfileView(key.getProfileId());
                                     throw new NotFoundException(
                                         "Not found timestamp field for query: " + queryInfo.getName());
                                   });

              if (tableInfo.getSProfile().getCsTypeMap().isEmpty()) {
                profileManager.setProfileInfoStatusById(key.getProfileId(), RunStatus.NOT_RUNNING);
                eventListener.fireOnStopOnWorkspaceProfileView(key.getProfileId());

                throw new NotFoundException("Metadata for query: " + queryInfo.getName() + " not found..");
              }
            });
          });

      ProfileInfo profileInfo = profileManager.getProfileInfoById(key.getProfileId());

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
                  key.getProfileId(), taskInfo.getId(), queryInfo.getId());

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
      ProfileInfo profileInfo = profileManager.getProfileInfoById(key.getProfileId());

      if (eventListener.isProfileOnDashboardRunning(key.getProfileId())) {
        String message = String.format("Charts running on the Dashboard tab for the %s", profileInfo.getName());
        DialogHelper.showMessageDialog(null, message, "Information");
      }

      profileManager.setProfileInfoStatusById(key.getProfileId(), RunStatus.NOT_RUNNING);
      eventListener.fireOnStopOnWorkspaceProfileView(key.getProfileId());

      collector.stop(profileInfo);

      profileInfo.getTaskInfoList()
          .forEach(taskId -> profileManager.getQueryInfoList(key.getProfileId(), taskId)
              .forEach(queryInfo -> {
                ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(key.getProfileId(), taskId, queryInfo.getId());
                taskExecutorPool.remove(profileTaskQueryKey);
              }));
    } else if (ActionName.PREVIEW.name().equals(actionName)) {
      handlePreviewAction();
    }

    setProfileStatusJLabel();
  }

  private void handlePreviewAction() {
    if (previewModule != null && previewModule.getModel().getKey().equals(key)) {
      log.info("Already running preview model by key: {}", key);
    } else {
      eventListener.clearListenerPreviewByClass(PreviewModule.class);
      previewModule = new PreviewModule(key, profileManager, sqlQueryState, dStore);
      eventListener.addCollectStartStopPreviewListener(key, previewModule);
    }

    previewModule.show();
  }

  private void setProfileStatusJLabel() {
    ProfileInfo profileInfo = profileManager.getProfileInfoById(key.getProfileId());
    view.getProfileActionPanel().setButtonState(profileInfo.getName(), profileInfo.getStatus());
  }

  @Override
  public void receive(Message message) {
    log.info("Message received: {}", message.action());

    switch (message.action()) {
      case SET_PROFILE_TASK_QUERY_KEY -> {
        key = message.parameters().get("key");

        ProfileInfo profileInfo = profileManager.getProfileInfoById(key.getProfileId());
        view.getProfileActionPanel().setButtonState(profileInfo.getName(), profileInfo.getStatus());
      }
    }
  }
}