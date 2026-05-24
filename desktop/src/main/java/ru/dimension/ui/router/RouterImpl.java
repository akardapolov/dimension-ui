package ru.dimension.ui.router;

import java.util.concurrent.ScheduledExecutorService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.view.ConfigState;
import ru.dimension.ui.model.view.ProgressbarState;
import ru.dimension.ui.model.view.TemplateState;
import ru.dimension.ui.model.view.ToolbarButtonState;
import ru.dimension.ui.router.event.EventDispatcher;

@Log4j2
@Singleton
public class RouterImpl implements Router {

  private final ScheduledExecutorService executorService;
  private final EventDispatcher eventDispatcher;

  @Inject
  public RouterImpl(@Named("executorService") ScheduledExecutorService executorService,
                    @Named("eventListener") EventDispatcher eventDispatcher) {

    this.executorService = executorService;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  public void runConfigDialog(int profileId) {
    log.info("Run configuration dialog..");

    executorService.submit(() -> {
      eventDispatcher.fireToolbarButtonStateChange(ToolbarButtonState.DISABLE);
      eventDispatcher.fireProgressbarVisible(ProgressbarState.SHOW);

      try {
        eventDispatcher.fireShowConfig(ConfigState.SHOW);
      } finally {
        eventDispatcher.fireProgressbarVisible(ProgressbarState.HIDE);
      }

      eventDispatcher.fireToolbarButtonStateChange(ToolbarButtonState.ENABLE);
    });
  }

  @Override
  public void runTemplateDialog() {
    log.info("Run template dialog..");
    eventDispatcher.fireToolbarButtonStateChange(ToolbarButtonState.DISABLE);
    eventDispatcher.fireProgressbarVisible(ProgressbarState.SHOW);

    try {
      eventDispatcher.fireShowTemplate(TemplateState.SHOW);
    } finally {
      eventDispatcher.fireProgressbarVisible(ProgressbarState.HIDE);
    }

    eventDispatcher.fireToolbarButtonStateChange(ToolbarButtonState.ENABLE);
  }

  @Override
  public void runReportDialog() {
    log.info("Run report dialog..");
    executorService.submit(() -> {
      eventDispatcher.fireToolbarButtonStateChange(ToolbarButtonState.DISABLE);
      eventDispatcher.fireProgressbarVisible(ProgressbarState.SHOW);

      try {
        // Report dialog logic
      } finally {
        eventDispatcher.fireProgressbarVisible(ProgressbarState.HIDE);
      }
      eventDispatcher.fireToolbarButtonStateChange(ToolbarButtonState.ENABLE);
    });
  }
}
