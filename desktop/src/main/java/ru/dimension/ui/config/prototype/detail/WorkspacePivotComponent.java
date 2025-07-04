package ru.dimension.ui.config.prototype.detail;

import dagger.Subcomponent;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;
import ru.dimension.ui.view.detail.pivot.MainPivotPanel;

@WorkspaceDetailScope
@Subcomponent(modules = WorkspacePivotModule.class)
public interface WorkspacePivotComponent {

  void inject(MainPivotPanel mainPivotPanel);
}