package ru.dimension.ui.component.module.adhoc;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.module.adhoc.charts.AdHocChartsModel;
import ru.dimension.ui.component.module.adhoc.charts.AdHocChartsPresenter;
import ru.dimension.ui.component.module.adhoc.charts.AdHocChartsView;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;

@Log4j2
public class AdHocChartsModule {

  private final AdHocChartsModel model;
  @Getter
  private final AdHocChartsView view;
  @Getter
  private final AdHocChartsPresenter presenter;

  private final MessageBroker broker = MessageBroker.getInstance();

  public AdHocChartsModule(ProfileManager profileManager,
                           ConnectionPoolManager connectionPoolManager,
                           AdHocDatabaseManager adHocDatabaseManager) {

    this.model = new AdHocChartsModel(profileManager, connectionPoolManager, adHocDatabaseManager);
    this.view = new AdHocChartsView();
    this.presenter = new AdHocChartsPresenter(model, view);

    broker.addReceiver(Destination.withDefault(Component.ADHOC, Module.CHARTS), presenter);
  }
}