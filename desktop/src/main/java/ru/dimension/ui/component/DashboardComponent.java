package ru.dimension.ui.component;

import java.util.function.Consumer;
import javax.swing.JSplitPane;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.module.ModelModule;
import ru.dimension.ui.component.module.preview.PreviewChartsModule;
import ru.dimension.ui.component.module.preview.PreviewConfigModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.listener.ProfileStartStopListener;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class DashboardComponent implements HelperChart, ProfileStartStopListener {
  private final MessageBroker.Component component = Component.DASHBOARD;

  @Getter
  private JSplitPane mainSplitPane;

  private final ProfileManager profileManager;
  private final EventListener eventListener;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  private ModelModule modelModule;
  private PreviewConfigModule configureModule;
  private PreviewChartsModule chartsModule;

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

    JSplitPane verticalSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 60);
    verticalSplitPane.setDividerLocation(60);
    verticalSplitPane.setResizeWeight(0.5);

    modelModule = new ModelModule(component, profileManager);
    configureModule = new PreviewConfigModule(component);
    chartsModule = new PreviewChartsModule(component, profileManager, sqlQueryState, dStore);

    verticalSplitPane.setTopComponent(configureModule.getView());
    verticalSplitPane.setBottomComponent(chartsModule.getView());

    mainSplitPane.setLeftComponent(modelModule.getView());
    mainSplitPane.setRightComponent(verticalSplitPane);

    broker.addReceiver(Destination.withDefault(component, Module.MODEL), modelModule);
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
