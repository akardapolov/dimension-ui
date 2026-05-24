package ru.dimension.ui.view.structure.progressbar;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.view.ProgressbarState;
import ru.dimension.ui.router.event.EventDispatcher;
import ru.dimension.ui.router.listener.ProgressbarListener;
import ru.dimension.ui.view.structure.ProgressbarView;

@Log4j2
@Singleton
public class ProgressbarPresenter implements ProgressbarListener {

  private final ProgressbarView progressbarView;
  private final EventDispatcher eventDispatcher;

  @Inject
  public ProgressbarPresenter(@Named("progressbarView") ProgressbarView progressbarView,
                              @Named("eventListener") EventDispatcher eventDispatcher) {
    this.progressbarView = progressbarView;
    this.eventDispatcher = eventDispatcher;

    this.eventDispatcher.addProgressbarListener(this);
  }

  @Override
  public void fireProgressbarVisible(ProgressbarState progressbarState) {
    this.progressbarView.setProgressbarVisible(progressbarState);
  }
}
