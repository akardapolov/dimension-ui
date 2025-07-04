package ru.dimension.ui.config.prototype.detail;

import dagger.Subcomponent;
import ru.dimension.ui.view.detail.raw.RawDataPanel;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;

@WorkspaceDetailScope
@Subcomponent(modules = WorkspaceRawModule.class)
public interface WorkspaceRawComponent {

  void inject(RawDataPanel detailPanel);
}