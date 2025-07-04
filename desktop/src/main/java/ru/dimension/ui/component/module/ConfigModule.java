package ru.dimension.ui.component.module;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.module.config.ConfigPresenter;
import ru.dimension.ui.component.module.config.ConfigView;
import ru.dimension.ui.component.Message;
import ru.dimension.ui.component.MessageAction;

@Log4j2
public class ConfigModule implements MessageAction {

  @Getter
  private final ConfigView view;
  private final ConfigPresenter presenter;

  public ConfigModule() {
    view = new ConfigView();
    presenter = new ConfigPresenter(view);
  }

  @Override
  public void receive(Message message) {
    // Handle incoming messages if needed
  }
}
