package ru.dimension.ui.router;

import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.view.ConfigState;
import ru.dimension.ui.model.view.ProgressbarState;
import ru.dimension.ui.model.view.ReportState;
import ru.dimension.ui.model.view.TemplateState;
import ru.dimension.ui.model.view.ToolbarButtonState;
import ru.dimension.ui.router.event.EventListener;

@Log4j2
@Singleton
public class RouterImpl implements Router {

  private final ScheduledExecutorService executorService;
  private final EventListener eventListener;

  @Inject
  public RouterImpl(@Named("executorService") ScheduledExecutorService executorService,
                    @Named("eventListener") EventListener eventListener) {

    this.executorService = executorService;
    this.eventListener = eventListener;
  }

  @Override
  public void runConfigDialog(int profileId) {
    log.info("Run configuration dialog..");

    executorService.submit(() -> {
      eventListener.fireToolbarButtonStateChange(ToolbarButtonState.DISABLE);
      eventListener.fireProgressbarVisible(ProgressbarState.SHOW);

      try {
        eventListener.fireShowConfig(ConfigState.SHOW);
      } finally {
        eventListener.fireProgressbarVisible(ProgressbarState.HIDE);
      }

      eventListener.fireToolbarButtonStateChange(ToolbarButtonState.ENABLE);
    });
  }

  @Override
  public void runTemplateDialog() {
    log.info("Run template dialog..");
    eventListener.fireToolbarButtonStateChange(ToolbarButtonState.DISABLE);
    eventListener.fireProgressbarVisible(ProgressbarState.SHOW);

    try {
      eventListener.fireShowTemplate(TemplateState.SHOW);
    } finally {
      eventListener.fireProgressbarVisible(ProgressbarState.HIDE);
    }

    eventListener.fireToolbarButtonStateChange(ToolbarButtonState.ENABLE);
  }

  @Override
  public void runReportDialog() {
    log.info("Run report dialog..");
    executorService.submit(() -> {
      eventListener.fireToolbarButtonStateChange(ToolbarButtonState.DISABLE);
      eventListener.fireProgressbarVisible(ProgressbarState.SHOW);

      try {
        eventListener.fireShowReport(ReportState.SHOW);
      } finally {
        eventListener.fireProgressbarVisible(ProgressbarState.HIDE);
      }
      eventListener.fireToolbarButtonStateChange(ToolbarButtonState.ENABLE);
    });
  }

  @Override
  public void fireOnSelectProfileOnNavigator(int profileId) {
    eventListener.fireOnSelectProfileOnNavigator(profileId);
  }
}
