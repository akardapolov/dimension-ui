package ru.dimension.ui.manager.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.dbcp2.BasicDataSource;
import ru.dimension.ui.helper.ConnectionErrorHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.security.EncryptDecrypt;
import ru.dimension.ui.manager.ConnectionPoolManager;

@Log4j2
@Singleton
public class ConnectionPoolManagerImpl implements ConnectionPoolManager {

  private final EncryptDecrypt encryptDecrypt;

  private final Map<Integer, BasicDataSource> dataSourceMap = new ConcurrentHashMap<>();
  private final Map<Integer, URLClassLoader> classLoaderMap = new ConcurrentHashMap<>();
  private final Map<Integer, Map<ProfileTaskQueryKey, Connection>> connectionForTaskMap = new ConcurrentHashMap<>();

  @Inject
  public ConnectionPoolManagerImpl(EncryptDecrypt encryptDecrypt) {
    this.encryptDecrypt = encryptDecrypt;
  }

  @Override
  public void createDataSource(ConnectionInfo connectionInfo) {
    getDatasource(connectionInfo);
  }

  @Override
  public BasicDataSource getDatasource(ConnectionInfo connectionInfo) {
    return dataSourceMap.computeIfAbsent(connectionInfo.getId(), id -> createDataSourceInternal(connectionInfo));
  }

  private BasicDataSource createDataSourceInternal(ConnectionInfo connectionInfo) {
    URLClassLoader classLoader = null;
    try {
      classLoader = getClassLoader(connectionInfo.getJar());

      BasicDataSource basicDataSource = new BasicDataSource();
      basicDataSource.setDriverClassLoader(classLoader);
      basicDataSource.setDriverClassName(connectionInfo.getDriver());
      basicDataSource.setUrl(connectionInfo.getUrl());
      basicDataSource.setUsername(connectionInfo.getUserName());
      basicDataSource.setPassword(encryptDecrypt.decrypt(connectionInfo.getPassword()));
      basicDataSource.setInitialSize(0);
      basicDataSource.setMaxTotal(20);
      basicDataSource.setMaxWaitMillis(TimeUnit.SECONDS.toMillis(5));

      if (connectionInfo.getDriver().contains("clickhouse")) {
        basicDataSource.setValidationQueryTimeout(5);
        basicDataSource.setValidationQuery("SELECT 1");
        basicDataSource.addConnectionProperty("compress", "0");
      }
      if (connectionInfo.getDriver().contains("com.microsoft.sqlserver")) {
        basicDataSource.setValidationQueryTimeout(5);
        basicDataSource.setValidationQuery("SELECT 1");
        basicDataSource.addConnectionProperty("encrypt", "true");
        basicDataSource.addConnectionProperty("trustServerCertificate", "true");
      }
      classLoaderMap.put(connectionInfo.getId(), classLoader);
      return basicDataSource;
    } catch (Exception e) {
      if (classLoader != null) {
        try {
          classLoader.close();
        } catch (IOException ignored) {
        }
      }
      ConnectionErrorHelper.logConnectionError(connectionInfo.getName(), new RuntimeException(e));
      throw new RuntimeException("Failed to create data source: " + e.getMessage(), e);
    }
  }

  private URLClassLoader getClassLoader(String jar) throws ClassNotFoundException, InstantiationException,
      IllegalAccessException, MalformedURLException {
    URL url = new File(jar.trim()).toURI().toURL();
    return new URLClassLoader(new URL[]{url});
  }

  @Override
  public Connection getConnection(ConnectionInfo connectionInfo) throws SQLException {
    try {
      Connection connection = getDatasource(connectionInfo).getConnection();
      if (connection.isClosed()) {
        throw new SQLException("Connection is closed for: " + connectionInfo.getName());
      }
      return connection;
    } catch (RuntimeException e) {
      throw new SQLException("Failed to get connection: " + e.getMessage(), e);
    }
  }

  @Override
  public Connection getConnection(ConnectionInfo connectionInfo, ProfileTaskQueryKey profileTaskQueryKey) throws SQLException {
    if (connectionInfo == null || connectionInfo.getId() <= 0) {
      throw new IllegalArgumentException("ConnectionInfo cannot be null");
    }

    Map<ProfileTaskQueryKey, Connection> taskConnections =
        connectionForTaskMap.computeIfAbsent(connectionInfo.getId(), id -> new ConcurrentHashMap<>());

    synchronized (taskConnections) {
      Connection connection = taskConnections.get(profileTaskQueryKey);
      if (connection == null || connection.isClosed()) {
        closeConnectionSafely(connection, connectionInfo.getId());
        connection = getDatasource(connectionInfo).getConnection();
        taskConnections.put(profileTaskQueryKey, connection);
      }
      return connection;
    }
  }

  @Override
  public void releaseTaskConnection(int connectionId, ProfileTaskQueryKey profileTaskQueryKey) {
    Map<ProfileTaskQueryKey, Connection> taskConnections = connectionForTaskMap.get(connectionId);
    if (taskConnections != null) {
      Connection connection = taskConnections.remove(profileTaskQueryKey);
      if (connection != null) {
        closeConnectionSafely(connection, connectionId);
        log.info("Released task connection for connectionId: {}, key: {}", connectionId, profileTaskQueryKey);
      }
    }
  }

  @Override
  public void removeConnection(int connectionId) {
    log.info("Removing connection pool resources for connectionId: {}", connectionId);

    Map<ProfileTaskQueryKey, Connection> taskConnections = connectionForTaskMap.remove(connectionId);
    if (taskConnections != null) {
      for (Map.Entry<ProfileTaskQueryKey, Connection> entry : taskConnections.entrySet()) {
        closeConnectionSafely(entry.getValue(), connectionId);
      }
      log.info("Closed {} task connections for connectionId: {}", taskConnections.size(), connectionId);
    }

    BasicDataSource dataSource = dataSourceMap.remove(connectionId);
    if (dataSource != null) {
      try {
        dataSource.close();
        log.info("Closed BasicDataSource for connectionId: {}", connectionId);
      } catch (SQLException e) {
        log.error("Error closing BasicDataSource for connectionId: {}", connectionId, e);
      }
    }

    URLClassLoader classLoader = classLoaderMap.remove(connectionId);
    if (classLoader != null) {
      try {
        classLoader.close();
        log.info("Closed URLClassLoader for connectionId: {}", connectionId);
      } catch (IOException e) {
        log.error("Error closing URLClassLoader for connectionId: {}", connectionId, e);
      }
    }

    log.info("Successfully removed all connection pool resources for connectionId: {}", connectionId);
  }

  private void closeConnectionSafely(Connection connection, int connectionId) {
    if (connection != null) {
      try {
        if (!connection.isClosed()) {
          connection.close();
        }
      } catch (SQLException e) {
        log.error("Error closing connection for connectionId: {}", connectionId, e);
      }
    }
  }
}