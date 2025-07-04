package ru.dimension.ui.config.prototype.detail;

import dagger.Subcomponent;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;
import ru.dimension.ui.view.detail.top.MainTopPanel;

@WorkspaceDetailScope
@Subcomponent(modules = WorkspaceGanttModule.class)
public interface WorkspaceGanttComponent {

  void inject(MainTopPanel mainTopPanel);
}