package ru.dimension.ui.component.module;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.config.ConfigPresenter;
import ru.dimension.ui.component.module.config.ConfigView;

@Log4j2
public class ConfigureModule implements MessageAction {

  @Getter
  private final ConfigView view;
  private final ConfigPresenter presenter;

  public ConfigureModule(MessageBroker.Component component) {
    this.view = new ConfigView();
    this.presenter = new ConfigPresenter(component, view);
  }

  @Override
  public void receive(Message message) {}
}
