package ru.dimension.ui.component.module.preview;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.preview.config.PreviewConfigPresenter;
import ru.dimension.ui.component.module.preview.config.PreviewConfigView;

@Log4j2
@Getter
public class PreviewConfigModule {
  private final PreviewConfigView view;
  private final PreviewConfigPresenter presenter;

  public PreviewConfigModule(MessageBroker.Component component) {
    this.view = new PreviewConfigView();
    this.presenter = new PreviewConfigPresenter(component, view);
  }
}
