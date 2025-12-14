package ru.dimension.ui.component;

import jakarta.inject.Inject;
import javax.swing.JSplitPane;
import lombok.Getter;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.module.adhoc.AdHocChartsModule;
import ru.dimension.ui.component.module.adhoc.AdHocConfigModule;
import ru.dimension.ui.component.module.adhoc.AdHocModelModule;
import ru.dimension.ui.component.module.factory.AdHocChartsModuleFactory;
import ru.dimension.ui.component.module.factory.AdHocConfigModuleFactory;
import ru.dimension.ui.component.module.factory.AdHocModelModuleFactory;
import ru.dimension.ui.helper.GUIHelper;

public class AdHocComponent {
  @Getter
  private JSplitPane mainSplitPane;

  private final AdHocModelModuleFactory adHocModelModuleFactory;
  private final AdHocConfigModuleFactory adHocConfigModuleFactory;
  private final AdHocChartsModuleFactory adHocChartsModuleFactory;

  private AdHocModelModule adHocModelModule;
  private AdHocConfigModule adHocConfigModule;
  private AdHocChartsModule adHocChartsModule;

  private final MessageBroker broker = MessageBroker.getInstance();

  @Inject
  public AdHocComponent(AdHocModelModuleFactory adHocModelModuleFactory,
                        AdHocConfigModuleFactory adHocConfigModuleFactory,
                        AdHocChartsModuleFactory adHocChartsModuleFactory) {
    this.adHocModelModuleFactory = adHocModelModuleFactory;
    this.adHocConfigModuleFactory = adHocConfigModuleFactory;
    this.adHocChartsModuleFactory = adHocChartsModuleFactory;

    initializeComponents();
  }

  private void initializeComponents() {
    mainSplitPane = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 240);

    JSplitPane verticalSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 60);
    verticalSplitPane.setDividerLocation(60);
    verticalSplitPane.setResizeWeight(0.5);

    this.adHocModelModule = adHocModelModuleFactory.create();
    this.adHocConfigModule = adHocConfigModuleFactory.create();
    this.adHocChartsModule = adHocChartsModuleFactory.create();

    verticalSplitPane.setTopComponent(adHocConfigModule.getView());
    verticalSplitPane.setBottomComponent(adHocChartsModule.getView());

    mainSplitPane.setLeftComponent(adHocModelModule.getView());
    mainSplitPane.setRightComponent(verticalSplitPane);

    broker.addReceiver(Destination.withDefault(Component.ADHOC, Module.MODEL), adHocModelModule);
  }
}