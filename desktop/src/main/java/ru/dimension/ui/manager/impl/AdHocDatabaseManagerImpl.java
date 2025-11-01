package ru.dimension.ui.manager.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.DBase;
import ru.dimension.db.config.DBaseConfig;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.table.BType;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConnectionPoolManager;

@Log4j2
@Singleton
public class AdHocDatabaseManagerImpl implements AdHocDatabaseManager {

  private final FilesHelper filesHelper;
  private final ConnectionPoolManager connectionPoolManager;

  private Map<Integer, DStore> dStoreMap;

  @Inject
  public AdHocDatabaseManagerImpl(FilesHelper filesHelper,
                                  @Named("connectionPoolManager") ConnectionPoolManager connectionPoolManager) {
    this.filesHelper = filesHelper;
    this.connectionPoolManager = connectionPoolManager;

    this.dStoreMap = new HashMap<>();
  }

  @Override
  public void createDataBase(ConnectionInfo connectionInfo) {
    try {
      filesHelper.createExternalDirectory(String.valueOf(connectionInfo.getId()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    String configDirectory = filesHelper.getExternalDir() + filesHelper.getFileSeparator() + connectionInfo.getId();

    DBaseConfig dBaseConfig = new DBaseConfig().setConfigDirectory(configDirectory);
    switch (connectionInfo.getDbType()) {
      case CLICKHOUSE ->
          dStoreMap.putIfAbsent(connectionInfo.getId(), getDBase(dBaseConfig, BType.CLICKHOUSE, connectionInfo).getDStore());
      case POSTGRES ->
          dStoreMap.putIfAbsent(connectionInfo.getId(), getDBase(dBaseConfig, BType.POSTGRES, connectionInfo).getDStore());
      case ORACLE ->
          dStoreMap.putIfAbsent(connectionInfo.getId(), getDBase(dBaseConfig, BType.ORACLE, connectionInfo).getDStore());
      case MSSQL ->
          dStoreMap.putIfAbsent(connectionInfo.getId(), getDBase(dBaseConfig, BType.MSSQL, connectionInfo).getDStore());
      case MYSQL ->
          dStoreMap.putIfAbsent(connectionInfo.getId(), getDBase(dBaseConfig, BType.MYSQL, connectionInfo).getDStore());
      case DUCKDB ->
          dStoreMap.putIfAbsent(connectionInfo.getId(), getDBase(dBaseConfig, BType.DUCKDB, connectionInfo).getDStore());
      default -> throw new RuntimeException("Not supported database: " + connectionInfo.getDbType());
    }
  }

  private DBase getDBase(DBaseConfig dBaseConfig,
                         BType bType,
                         ConnectionInfo connectionInfo) {
    return new DBase(dBaseConfig, bType, connectionPoolManager.getDatasource(connectionInfo));
  }

  @Override
  public DStore getDataBase(ConnectionInfo connectionInfo) {
    return dStoreMap.get(connectionInfo.getId());
  }
}
