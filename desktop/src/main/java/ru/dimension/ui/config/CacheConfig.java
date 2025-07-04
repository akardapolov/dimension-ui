package ru.dimension.ui.config;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.ui.cache.AppCache;
import ru.dimension.ui.cache.impl.AppCacheImpl;

@Module
public abstract class CacheConfig {

  @Binds
  @Named("appCache")
  public abstract AppCache bindAppCache(AppCacheImpl appCache);
}
