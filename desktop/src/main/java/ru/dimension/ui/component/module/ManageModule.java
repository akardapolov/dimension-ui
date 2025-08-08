package ru.dimension.ui.component.module;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.collector.Collector;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.module.manage.ManagePresenter;
import ru.dimension.ui.component.module.manage.ManageView;
import ru.dimension.ui.executor.TaskExecutorPool;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class ManageModule implements MessageAction {

  @Getter
  private final ManageView view;
  private final ManagePresenter presenter;

  private final EventListener eventListener;
  private final ProfileManager profileManager;
  private final TaskExecutorPool taskExecutorPool;
  private final ConnectionPoolManager connectionPoolManager;
  private final HttpResponseFetcher httpResponseFetcher;
  private final SqlQueryState sqlQueryState;
  private final Collector collector;
  private final DStore dStore;

  private final MessageBroker broker = MessageBroker.getInstance();

  public ManageModule(Component component,
                      EventListener eventListener,
                      ProfileManager profileManager,
                      TaskExecutorPool taskExecutorPool,
                      ConnectionPoolManager connectionPoolManager,
                      HttpResponseFetcher httpResponseFetcher,
                      SqlQueryState sqlQueryState,
                      Collector collector,
                      DStore dStore) {
    this.eventListener = eventListener;
    this.profileManager = profileManager;
    this.taskExecutorPool = taskExecutorPool;
    this.connectionPoolManager = connectionPoolManager;
    this.httpResponseFetcher = httpResponseFetcher;
    this.sqlQueryState = sqlQueryState;
    this.collector = collector;
    this.dStore = dStore;

    this.view = new ManageView();
    this.presenter = new ManagePresenter(component, view,
                                         eventListener, profileManager,
                                         taskExecutorPool, connectionPoolManager, httpResponseFetcher, sqlQueryState,
                                         collector, dStore);

    broker.addReceiver(Destination.withDefault(component, Module.MANAGE), presenter);
  }

  @Override
  public void receive(Message message) {

  }
}
