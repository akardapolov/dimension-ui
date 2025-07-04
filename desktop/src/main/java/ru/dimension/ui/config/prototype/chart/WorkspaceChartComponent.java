package ru.dimension.ui.config.prototype.chart;

import dagger.Subcomponent;
import ru.dimension.ui.config.prototype.WorkspaceChartScope;
import ru.dimension.ui.view.chart.search.SearchStackChartPanel;
import ru.dimension.ui.view.chart.stacked.StackChartPanel;

@WorkspaceChartScope
@Subcomponent(modules = WorkspaceChartModule.class)
public interface WorkspaceChartComponent {

  void inject(StackChartPanel stackChartPanel);

  void inject(SearchStackChartPanel searchStackChartPanel);
}