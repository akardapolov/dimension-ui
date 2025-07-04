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
    taskExecutorMap.entrySet()
        .stream()
        .filter(f -> f.getKey().equals(profileTaskQueryKey))
        .findAny()
        .ifPresentOrElse(taskExecutorEntry -> taskExecutorEntry.getValue().stop(),
                         () -> log.info("Not found running Task Executor for {}", profileTaskQueryKey));
  }

  public void remove(ProfileTaskQueryKey profileTaskQueryKey) {
    taskExecutorMap.remove(profileTaskQueryKey);
  }
}
