package ru.dimension.ui.component;

import javax.swing.JSplitPane;
import lombok.Getter;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.module.adhoc.AdHocChartsModule;
import ru.dimension.ui.component.module.adhoc.AdHocConfigModule;
import ru.dimension.ui.component.module.adhoc.AdHocModelModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.router.event.EventListener;

public class AdHocComponent {
  @Getter
  private JSplitPane mainSplitPane;

  private final ProfileManager profileManager;
  private final ConfigurationManager configurationManager;
  private final EventListener eventListener;
  private final ConnectionPoolManager connectionPoolManager;
  private final AdHocDatabaseManager adHocDatabaseManager;

  private AdHocModelModule adHocModelModule;
  private AdHocConfigModule adHocConfigModule;
  private AdHocChartsModule adHocChartsModule;

  private final MessageBroker broker = MessageBroker.getInstance();

  public AdHocComponent(ProfileManager profileManager,
                        ConfigurationManager configurationManager,
                        EventListener eventListener,
                        ConnectionPoolManager connectionPoolManager,
                        AdHocDatabaseManager adHocDatabaseManager) {

    this.profileManager = profileManager;
    this.configurationManager = configurationManager;
    this.eventListener = eventListener;
    this.connectionPoolManager = connectionPoolManager;
    this.adHocDatabaseManager = adHocDatabaseManager;

    initializeComponents();
  }


  private void initializeComponents() {
    mainSplitPane = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 240);

    JSplitPane verticalSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 60);
    verticalSplitPane.setDividerLocation(60);
    verticalSplitPane.setResizeWeight(0.5);

    this.adHocModelModule = new AdHocModelModule(profileManager, configurationManager, eventListener,
                                                 connectionPoolManager, adHocDatabaseManager);

    this.adHocConfigModule = new AdHocConfigModule();
    this.adHocChartsModule = new AdHocChartsModule(profileManager,
                                                   connectionPoolManager, adHocDatabaseManager);

    verticalSplitPane.setTopComponent(adHocConfigModule.getView());
    verticalSplitPane.setBottomComponent(adHocChartsModule.getView());

    mainSplitPane.setLeftComponent(adHocModelModule.getView());
    mainSplitPane.setRightComponent(verticalSplitPane);

    broker.addReceiver(Destination.withDefault(Component.ADHOC, Module.MODEL), adHocModelModule);
  }
}