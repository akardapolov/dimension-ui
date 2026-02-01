package ru.dimension.ui.manager.impl;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.dbcp2.BasicDataSource;
import ru.dimension.ui.exception.TimeoutConnectionException;
import ru.dimension.ui.model.ProfileTaskKey;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.security.EncryptDecrypt;
import ru.dimension.ui.manager.ConnectionPoolManager;

@Log4j2
@Singleton
public class ConnectionPoolManagerImpl implements ConnectionPoolManager {

  private final EncryptDecrypt encryptDecrypt;

  private final Map<Integer, BasicDataSource> dataSourceMap = new ConcurrentHashMap<>();
  private final Map<Integer, List<Connection>> connectionMap = new ConcurrentHashMap<>();
  private final Map<Integer, Map<ProfileTaskKey, Connection>> connectionForTaskMap = new ConcurrentHashMap<>();

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
    BasicDataSource basicDataSource = null;
    try {
      basicDataSource = new BasicDataSource();
      basicDataSource.setDriverClassLoader(getClassLoader(connectionInfo.getJar()));
      basicDataSource.setDriverClassName(connectionInfo.getDriver());
      basicDataSource.setUrl(connectionInfo.getUrl());
      basicDataSource.setUsername(connectionInfo.getUserName());
      basicDataSource.setPassword(encryptDecrypt.decrypt(connectionInfo.getPassword()));
      basicDataSource.setInitialSize(3);
      basicDataSource.setMaxTotal(5);
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

      if (!dataSourceMap.containsKey(connectionInfo.getId())) {
        dataSourceMap.put(connectionInfo.getId(), basicDataSource);
      }
    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | MalformedURLException e) {
      log.error("Failed to create data source for connection ID: {}", connectionInfo.getId(), e);
      throw new RuntimeException("Failed to create data source: " + e.getMessage(), e);
    }

    return basicDataSource;
  }

  private ClassLoader getClassLoader(String jar) throws ClassNotFoundException, InstantiationException,
      IllegalAccessException, MalformedURLException {
    URL url = new File(jar.trim()).toURI().toURL();
    URLClassLoader ucl = new URLClassLoader(new URL[]{url});
    return ucl;
  }

  @Override
  public Connection getConnection(ConnectionInfo connectionInfo) {
    try {
      BasicDataSource dataSource = getDatasource(connectionInfo);
      if (dataSource == null) {
        log.error("DataSource is null for connection: {}", connectionInfo.getName());
        return null;
      }

      Connection connection = dataSource.getConnection();
      if (connection == null || connection.isClosed()) {
        log.error("Connection is null or closed for: {}", connectionInfo.getName());
        return null;
      }

      return connection;
    } catch (Exception e) {
      log.error("Failed to get connection for: {}", connectionInfo.getName(), e);
      return null;
    }
  }

  @Override
  public Connection getConnection(ConnectionInfo connectionInfo, ProfileTaskKey profileTaskKey) throws SQLException {
    if (connectionInfo == null || connectionInfo.getId() <= 0) {
      throw new IllegalArgumentException("ConnectionInfo cannot be null");
    }

    if (!connectionForTaskMap.containsKey(connectionInfo.getId())) {
      connectionForTaskMap.put(connectionInfo.getId(), new HashMap<>());
    }

    Map<ProfileTaskKey, Connection> taskConnections = connectionForTaskMap.get(connectionInfo.getId());
    if (!taskConnections.containsKey(profileTaskKey)) {
      Connection connection = futureExecutionWithTimeout(5, connectionInfo);
      taskConnections.put(profileTaskKey, connection);
    }

    return taskConnections.get(profileTaskKey);
  }

  @Override
  public void removeConnection(int connectionId) {
    log.info("Removing connection pool resources for connectionId: {}", connectionId);

    // Close and remove connections from connectionMap
    List<Connection> connections = connectionMap.remove(connectionId);
    if (connections != null) {
      for (Connection conn : connections) {
        closeConnectionSafely(conn, connectionId);
      }
      log.info("Closed {} connections from connectionMap for connectionId: {}", connections.size(), connectionId);
    }

    // Close and remove connections from connectionForTaskMap
    Map<ProfileTaskKey, Connection> taskConnections = connectionForTaskMap.remove(connectionId);
    if (taskConnections != null) {
      for (Map.Entry<ProfileTaskKey, Connection> entry : taskConnections.entrySet()) {
        closeConnectionSafely(entry.getValue(), connectionId);
      }
      log.info("Closed {} task connections for connectionId: {}", taskConnections.size(), connectionId);
    }

    // Close and remove BasicDataSource
    BasicDataSource dataSource = dataSourceMap.remove(connectionId);
    if (dataSource != null) {
      try {
        dataSource.close();
        log.info("Closed BasicDataSource for connectionId: {}", connectionId);
      } catch (SQLException e) {
        log.error("Error closing BasicDataSource for connectionId: {}", connectionId, e);
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

  private Connection futureExecutionWithTimeout(int timeoutSeconds,
                                                ConnectionInfo connectionInfo) {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Callable<Connection> task = () -> dataSourceMap.get(connectionInfo.getId()).getConnection();

    Future<Connection> future = executor.submit(task);
    try {
      return future.get(timeoutSeconds, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      future.cancel(true);
      throw new TimeoutConnectionException(
          "Timeout " + timeoutSeconds + " sec. is exceed to get data from: " + connectionInfo);
    } catch (Exception e) {
      log.error("Failed to get connection for connection ID: {}", connectionInfo.getId(), e);
      dataSourceMap.remove(connectionInfo.getId());
      throw new RuntimeException("Failed to get connection: " + e.getMessage(), e);
    } finally {
      executor.shutdownNow();
    }
  }
}