package ru.dimension.ui.config.prototype.detail;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.view.detail.raw.RawDataPanel;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;
import ru.dimension.ui.router.event.EventListener;

@Module
public class WorkspaceRawModule {

  private final RawDataPanel rawDataPanel;

  public WorkspaceRawModule(RawDataPanel rawDataPanel) {
    this.rawDataPanel = rawDataPanel;
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
