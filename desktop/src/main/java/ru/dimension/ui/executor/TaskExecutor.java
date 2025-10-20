package ru.dimension.ui.executor;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.collector.by.ByClient;
import ru.dimension.ui.collector.by.ByServer;
import ru.dimension.ui.collector.by.ByTarget;
import ru.dimension.ui.collector.collect.AbstractCollect;
import ru.dimension.ui.collector.collect.HttpCollect;
import ru.dimension.ui.collector.collect.JdbcCollect;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.model.ProfileTaskKey;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class TaskExecutor {

  private final ProfileInfo profileInfo;
  private final TaskInfo taskInfo;
  private final QueryInfo queryInfo;

  private final int pullTimeout;

  private final EventListener eventListener;

  private Connection connection = null;
  private final AbstractCollect loader;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private Thread periodicTask;
  private long lastExecutionTime;

  public TaskExecutor(ProfileInfo profileInfo,
                      TaskInfo taskInfo,
                      QueryInfo queryInfo,
                      TableInfo tableInfo,
                      ConnectionInfo connectionInfo,
                      ConnectionPoolManager connectionPoolManager,
                      SqlQueryState sqlQueryState,
                      EventListener eventListener,
                      HttpResponseFetcher httpResponseFetcher,
                      DStore dStore) {
    this.profileInfo = profileInfo;
    this.taskInfo = taskInfo;
    this.queryInfo = queryInfo;

    this.eventListener = eventListener;

    this.pullTimeout = taskInfo.getPullTimeout();

    ProfileTaskKey profileTaskKey = new ProfileTaskKey(profileInfo.getId(), taskInfo.getId());

    ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(profileInfo.getId(), taskInfo.getId(), queryInfo.getId());

    try {
      if (GatherDataMode.BY_CLIENT_JDBC.equals(queryInfo.getGatherDataMode())) {
        connection = connectionPoolManager.getConnection(connectionInfo, profileTaskKey);
        ByTarget byClient = new ByClient(profileTaskQueryKey, queryInfo, connection);
        this.loader = new JdbcCollect(byClient, connection, profileTaskQueryKey,
                                      taskInfo, queryInfo, tableInfo, sqlQueryState, dStore);
      } else if (GatherDataMode.BY_SERVER_JDBC.equals(queryInfo.getGatherDataMode())) {
        connection = connectionPoolManager.getConnection(connectionInfo, profileTaskKey);
        ByTarget byServer = new ByServer(profileTaskQueryKey, queryInfo, tableInfo, connection, sqlQueryState);
        this.loader = new JdbcCollect(byServer, connection, profileTaskQueryKey,
                                      taskInfo, queryInfo, tableInfo, sqlQueryState, dStore);
      } else if (GatherDataMode.BY_CLIENT_HTTP.equals(queryInfo.getGatherDataMode())) {
        this.loader = new HttpCollect(profileTaskQueryKey,
                                      taskInfo, connectionInfo, queryInfo, tableInfo, sqlQueryState,
                                      httpResponseFetcher, dStore);
      } else {
        throw new IllegalArgumentException("Unsupported gather data mode: " + queryInfo.getGatherDataMode());
      }
    } catch (Exception e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  public void start(long startUpTimeMillis) {
    if (!running.compareAndSet(false, true)) {
      log.warn("Task for query {} is already running.", queryInfo.getName());
      return;
    }

    lastExecutionTime = startUpTimeMillis;

    periodicTask = Thread.ofVirtual()
        .name("TaskExecutor-" + profileInfo.getId() + "-" + taskInfo.getId() + "-" + queryInfo.getId())
        .uncaughtExceptionHandler((t, e) -> log.error("Task crashed: {}", t, e))
        .start(() -> {
          try {
            while (running.get()) {
              try {
                run();
              } catch (Exception e) {
                log.error("Uncaught exception in task execution loop for query {}. Task may be unstable.", queryInfo.getName(), e);
              }

              long executionTime = System.currentTimeMillis() - lastExecutionTime;

              if (executionTime > (pullTimeout * 1000L)) {
                log.warn("Task execution took too long ({}ms), more than task pull timeout", executionTime);

                Thread.sleep(Duration.ofMillis(executionTime));
              } else {
                Thread.sleep(Duration.ofMillis((pullTimeout * 1000L) - executionTime));
              }

              lastExecutionTime = System.currentTimeMillis();
            }
          } catch (InterruptedException e) {
            log.info("Task for query {} was interrupted.", queryInfo.getName());
            Thread.currentThread().interrupt();
          } finally {
            log.info("Task executor thread for query {} has stopped.", queryInfo.getName());
            running.set(false);
          }
        });
  }

  public void stop() {
    running.set(false);
    if (periodicTask != null) {
      periodicTask.interrupt();
    }
  }

  private void run() {
    Instant before = Instant.now();

    ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(profileInfo.getId(), taskInfo.getId(), queryInfo.getId());

    eventListener.fireOnStartCollect(profileTaskQueryKey);
    loader.collect();
    eventListener.fireOnStopCollect(profileTaskQueryKey);

    Instant after = Instant.now();

    double range = ((double) after.toEpochMilli() - (double) before.toEpochMilli()) / 1000;
    log.info("Task: " + taskInfo.getName() + ", Query: " + queryInfo.getName() + ", execution (sec): " + range);
  }
}