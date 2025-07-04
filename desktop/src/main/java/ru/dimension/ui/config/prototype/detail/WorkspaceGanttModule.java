package ru.dimension.ui.config.prototype.detail;

import dagger.Module;
import dagger.Provides;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Named;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.detail.top.MainTopPanel;

@Module
public class WorkspaceGanttModule {

  private final MainTopPanel mainTopPanel;

  public WorkspaceGanttModule(MainTopPanel mainTopPanel) {
    this.mainTopPanel = mainTopPanel;
  }

  @WorkspaceDetailScope
  @Provides
  public EventListener provideEventListener(@Named("eventListener") EventListener eventListener) {
    return eventListener;
  }

  @WorkspaceDetailScope
  @Provides
  public DStore provideDStore(@Named("localDB") DStore dStore) {
    return dStore;
  }

  @WorkspaceDetailScope
  @Provides
  public ScheduledExecutorService getScheduledExecutorService(@Named("executorService") ScheduledExecutorService executorService) {
    return executorService;
  }

}
