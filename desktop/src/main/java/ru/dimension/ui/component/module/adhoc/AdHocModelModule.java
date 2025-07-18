package ru.dimension.ui.component.module.adhoc;

import lombok.Getter;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.module.adhoc.model.AdHocModel;
import ru.dimension.ui.component.module.adhoc.model.AdHocPresenter;
import ru.dimension.ui.component.module.adhoc.model.AdHocView;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.router.event.EventListener;

public class AdHocModelModule implements MessageAction {

  private final AdHocModel model;
  @Getter
  private final AdHocView view;
  private final AdHocPresenter presenter;

  public AdHocModelModule(ProfileManager profileManager,
                          ConfigurationManager configurationManager,
                          EventListener eventListener,
                          ConnectionPoolManager connectionPoolManager,
                          AdHocDatabaseManager adHocDatabaseManager) {

    this.model = new AdHocModel(profileManager, configurationManager, eventListener, connectionPoolManager, adHocDatabaseManager);
    this.view = new AdHocView();
    this.presenter = new AdHocPresenter(model, view);

    this.presenter.loadConnections();
  }

  @Override
  public void receive(Message message) {

  }
}
