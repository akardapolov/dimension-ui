package ru.dimension.ui.component.module;

import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.di.Assisted;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageAction;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.config.ConfigPresenter;
import ru.dimension.ui.component.module.config.ConfigView;

@Log4j2
public class ConfigModule implements MessageAction {

  @Getter
  private final ConfigView view;
  @Getter
  private final ConfigPresenter presenter;

  @Inject
  public ConfigModule(@Assisted MessageBroker.Component component) {
    this.view = new ConfigView();
    this.presenter = new ConfigPresenter(component, view);
  }

  @Override
  public void receive(Message message) {}
}