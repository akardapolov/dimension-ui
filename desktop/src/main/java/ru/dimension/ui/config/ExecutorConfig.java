package ru.dimension.ui.config;

import dagger.Module;
import dagger.Provides;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Named;
import javax.inject.Singleton;
import ru.dimension.ui.executor.TaskExecutorPool;

@Module
public class ExecutorConfig {

  @Provides
  @Singleton
  @Named("executorService")
  public ScheduledExecutorService getScheduledExecutorService() {
    return Executors.newScheduledThreadPool(10);
  }

  @Provides
  @Singleton
  @Named("taskExecutorPool")
  public TaskExecutorPool getTaskExecutorPool() {
    return new TaskExecutorPool();
  }
}
