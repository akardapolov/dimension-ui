package ru.dimension.ui.component;

import java.util.function.Consumer;
import javax.swing.JSplitPane;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.collector.Collector;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.module.ChartsModule;
import ru.dimension.ui.component.module.ConfigModule;
import ru.dimension.ui.component.module.ManageModule;
import ru.dimension.ui.component.module.ModelModule;
import ru.dimension.ui.executor.TaskExecutorPool;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.listener.ProfileStartStopListener;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class WorkspaceComponent implements HelperChart, ProfileStartStopListener {
  private final Component component = Component.WORKSPACE;

  @Getter
  private JSplitPane mainSplitPane;

  private JSplitPane manageConfigChartsSplitPane;

  private JSplitPane manageConfigSplitPane;

  private final EventListener eventListener;
  private final ProfileManager profileManager;
  private final TaskExecutorPool taskExecutorPool;
  private final ConnectionPoolManager connectionPoolManager;
  private final HttpResponseFetcher httpResponseFetcher;
  private final SqlQueryState sqlQueryState;
  private final Collector collector;
  private final DStore dStore;

  private ManageModule manageModule;
  private ModelModule modelModule;
  private ConfigModule configModule;
  private ChartsModule chartsModule;

  private final MessageBroker broker = MessageBroker.getInstance();

  public WorkspaceComponent(EventListener eventListener,
                            ProfileManager profileManager,
                            TaskExecutorPool taskExecutorPool,
                            ConnectionPoolManager connectionPoolManager,
                            HttpResponseFetcher httpResponseFetcher,
                            SqlQueryState sqlQueryState,
                            Collector collector,
                            DStore dStore) {
    this.eventListener = eventListener;
    this.profileManager = profileManager;
    this.taskExecutorPool = taskExecutorPool;
    this.connectionPoolManager = connectionPoolManager;
    this.httpResponseFetcher = httpResponseFetcher;
    this.sqlQueryState = sqlQueryState;
    this.collector = collector;
    this.dStore = dStore;

    initializeComponents();
  }

  private void initializeComponents() {
    mainSplitPane = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 170);

    manageConfigChartsSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 70);
    manageConfigChartsSplitPane.setDividerLocation(70);
    manageConfigChartsSplitPane.setResizeWeight(0.5);

    manageModule = new ManageModule(component, eventListener, profileManager,
                                    taskExecutorPool, connectionPoolManager, httpResponseFetcher, sqlQueryState,
                                    collector, dStore);
    modelModule = new ModelModule(component, profileManager);
    configModule = new ConfigModule(component);
    chartsModule = new ChartsModule(component, profileManager, sqlQueryState, dStore);

    manageConfigSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    manageConfigSplitPane.setLeftComponent(manageModule.getView());
    manageConfigSplitPane.setRightComponent(configModule.getView());
    manageConfigSplitPane.setDividerLocation(0.2);
    manageConfigSplitPane.setResizeWeight(0.2);

    manageConfigChartsSplitPane.setTopComponent(manageConfigSplitPane);
    manageConfigChartsSplitPane.setBottomComponent(chartsModule.getView());

    mainSplitPane.setLeftComponent(modelModule.getView());
    mainSplitPane.setRightComponent(manageConfigChartsSplitPane);

    broker.addReceiver(Destination.withDefault(component, Module.MODEL), modelModule);
  }

  @Override
  public void fireOnStartOnWorkspaceProfileView(int profileId) {
    handleProfileAction(profileId, (key) ->
        eventListener.addCollectStartStopWorkspaceListener(key, chartsModule.getPresenter()));
  }

  @Override
  public void fireOnStopOnWorkspaceProfileView(int profileId) {
    handleProfileAction(profileId, eventListener::clearListenerWorkspaceByKey);
  }

  private void handleProfileAction(int profileId, Consumer<ProfileTaskQueryKey> action) {
    ProfileInfo profileInfo = profileManager.getProfileInfoById(profileId);
    log.info("Handle {} workspace profile: {}",
             action.toString().contains("add") ? "start" : "stop",
             profileInfo.getName());

    profileInfo.getTaskInfoList().forEach(taskId -> {
      profileManager.getTaskInfoById(taskId).getQueryInfoList()
          .forEach(queryId -> {
            ProfileTaskQueryKey key = new ProfileTaskQueryKey(profileId, taskId, queryId);
            action.accept(key);
          });
    });
  }
}
