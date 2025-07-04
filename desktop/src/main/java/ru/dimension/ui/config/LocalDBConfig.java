package ru.dimension.ui.config;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.warehouse.LocalDB;

@Module
public abstract class LocalDBConfig {

  @Binds
  @Named("localDB")
  public abstract DStore bindDStore(LocalDB localDB);
}
