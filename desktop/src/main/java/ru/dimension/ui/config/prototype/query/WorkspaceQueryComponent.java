package ru.dimension.ui.config.prototype.query;

import dagger.Subcomponent;
import ru.dimension.ui.view.structure.workspace.query.WorkspaceQueryView;
import ru.dimension.ui.config.prototype.WorkspaceQueryScope;
import ru.dimension.ui.config.prototype.chart.WorkspaceChartComponent;
import ru.dimension.ui.config.prototype.chart.WorkspaceChartModule;
import ru.dimension.ui.config.prototype.detail.WorkspaceAnalyzeHistoryComponent;
import ru.dimension.ui.config.prototype.detail.WorkspaceAnalyzeHistoryModule;
import ru.dimension.ui.config.prototype.detail.WorkspaceAnalyzeRealTimeComponent;
import ru.dimension.ui.config.prototype.detail.WorkspaceAnalyzeRealTimeModule;
import ru.dimension.ui.config.prototype.detail.WorkspaceDetailComponent;
import ru.dimension.ui.config.prototype.detail.WorkspaceDetailModule;
import ru.dimension.ui.config.prototype.detail.WorkspaceGanttComponent;
import ru.dimension.ui.config.prototype.detail.WorkspaceGanttModule;
import ru.dimension.ui.config.prototype.detail.WorkspacePivotComponent;
import ru.dimension.ui.config.prototype.detail.WorkspacePivotModule;
import ru.dimension.ui.config.prototype.detail.WorkspaceRawComponent;
import ru.dimension.ui.config.prototype.detail.WorkspaceRawModule;
import ru.dimension.ui.view.structure.workspace.handler.MetricColumnSelectionHandler;
import ru.dimension.ui.view.structure.workspace.handler.QuerySearchHandler;
import ru.dimension.ui.view.structure.workspace.handler.TimeRangeAbsoluteHandler;
import ru.dimension.ui.view.structure.workspace.handler.TimeRangeQuickHandler;
import ru.dimension.ui.view.structure.workspace.handler.TimeRangeRecentHandler;
import ru.dimension.ui.view.structure.workspace.handler.TimeRangeRelativeHandler;

@WorkspaceQueryScope
@Subcomponent(modules = WorkspaceQueryModule.class)
public interface WorkspaceQueryComponent {

  void inject(WorkspaceQueryView workspaceQueryView);

  void injectMetricColumnSelectionHandler(MetricColumnSelectionHandler metricColumnSelectionHandler);

  void injectTimeRangeQuickHandler(TimeRangeQuickHandler timeRangeQuickHandler);

  void injectTimeRangeRelativeHandler(TimeRangeRelativeHandler timeRangeRelativeHandler);

  void injectTimeRangeAbsoluteHandler(TimeRangeAbsoluteHandler timeRangeAbsoluteHandler);

  void injectTimeRangeRecentHandler(TimeRangeRecentHandler timeRangeRecentHandler);

  void injectSearchHandler(QuerySearchHandler querySearchHandler);

  WorkspaceChartComponent initChart(WorkspaceChartModule workspaceChartModule);

  WorkspaceDetailComponent initDetail(WorkspaceDetailModule workspaceDetailModule);

  WorkspaceRawComponent initRaw(WorkspaceRawModule workspaceRawModule);

  WorkspaceGanttComponent initGantt(WorkspaceGanttModule workspaceGanttModule);

  WorkspacePivotComponent initPivot(WorkspacePivotModule workspacePivotModule);

  WorkspaceAnalyzeHistoryComponent initAnalyzeHistory(
      WorkspaceAnalyzeHistoryModule workspaceAnalyzeHistoryModule);

  WorkspaceAnalyzeRealTimeComponent initAnalyzeRealTime(
      WorkspaceAnalyzeRealTimeModule workspaceAnalyzeHistoryDimensionModule);
}