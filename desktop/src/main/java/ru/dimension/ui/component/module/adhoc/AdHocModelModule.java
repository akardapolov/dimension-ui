package ru.dimension.ui.component.module.adhoc;

import jakarta.inject.Inject;
import lombok.Getter;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.ConnectionAddEvent;
import ru.dimension.ui.bus.event.ConnectionRemoveEvent;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.module.adhoc.model.AdHocModelModel;
import ru.dimension.ui.component.module.adhoc.model.AdHocModelPresenter;
import ru.dimension.ui.component.module.adhoc.model.AdHocModelView;
import ru.dimension.ui.helper.event.EventRouteRegistry;
import ru.dimension.ui.helper.event.EventUtils;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.type.ConnectionStatus;
import ru.dimension.ui.router.event.EventListener;

public class AdHocModelModule implements MessageAction {

  private final AdHocModelModel model;
  @Getter
  private final AdHocModelView view;
  private final AdHocModelPresenter presenter;
  private final EventBus eventBus;

  private final EventRouteRegistry eventRegistry;

  private final MessageBroker broker = MessageBroker.getInstance();

  @Inject
  public AdHocModelModule(ProfileManager profileManager,
                          ConfigurationManager configurationManager,
                          EventListener eventListener,
                          ConnectionPoolManager connectionPoolManager,
                          AdHocDatabaseManager adHocDatabaseManager,
                          EventBus eventBus) {

    this.model = new AdHocModelModel(profileManager, configurationManager, eventListener, connectionPoolManager, adHocDatabaseManager);
    this.view = new AdHocModelView();
    this.presenter = new AdHocModelPresenter(model, view);
    this.eventBus = eventBus;

    this.eventRegistry = EventRouteRegistry.forComponent(Component.ADHOC, EventUtils::getComponent)
        .routeGlobal(ConnectionAddEvent.class, this::onConnectionAdd)
        .routeGlobal(ConnectionRemoveEvent.class, this::onConnectionRemove)
        .register(eventBus);

    this.presenter.loadConnections();

    broker.addReceiver(Destination.withDefault(Component.ADHOC, Module.MODEL), this);
  }

  @Override
  public void receive(Message message) {
    switch (message.action()) {
      case CLEAR_SELECTION_FOR_TABLE_OR_VIEW -> presenter.clearSelectionForTableOrView(message);
    }
  }

  public void onConnectionAdd(ConnectionAddEvent event) {
    presenter.addConnection(event.connectionId(), event.connectionName(), event.type());
  }

  public void onConnectionRemove(ConnectionRemoveEvent event) {
    presenter.removeConnection(event.connectionId());
  }

  public void updateConnectionStatus(int connectionId, ConnectionStatus status) {
    presenter.updateConnectionStatus(connectionId, status);
  }

  public void recheckConnection(int connectionId) {
    presenter.recheckConnection(connectionId);
  }

  public void shutdown() {
    presenter.shutdown();
  }
}