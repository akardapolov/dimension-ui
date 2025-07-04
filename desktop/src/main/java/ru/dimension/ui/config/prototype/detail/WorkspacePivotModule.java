package ru.dimension.ui.config.prototype.detail;

import dagger.Module;
import dagger.Provides;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Named;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.detail.pivot.MainPivotPanel;

@Module
public class WorkspacePivotModule {

  private final MainPivotPanel mainPivotPanel;

  public WorkspacePivotModule(MainPivotPanel mainPivotPanel) {
    this.mainPivotPanel = mainPivotPanel;
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
