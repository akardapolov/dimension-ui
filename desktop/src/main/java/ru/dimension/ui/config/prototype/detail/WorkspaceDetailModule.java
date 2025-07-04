package ru.dimension.ui.config.prototype.detail;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.detail.DetailPanel;

@Module
public class WorkspaceDetailModule {

  private final DetailPanel detailPanel;

  public WorkspaceDetailModule(DetailPanel detailPanel) {
    this.detailPanel = detailPanel;
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

}
