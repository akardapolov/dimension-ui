package ru.dimension.ui.config.prototype.detail;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.config.prototype.WorkspaceDetailScope;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.analyze.panel.AnalyzeRealtimePanel;

@Module
public class WorkspaceAnalyzeRealTimeModule {

  private final AnalyzeRealtimePanel analyzeRealtimePanel;

  public WorkspaceAnalyzeRealTimeModule(AnalyzeRealtimePanel analyzeRealtimePanel) {
    this.analyzeRealtimePanel = analyzeRealtimePanel;
  }

  @WorkspaceDetailScope
  @Provides
  public SqlQueryState provideSqlQueryState(@Named("sqlQueryState") SqlQueryState sqlQueryState) {
    return sqlQueryState;
  }

  @WorkspaceDetailScope
  @Provides
  public EventListener provideEventListener(@Named("eventListener") EventListener eventListener) {
    return eventListener;
  }

  @WorkspaceDetailScope
  @Provides
  public DStore provideDStore(@Named("localDB") DStore dStore) {
    return dStore;
  }
}
