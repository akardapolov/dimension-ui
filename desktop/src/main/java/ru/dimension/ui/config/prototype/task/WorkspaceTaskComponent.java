package ru.dimension.ui.config.prototype.task;

import dagger.Subcomponent;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryModule;
import ru.dimension.ui.config.prototype.WorkspaceTaskScope;
import ru.dimension.ui.view.structure.workspace.task.WorkspaceTaskView;

@WorkspaceTaskScope
@Subcomponent(modules = WorkspaceTaskModule.class)
public interface WorkspaceTaskComponent {

  void inject(WorkspaceTaskView workspaceTaskView);

  WorkspaceQueryComponent initQuery(WorkspaceQueryModule workspaceQueryModule);
}