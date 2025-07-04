package ru.dimension.ui.config.prototype.detail;

import dagger.Subcomponent;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;
import ru.dimension.ui.view.analyze.panel.AnalyzeHistoryPanel;

@WorkspaceDetailScope
@Subcomponent(modules = WorkspaceAnalyzeHistoryModule.class)
public interface WorkspaceAnalyzeHistoryComponent {

  void inject(AnalyzeHistoryPanel analyzeHistoryPanel);
}