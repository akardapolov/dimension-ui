package ru.dimension.ui.config.prototype.search;

import dagger.Subcomponent;
import ru.dimension.ui.config.prototype.WorkspaceChartScope;
import ru.dimension.ui.view.chart.stacked.StackChartPanel;

@WorkspaceChartScope
@Subcomponent(modules = WorkspaceSearchModule.class)
public interface WorkspaceSearchComponent {

  void inject(StackChartPanel stackChartPanel);
}