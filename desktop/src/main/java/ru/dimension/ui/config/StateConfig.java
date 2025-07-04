package ru.dimension.ui.config;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.ui.state.NavigatorState;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.impl.NavigatorStateImpl;
import ru.dimension.ui.state.impl.SqlQueryStateImpl;

@Module
public abstract class StateConfig {

  @Binds
  @Named("navigatorState")
  public abstract NavigatorState bindNavigatorState(NavigatorStateImpl navigatorState);

  @Binds
  @Named("sqlQueryState")
  public abstract SqlQueryState bindSqlQueryState(SqlQueryStateImpl sqlQueryState);
}
