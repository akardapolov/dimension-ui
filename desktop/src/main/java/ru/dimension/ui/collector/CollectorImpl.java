package ru.dimension.ui.collector;

import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.db.model.profile.table.BType;
import ru.dimension.ui.collector.collect.prometheus.ExporterParser;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.executor.TaskExecutorPool;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;

@Log4j2
@Singleton
public class CollectorImpl implements Collector, JdbcLoader, HttpLoader {

  private final DStore dStore;
  private final SqlQueryState sqlQueryState;

  private final ProfileManager profileManager;
  private final TaskExecutorPool taskExecutorPool;

  private final ExporterParser exporterParser;

  private final HttpResponseFetcher httpResponseFetcher;

  @Inject
  public CollectorImpl(@Named("localDB") DStore dStore,
                       @Named("sqlQueryState") SqlQueryState sqlQueryState,
                       @Named("profileManager") ProfileManager profileManager,
                       @Named("taskExecutorPool") TaskExecutorPool taskExecutorPool,
                       @Named("exporterParser") ExporterParser exporterParser,
                       @Named("httpResponseFetcher") HttpResponseFetcher httpResponseFetcher) {
    this.dStore = dStore;
    this.sqlQueryState = sqlQueryState;
    this.profileManager = profileManager;
    this.taskExecutorPool = taskExecutorPool;
    this.exporterParser = exporterParser;
    this.httpResponseFetcher = httpResponseFetcher;
  }

  @Override
  public void fillMetadataJdbc(ProfileTaskQueryKey profileTaskQueryKey,
                               QueryInfo queryInfo,
                               TableInfo tableInfo,
                               Connection connection) {
    try {
      TProfile tProfile = dStore.loadJdbcTableMetadata(connection, queryInfo.getText(), tableInfo.getSProfile());

      tableInfo.setTableType(tProfile.getTableType());
      tableInfo.setIndexType(tProfile.getIndexType());
      tableInfo.setBackendType(tProfile.getBackendType());
      tableInfo.setCompression(tProfile.getCompression());
      tableInfo.setCProfiles(tProfile.getCProfiles());

      long localDateTime = System.currentTimeMillis();
      long serverDateTime = getSysdate(queryInfo.getDbType().getQuery(), connection, log);

      queryInfo.setDeltaLocalServerTime(localDateTime - serverDateTime);

      sqlQueryState.initializeLastTimestamp(profileTaskQueryKey, serverDateTime);

      log.info("Server timestamp for: {} is: {}", profileTaskQueryKey, toLocalDateTime(serverDateTime));
      log.info("Last timestamp for: {} is: {}", profileTaskQueryKey, toLocalDateTime(sqlQueryState.getLastTimestamp(profileTaskQueryKey)));

    } catch (Exception e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void fillMetadataHttp(ConnectionInfo connectionInfo,
                               QueryInfo queryInfo,
                               TableInfo tableInfo) {
    try {
      SProfile sProfile;
      if (tableInfo.getSProfile().getCsTypeMap().isEmpty()) {
        sProfile = new SProfile();
        sProfile.setBackendType(BType.BERKLEYDB);
        fillSProfileFromResponse(exporterParser, httpResponseFetcher.fetchResponse(getHttpProtocol(connectionInfo)), sProfile);
      } else {
        sProfile = tableInfo.getSProfile();
        sProfile.setBackendType(BType.BERKLEYDB);
      }

      TProfile tProfile = dStore.loadDirectTableMetadata(sProfile);

      tableInfo.setTableType(tProfile.getTableType());
      tableInfo.setIndexType(tProfile.getIndexType());
      tableInfo.setBackendType(tProfile.getBackendType());
      tableInfo.setCompression(tProfile.getCompression());
      tableInfo.setCProfiles(tProfile.getCProfiles());

      queryInfo.setDeltaLocalServerTime(0);
    } catch (Exception e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void start(ProfileInfo profileInfo) {
    long startUpTimeMillis = System.currentTimeMillis();

    int profileId = profileInfo.getId();

    profileInfo.getTaskInfoList()
        .forEach(taskId -> profileManager.getQueryInfoList(profileId, taskId)
            .forEach(queryInfo -> {
              ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(profileId, taskId, queryInfo.getId());
              taskExecutorPool.get(profileTaskQueryKey).start(startUpTimeMillis);
            }));
  }

  @Override
  public void stop(ProfileInfo profileInfo) {
    int profileId = profileInfo.getId();

    profileInfo.getTaskInfoList()
        .forEach(taskId -> profileManager.getQueryInfoList(profileId, taskId)
            .forEach(queryInfo -> {
              ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(profileId, taskId, queryInfo.getId());
              taskExecutorPool.get(profileTaskQueryKey).stop();
            }));
  }

  private LocalDateTime toLocalDateTime(long ofEpochMilli) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(ofEpochMilli), TimeZone.getDefault().toZoneId());
  }
}