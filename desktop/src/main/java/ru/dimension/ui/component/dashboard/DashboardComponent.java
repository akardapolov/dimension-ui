package ru.dimension.ui.component.dashboard;

import java.util.function.Consumer;
import javax.swing.JSplitPane;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.Destination;
import ru.dimension.ui.component.MessageBroker;
import ru.dimension.ui.component.MessageBroker.Component;
import ru.dimension.ui.component.MessageBroker.Module;
import ru.dimension.ui.component.module.ChartsModule;
import ru.dimension.ui.component.module.ConfigModule;
import ru.dimension.ui.component.module.ModelModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.listener.ProfileStartStopListener;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.chart.HelperChart;

@Log4j2
public class DashboardComponent implements HelperChart, ProfileStartStopListener {

  @Getter
  private JSplitPane mainSplitPane;

  private final ProfileManager profileManager;
  private final EventListener eventListener;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  private ModelModule modelModule;
  private ConfigModule configModule;
  private ChartsModule chartsModule;

  private final MessageBroker broker = MessageBroker.getInstance();

  public DashboardComponent(ProfileManager profileManager,
                            EventListener eventListener,
                            SqlQueryState sqlQueryState,
                            DStore dStore) {
    this.profileManager = profileManager;
    this.eventListener = eventListener;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;

    initializeComponents();
  }

  private void initializeComponents() {
    mainSplitPane = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 170);

    JSplitPane verticalSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 55);
    verticalSplitPane.setDividerLocation(55);
    verticalSplitPane.setResizeWeight(0.5);

    modelModule = new ModelModule(profileManager);
    configModule = new ConfigModule();
    chartsModule = new ChartsModule(profileManager, sqlQueryState, dStore);

    verticalSplitPane.setTopComponent(configModule.getView());
    verticalSplitPane.setBottomComponent(chartsModule.getView());

    mainSplitPane.setLeftComponent(modelModule.getView());
    mainSplitPane.setRightComponent(verticalSplitPane);

    broker.addReceiver(Destination.withDefault(Component.DASHBOARD, Module.MODEL), modelModule);
  }

  @Override
  public void fireOnStartOnWorkspaceProfileView(int profileId) {
    handleProfileAction(profileId, (key) ->
        eventListener.addCollectStartStopDashboardListener(key, chartsModule.getPresenter()));
  }

  @Override
  public void fireOnStopOnWorkspaceProfileView(int profileId) {
    handleProfileAction(profileId, eventListener::clearListenerDashboardByKey);
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
