package ru.dimension.ui.config.prototype.task;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import ru.dimension.ui.config.prototype.WorkspaceTaskScope;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.structure.workspace.task.WorkspaceTaskView;

@Module
public class WorkspaceTaskModule {

  private final WorkspaceTaskView workspaceTaskView;

  public WorkspaceTaskModule(WorkspaceTaskView workspaceTaskView) {
    this.workspaceTaskView = workspaceTaskView;
  }

  @WorkspaceTaskScope
  @Provides
  public EventListener provideEventListener(@Named("eventListener") EventListener eventListener) {
    return eventListener;
  }

  @WorkspaceTaskScope
  @Provides
  public ProfileManager provideProfileManager(@Named("profileManager") ProfileManager profileManager) {
    return profileManager;
  }
}
