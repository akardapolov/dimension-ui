package ru.dimension.ui.component;

import jakarta.inject.Inject;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.module.ChartsModule;
import ru.dimension.ui.component.module.ConfigModule;
import ru.dimension.ui.component.module.ManageModule;
import ru.dimension.ui.component.module.ModelModule;
import ru.dimension.ui.component.module.factory.ChartsModuleFactory;
import ru.dimension.ui.component.module.factory.ConfigModuleFactory;
import ru.dimension.ui.component.module.factory.ManageModuleFactory;
import ru.dimension.ui.component.module.factory.ModelModuleFactory;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.listener.ProfileStartStopListener;

@Log4j2
public class WorkspaceComponent implements HelperChart, ProfileStartStopListener {
  private final Component component = Component.WORKSPACE;

  @Getter
  private JSplitPane mainSplitPane;

  private JSplitPane manageConfigChartsSplitPane;
  private JSplitPane manageConfigSplitPane;

  private final EventListener eventListener;
  private final ProfileManager profileManager;

  private final ManageModuleFactory manageModuleFactory;
  private ManageModule manageModule;

  private final ModelModuleFactory modelModuleFactory;
  private ModelModule modelModule;

  private final ConfigModuleFactory configModuleFactory;
  private ConfigModule configModule;

  private final ChartsModuleFactory chartsModuleFactory;
  private ChartsModule chartsModule;

  private static final int TOP_HEIGHT_PX = 70;

  @Inject
  public WorkspaceComponent(ModelModuleFactory modelModuleFactory,
                            ManageModuleFactory manageModuleFactory,
                            ConfigModuleFactory configModuleFactory,
                            ChartsModuleFactory chartsModuleFactory,
                            EventListener eventListener,
                            ProfileManager profileManager) {
    this.modelModuleFactory = modelModuleFactory;
    this.manageModuleFactory = manageModuleFactory;
    this.configModuleFactory = configModuleFactory;
    this.chartsModuleFactory = chartsModuleFactory;

    this.eventListener = eventListener;
    this.profileManager = profileManager;

    initializeComponents();
  }

  private void initializeComponents() {
    manageModule = manageModuleFactory.create(component);
    modelModule  = modelModuleFactory.create(component);
    configModule = configModuleFactory.create(component);
    chartsModule = chartsModuleFactory.create(component);

    mainSplitPane = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 170);
    mainSplitPane.setResizeWeight(0.0);

    manageConfigSplitPane = createManageConfigSplit();

    manageConfigChartsSplitPane = createRightVerticalSplit(
        manageConfigSplitPane,
        chartsModule.getView(),
        TOP_HEIGHT_PX,
        0.2
    );

    mainSplitPane.setLeftComponent(modelModule.getView());
    mainSplitPane.setRightComponent(manageConfigChartsSplitPane);
  }

  private JSplitPane createManageConfigSplit() {
    JSplitPane sp = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 170);
    sp.setContinuousLayout(true);
    sp.setResizeWeight(0.2);

    sp.setLeftComponent(manageModule.getView());
    sp.setRightComponent(configModule.getView());
    return sp;
  }

  private JSplitPane createRightVerticalSplit(JComponent top,
                                              JComponent bottom,
                                              int topHeightPx,
                                              double topHorizontalDividerRatio) {

    JComponent shrinkableTop = shrinkable(top);

    JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT) {
      private boolean inited = false;

      @Override
      public void addNotify() {
        super.addNotify();
        if (!inited) {
          inited = true;

          setDividerLocation(topHeightPx);

          if (top instanceof JSplitPane topSplit) {
            topSplit.setDividerLocation(topHorizontalDividerRatio);
          }
        }
      }
    };

    sp.setOneTouchExpandable(true);
    sp.setDividerSize(10);
    sp.setContinuousLayout(true);

    sp.setResizeWeight(0.0);

    sp.setTopComponent(shrinkableTop);
    sp.setBottomComponent(bottom);

    return sp;
  }

  private static JComponent shrinkable(JComponent inner) {
    JPanel p = new JPanel(new BorderLayout()) {
      @Override
      public Dimension getMinimumSize() {
        return new Dimension(0, 0);
      }
    };
    p.add(inner, BorderLayout.CENTER);
    return p;
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