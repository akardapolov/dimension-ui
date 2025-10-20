package ru.dimension.ui.executor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.ProfileTaskQueryKey;

@Log4j2
@Singleton
public class TaskExecutorPool {

  private final Map<ProfileTaskQueryKey, TaskExecutor> taskExecutorMap = new ConcurrentHashMap<>();

  @Inject
  public TaskExecutorPool() {
  }

  public void add(ProfileTaskQueryKey profileTaskQueryKey,
                  TaskExecutor taskExecutor) {
    taskExecutorMap.put(profileTaskQueryKey, taskExecutor);
  }

  public TaskExecutor get(ProfileTaskQueryKey profileTaskQueryKey) {
    return taskExecutorMap.get(profileTaskQueryKey);
  }

  public void stop(ProfileTaskQueryKey profileTaskQueryKey) {
    TaskExecutor executor = taskExecutorMap.get(profileTaskQueryKey);
    if (executor != null) {
      executor.stop();
      log.info("Stopped Task Executor for {}", profileTaskQueryKey);
    } else {
      log.warn("Not found running Task Executor for {}", profileTaskQueryKey);
      log.debug("Available executors: {}", taskExecutorMap.keySet());
    }
  }

  public void remove(ProfileTaskQueryKey profileTaskQueryKey) {
    taskExecutorMap.remove(profileTaskQueryKey);
  }
}
