package ru.dimension.ui.config;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.ui.collector.Collector;
import ru.dimension.ui.collector.CollectorImpl;

@Module
public abstract class CollectorConfig {

  @Binds
  @Named("collector")
  public abstract Collector bindCollector(CollectorImpl router);
}
