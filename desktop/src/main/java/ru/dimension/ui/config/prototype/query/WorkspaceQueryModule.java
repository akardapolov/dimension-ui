package ru.dimension.ui.config.prototype.query;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.view.structure.workspace.query.WorkspaceQueryView;
import ru.dimension.ui.cache.AppCache;
import ru.dimension.ui.config.prototype.WorkspaceQueryScope;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.router.event.EventListener;

@Module
public class WorkspaceQueryModule {

  private final WorkspaceQueryView workspaceQueryView;

  public WorkspaceQueryModule(WorkspaceQueryView workspaceQueryView) {
    this.workspaceQueryView = workspaceQueryView;
  }

  @WorkspaceQueryScope
  @Provides
  public EventListener provideEventListener(@Named("eventListener") EventListener eventListener) {
    return eventListener;
  }

  @WorkspaceQueryScope
  @Provides
  public ProfileManager provideProfileManager(@Named("profileManager") ProfileManager profileManager) {
    return profileManager;
  }

  @WorkspaceQueryScope
  @Provides
  public ConfigurationManager provideConfigurationManager(@Named("configurationManager") ConfigurationManager configurationManager) {
    return configurationManager;
  }

  @WorkspaceQueryScope
  @Provides
  public AppCache provideAppCache(@Named("appCache") AppCache appCache) {
    return appCache;
  }

  @WorkspaceQueryScope
  @Provides
  public DStore provideDStore(@Named("localDB") DStore dStore) {
    return dStore;
  }
}
