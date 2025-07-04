package ru.dimension.ui.config.prototype.detail;

import dagger.Subcomponent;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;
import ru.dimension.ui.view.detail.DetailPanel;

@WorkspaceDetailScope
@Subcomponent(modules = WorkspaceDetailModule.class)
public interface WorkspaceDetailComponent {

  void inject(DetailPanel detailPanel);
}