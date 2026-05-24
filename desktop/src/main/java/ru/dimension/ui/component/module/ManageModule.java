package ru.dimension.ui.component.module;

import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.di.Assisted;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.module.factory.ManageModulePresenterFactory;
import ru.dimension.ui.component.module.manage.ManagePresenter;
import ru.dimension.ui.component.module.manage.ManageView;

@Log4j2
public class ManageModule implements MessageAction {

  @Getter
  private final ManageView view;
  private final ManageModulePresenterFactory manageModulePresenterFactory;
  private ManagePresenter presenter;

  private final MessageBroker broker = MessageBroker.getInstance();

  @Inject
  public ManageModule(@Assisted Component component,
                      ManageModulePresenterFactory manageModulePresenterFactory) {

    this.view = new ManageView();

    this.manageModulePresenterFactory = manageModulePresenterFactory;
    this.presenter = manageModulePresenterFactory.create(component, view);

    broker.addReceiver(Destination.withDefault(component, Module.MANAGE), presenter);
  }

  @Override
  public void receive(Message message) {

  }
}
