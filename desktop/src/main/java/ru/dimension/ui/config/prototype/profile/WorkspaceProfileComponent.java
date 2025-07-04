package ru.dimension.ui.config.prototype.profile;

import dagger.Subcomponent;
import ru.dimension.ui.config.prototype.task.WorkspaceTaskComponent;
import ru.dimension.ui.config.prototype.task.WorkspaceTaskModule;
import ru.dimension.ui.config.prototype.WorkspaceProfileScope;
import ru.dimension.ui.view.structure.workspace.profile.WorkspaceProfileView;

@WorkspaceProfileScope
@Subcomponent(modules = WorkspaceProfileModule.class)
public interface WorkspaceProfileComponent {

  void inject(WorkspaceProfileView workspaceProfileView);

  WorkspaceTaskComponent initTask(WorkspaceTaskModule workspaceTaskModule);
}