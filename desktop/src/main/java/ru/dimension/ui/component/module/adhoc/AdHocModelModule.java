package ru.dimension.ui.component.module.adhoc;

import lombok.Getter;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.module.adhoc.model.AdHocModelModel;
import ru.dimension.ui.component.module.adhoc.model.AdHocModelPresenter;
import ru.dimension.ui.component.module.adhoc.model.AdHocModelView;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.router.event.EventListener;

public class AdHocModelModule implements MessageAction {

  private final AdHocModelModel model;
  @Getter
  private final AdHocModelView view;
  private final AdHocModelPresenter presenter;

  public AdHocModelModule(ProfileManager profileManager,
                          ConfigurationManager configurationManager,
                          EventListener eventListener,
                          ConnectionPoolManager connectionPoolManager,
                          AdHocDatabaseManager adHocDatabaseManager) {

    this.model = new AdHocModelModel(profileManager, configurationManager, eventListener, connectionPoolManager, adHocDatabaseManager);
    this.view = new AdHocModelView();
    this.presenter = new AdHocModelPresenter(model, view);

    this.presenter.loadConnections();
  }

  @Override
  public void receive(Message message) {

  }
}
