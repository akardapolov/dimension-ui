package ru.dimension.ui.component;

import jakarta.inject.Inject;
import java.util.function.Consumer;
import javax.swing.JSplitPane;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.module.ModelModule;
import ru.dimension.ui.component.module.factory.ZoomModuleFactory;
import ru.dimension.ui.component.module.factory.ModelModuleFactory;
import ru.dimension.ui.component.module.factory.PreviewChartsModuleFactory;
import ru.dimension.ui.component.module.factory.PreviewConfigModuleFactory;
import ru.dimension.ui.component.module.preview.ZoomModule;
import ru.dimension.ui.component.module.preview.PreviewChartsModule;
import ru.dimension.ui.component.module.preview.PreviewConfigModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.listener.ProfileStartStopListener;

@Log4j2
public class DashboardComponent implements HelperChart, ProfileStartStopListener {
  private final Component component = Component.DASHBOARD;

  @Getter
  private JSplitPane mainSplitPane;

  private final ProfileManager profileManager;
  private final EventListener eventListener;

  private final ModelModuleFactory modelModuleFactory;
  private final PreviewConfigModuleFactory previewConfigModuleFactory;
  private final PreviewChartsModuleFactory previewChartsModuleFactory;
  private final ZoomModuleFactory zoomModuleFactory;

  private ModelModule modelModule;
  private PreviewConfigModule configureModule;
  private PreviewChartsModule chartsModule;
  private ZoomModule zoomModule;

  private final MessageBroker broker = MessageBroker.getInstance();

  @Inject
  public DashboardComponent(ModelModuleFactory modelModuleFactory,
                            PreviewConfigModuleFactory previewConfigModuleFactory,
                            PreviewChartsModuleFactory previewChartsModuleFactory,
                            ZoomModuleFactory zoomModuleFactory,
                            ProfileManager profileManager,
                            EventListener eventListener) {
    this.modelModuleFactory = modelModuleFactory;
    this.previewConfigModuleFactory = previewConfigModuleFactory;
    this.previewChartsModuleFactory = previewChartsModuleFactory;
    this.zoomModuleFactory = zoomModuleFactory;
    this.profileManager = profileManager;
    this.eventListener = eventListener;

    initializeComponents();
  }

  private void initializeComponents() {
    mainSplitPane = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 170);

    JSplitPane verticalSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 60);
    verticalSplitPane.setDividerLocation(60);
    verticalSplitPane.setResizeWeight(0.5);

    modelModule = modelModuleFactory.create(component);
    configureModule = previewConfigModuleFactory.create(component);
    chartsModule = previewChartsModuleFactory.create(component);
    zoomModule = zoomModuleFactory.create(component);

    zoomModule.getPresenter().setChartPanesRef(chartsModule.getModel().getChartPanes());

    javax.swing.JPanel configWithZoomPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
    configWithZoomPanel.add(configureModule.getView(), java.awt.BorderLayout.CENTER);
    configWithZoomPanel.add(zoomModule.getView(), java.awt.BorderLayout.EAST);

    verticalSplitPane.setTopComponent(configWithZoomPanel);
    verticalSplitPane.setBottomComponent(chartsModule.getView());

    mainSplitPane.setLeftComponent(modelModule.getView());
    mainSplitPane.setRightComponent(verticalSplitPane);
  }

  @Override
  public void fireOnStartOnWorkspaceProfileView(int profileId) {
    handleProfileAction(profileId, (key) -> {
      eventListener.addCollectStartStopDashboardListener(key, chartsModule.getPresenter());
      eventListener.addCollectStartStopZoomListener(key, zoomModule.getPresenter());
    });
  }

  @Override
  public void fireOnStopOnWorkspaceProfileView(int profileId) {
    handleProfileAction(profileId, (key) -> {
      eventListener.clearListenerDashboardByKey(key);
      eventListener.clearListenerZoomByKey(key);
    });
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