package ru.dimension.ui.config.prototype.chart;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.config.prototype.WorkspaceChartScope;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.chart.search.SearchStackChartPanel;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.view.chart.stacked.StackChartPanel;

@Module
public class WorkspaceChartModule {

  private final StackChartPanel stackChartPanel;
  private final SearchStackChartPanel searchStackChartPanel;

  public WorkspaceChartModule(StackChartPanel stackChartPanel) {
    this.stackChartPanel = stackChartPanel;
    this.searchStackChartPanel = null;
  }

  public WorkspaceChartModule(SearchStackChartPanel searchStackChartPanel) {
    this.stackChartPanel = null;
    this.searchStackChartPanel = searchStackChartPanel;
  }

  @WorkspaceChartScope
  @Provides
  public EventListener provideEventListener(@Named("eventListener") EventListener eventListener) {
    return eventListener;
  }

  @WorkspaceChartScope
  @Provides
  public DStore provideDStore(@Named("localDB") DStore dStore) {
    return dStore;
  }

  @WorkspaceChartScope
  @Provides
  public SqlQueryState provideSqlQueryState(@Named("sqlQueryState") SqlQueryState sqlQueryState) {
    return sqlQueryState;
  }

  @WorkspaceChartScope
  @Provides
  public ProfileManager provideProfileManager(@Named("profileManager") ProfileManager profileManager) {
    return profileManager;
  }
}
