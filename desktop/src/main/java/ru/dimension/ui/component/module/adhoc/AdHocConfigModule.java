package ru.dimension.ui.component.module.adhoc;

import lombok.Getter;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.module.adhoc.config.AdHocConfigPresenter;
import ru.dimension.ui.component.module.adhoc.config.AdHocConfigView;

public class AdHocConfigModule implements MessageAction {

  @Getter
  private final AdHocConfigView view;
  private final AdHocConfigPresenter presenter;

  private final MessageBroker broker = MessageBroker.getInstance();

  public AdHocConfigModule() {
    view = new AdHocConfigView();
    presenter = new AdHocConfigPresenter(view);

    broker.addReceiver(Destination.withDefault(Component.ADHOC, Module.CONFIG), presenter);
  }

  @Override
  public void receive(Message message) {}
}
