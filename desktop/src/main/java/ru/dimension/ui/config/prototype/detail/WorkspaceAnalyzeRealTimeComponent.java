package ru.dimension.ui.config.prototype.detail;

import dagger.Subcomponent;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;
import ru.dimension.ui.view.analyze.panel.AnalyzeRealtimePanel;

@WorkspaceDetailScope
@Subcomponent(modules = WorkspaceAnalyzeRealTimeModule.class)
public interface WorkspaceAnalyzeRealTimeComponent {

  void inject(AnalyzeRealtimePanel analyzeHistoryDimensionPanel);
}