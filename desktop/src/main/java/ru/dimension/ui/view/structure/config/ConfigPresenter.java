package ru.dimension.ui.view.structure.config;

import static ru.dimension.ui.component.broker.MessageBroker.Component;
import static ru.dimension.ui.model.config.ConfigClasses.Connection;
import static ru.dimension.ui.model.config.ConfigClasses.Query;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.ProfileAddEvent;
import ru.dimension.ui.bus.event.ProfileRemoveEvent;
import ru.dimension.ui.bus.event.UpdateMetadataColumnsEvent;
import ru.dimension.ui.bus.event.UpdateQueryList;
import ru.dimension.ui.helper.event.EventRouteRegistry;
import ru.dimension.ui.helper.event.EventUtils;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.config.ConfigClasses;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionStatus;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.ConfigState;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.listener.ConfigListener;
import ru.dimension.ui.state.NavigatorState;
import ru.dimension.ui.view.structure.ConfigView;
import ru.dimension.ui.view.table.renderer.ConnectionStatusCellRenderer;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Singleton
public class ConfigPresenter extends WindowAdapter implements ConfigListener {

  private static final int CONNECTION_CHECK_TIMEOUT_SECONDS = 10;
  private static final int THREAD_POOL_SIZE = 4;

  private final ConfigView configView;
  private final NavigatorState navigatorState;
  private final EventListener eventListener;
  private final ConfigurationManager configurationManager;
  private final ProfileManager profileManager;
  private final ConnectionPoolManager connectionPoolManager;

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;

  private final JCheckBox checkboxConfig;

  private final EventRouteRegistry eventRegistry;

  private final Map<Integer, ConnectionStatus> connectionStatusMap = new ConcurrentHashMap<>();
  private volatile ExecutorService connectionCheckExecutor;
  private boolean statusRendererApplied = false;

  private volatile boolean statusCheckPerformed = false;
  private volatile boolean statusCheckInProgress = false;

  private volatile CompletableFuture<Void> currentCheckFuture = null;

  @Inject
  public ConfigPresenter(@Named("configView") ConfigView configView,
                         @Named("navigatorState") NavigatorState navigatorState,
                         @Named("eventListener") EventListener eventListener,
                         @Named("eventBus") EventBus eventBus,
                         @Named("configurationManager") ConfigurationManager configurationManager,
                         @Named("profileManager") ProfileManager profileManager,
                         @Named("connectionPoolManager") ConnectionPoolManager connectionPoolManager,
                         @Named("profileConfigCase") JXTableCase profileCase,
                         @Named("taskConfigCase") JXTableCase taskCase,
                         @Named("connectionConfigCase") JXTableCase connectionCase,
                         @Named("queryConfigCase") JXTableCase queryCase,
                         @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.configView = configView;
    this.navigatorState = navigatorState;
    this.eventListener = eventListener;
    this.configurationManager = configurationManager;
    this.profileManager = profileManager;
    this.connectionPoolManager = connectionPoolManager;

    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.checkboxConfig = checkboxConfig;

    this.connectionCheckExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    this.eventListener.addConfigStateListener(this);

    this.eventRegistry = EventRouteRegistry.forComponent(Component.CONFIGURATION, EventUtils::getComponent)
        .routeGlobal(ProfileAddEvent.class, this::fireProfileAdd)
        .routeGlobal(ProfileRemoveEvent.class, this::fireProfileRemove)
        .routeGlobal(UpdateMetadataColumnsEvent.class, this::fireUpdateMetadataColumns)
        .routeGlobal(UpdateQueryList.class, this::fireUpdateQueryList)
        .register(eventBus);

    checkProfileListState();
  }

  @Override
  public void windowClosing(WindowEvent e) {
    log.info("Window configuration closing event received");

    if (currentCheckFuture != null) {
      currentCheckFuture.cancel(true);
      currentCheckFuture = null;
    }

    shutdownExecutor();

    synchronized (this) {
      statusCheckInProgress = false;
      statusCheckPerformed = false;
    }
  }

  private synchronized ExecutorService getOrCreateExecutor() {
    if (connectionCheckExecutor == null
        || connectionCheckExecutor.isShutdown()
        || connectionCheckExecutor.isTerminated()) {
      connectionCheckExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
      log.debug("Created new connection check executor");
    }
    return connectionCheckExecutor;
  }

  private void shutdownExecutor() {
    ExecutorService executor = connectionCheckExecutor;
    if (executor != null && !executor.isShutdown()) {
      executor.shutdownNow();
      log.debug("Connection check executor shutdown");
    }
  }

  @Override
  public void fireShowConfig(ConfigState configState) {
    if (configState == ConfigState.SHOW) {
      startConnectionStatusCheckAsync();
      this.configView.showConfig(navigatorState.getSelectedProfile());
    }
    if (configState == ConfigState.HIDE) {
      this.configView.hideProfile();
    }
  }

  public <T> void fillProfileModel(Class<T> clazz) {

    if (ConfigClasses.Profile.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Profile..");
      fillProfileTable();
    }

    if (ConfigClasses.Task.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Task..");
      fillTaskTable();
    }

    if (Connection.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Connection..");
      fillConnectionTable();
      applyStatusRendererIfNeeded();
    }

    if (Query.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Query..");
      fillQueryTable();
    }
  }

  private void fillProfileTable() {
    TTTable<ProfileRow, JXTable> tt = profileCase.getTypedTable();
    List<ProfileRow> rows = configurationManager.getConfigList(Profile.class).stream()
        .map(e -> new ProfileRow(e.getId(), e.getName()))
        .collect(Collectors.toList());
    tt.setItems(rows);
  }

  private void fillTaskTable() {
    TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();
    List<TaskRow> rows = configurationManager.getConfigList(Task.class).stream()
        .map(e -> new TaskRow(e.getId(), e.getName()))
        .collect(Collectors.toList());
    tt.setItems(rows);
  }

  private void fillConnectionTable() {
    TTTable<ConnectionRow, JXTable> tt = connectionCase.getTypedTable();
    List<ConnectionRow> rows = configurationManager.getConfigList(ru.dimension.ui.model.config.Connection.class).stream()
        .map(e -> {
          ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(e.getId());
          ConnectionStatus status = connectionStatusMap.getOrDefault(e.getId(), ConnectionStatus.NOT_CONNECTED);
          return new ConnectionRow(
              e.getId(),
              e.getName(),
              e.getType(),
              connectionInfo.getDbType(),
              status
          );
        })
        .collect(Collectors.toList());
    tt.setItems(rows);
  }

  private void fillQueryTable() {
    TTTable<QueryRow, JXTable> tt = queryCase.getTypedTable();
    List<QueryRow> rows = configurationManager.getConfigList(Query.class).stream()
        .map(e -> new QueryRow(e.getId(), e.getName()))
        .collect(Collectors.toList());
    tt.setItems(rows);
  }

  private void applyStatusRendererIfNeeded() {
    if (statusRendererApplied) {
      return;
    }

    JXTable table = connectionCase.getJxTable();
    if (table == null) {
      return;
    }

    SwingUtilities.invokeLater(() -> {
      try {
        int statusColumnIndex = findColumnIndex(table, "Status");
        if (statusColumnIndex >= 0) {
          TableColumn column = table.getColumnModel().getColumn(statusColumnIndex);
          column.setCellRenderer(new ConnectionStatusCellRenderer());

          MouseAdapter statusMouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
              int col = table.columnAtPoint(e.getPoint());
              int row = table.rowAtPoint(e.getPoint());
              if (col < 0 || row < 0) {
                return;
              }

              String colName = table.getColumnName(col);
              if (!"Status".equals(colName)) {
                return;
              }

              Object value = table.getValueAt(row, col);
              if (value instanceof ConnectionStatus status && status == ConnectionStatus.NOT_CONNECTED) {
                TTTable<ConnectionRow, JXTable> tt = connectionCase.getTypedTable();
                if (tt != null) {
                  int modelRow = table.convertRowIndexToModel(row);
                  ConnectionRow connectionRow = tt.model().itemAt(modelRow);
                  if (connectionRow != null) {
                    recheckConnection(connectionRow.getId());
                  }
                }
              }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
              int col = table.columnAtPoint(e.getPoint());
              int row = table.rowAtPoint(e.getPoint());
              if (col >= 0 && row >= 0) {
                String colName = table.getColumnName(col);
                if ("Status".equals(colName)) {
                  Object value = table.getValueAt(row, col);
                  if (value instanceof ConnectionStatus status && status == ConnectionStatus.NOT_CONNECTED) {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                  }
                }
              }
              table.setCursor(Cursor.getDefaultCursor());
            }
          };

          table.addMouseListener(statusMouseHandler);
          table.addMouseMotionListener(statusMouseHandler);

          statusRendererApplied = true;
          log.info("Status column renderer and mouse handler applied successfully");
        }
      } catch (Exception e) {
        log.debug("Could not apply status renderer: {}", e.getMessage());
      }
    });
  }

  private int findColumnIndex(JXTable table, String columnName) {
    for (int i = 0; i < table.getColumnCount(); i++) {
      if (columnName.equals(table.getColumnName(i))) {
        return i;
      }
    }
    return -1;
  }

  private void startConnectionStatusCheckAsync() {
    if (statusCheckPerformed) {
      log.debug("Connection status check already performed, skipping");
      return;
    }

    if (statusCheckInProgress) {
      log.debug("Connection status check already in progress, skipping");
      return;
    }

    ExecutorService executor = getOrCreateExecutor();
    CompletableFuture.runAsync(this::startConnectionStatusCheck, executor);
  }

  private void startConnectionStatusCheck() {
    synchronized (this) {
      if (statusCheckPerformed || statusCheckInProgress) {
        log.debug("Connection status check already performed or in progress (synchronized check)");
        return;
      }
      statusCheckInProgress = true;
    }

    try {
      List<ru.dimension.ui.model.config.Connection> connections =
          configurationManager.getConfigList(ru.dimension.ui.model.config.Connection.class);

      if (connections.isEmpty()) {
        log.debug("No connections to check");
        markCheckCompleted();
        return;
      }

      ExecutorService executor = getOrCreateExecutor();

      log.info("Starting connection status check for {} connections", connections.size());

      for (ru.dimension.ui.model.config.Connection conn : connections) {
        connectionStatusMap.put(conn.getId(), ConnectionStatus.CONNECTING);
      }

      SwingUtilities.invokeLater(this::updateConnectionTableStatus);

      AtomicInteger completedCount = new AtomicInteger(0);
      int totalCount = connections.size();

      List<CompletableFuture<Void>> futures = connections.stream()
          .map(conn -> createConnectionCheckFutureWithTimeout(conn, completedCount, totalCount, executor))
          .collect(Collectors.toList());

      currentCheckFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
          .orTimeout(CONNECTION_CHECK_TIMEOUT_SECONDS * 2L, TimeUnit.SECONDS)
          .whenComplete((result, throwable) -> {
            if (throwable != null) {
              if (throwable instanceof TimeoutException) {
                log.warn("Connection status check timed out");
              } else {
                log.error("Error during connection status check: {}", throwable.getMessage());
              }
              markRemainingConnectionsAsFailed(connections);
            }
            markCheckCompleted();
            log.info("All connection status checks finished");
          });

    } catch (Exception e) {
      log.error("Exception starting connection status check: {}", e.getMessage());
      markCheckCompleted();
    }
  }

  private void markRemainingConnectionsAsFailed(List<ru.dimension.ui.model.config.Connection> connections) {
    for (ru.dimension.ui.model.config.Connection conn : connections) {
      ConnectionStatus currentStatus = connectionStatusMap.get(conn.getId());
      if (currentStatus == ConnectionStatus.CONNECTING) {
        connectionStatusMap.put(conn.getId(), ConnectionStatus.NOT_CONNECTED);
      }
    }
    SwingUtilities.invokeLater(this::updateConnectionTableStatus);
  }

  private void markCheckCompleted() {
    synchronized (this) {
      statusCheckInProgress = false;
      statusCheckPerformed = true;
      currentCheckFuture = null;
    }
  }

  private CompletableFuture<Void> createConnectionCheckFutureWithTimeout(
      ru.dimension.ui.model.config.Connection conn,
      AtomicInteger completedCount,
      int totalCount,
      ExecutorService executor) {

    return CompletableFuture.supplyAsync(() -> {
          try {
            return checkConnectionStatus(conn);
          } catch (Exception e) {
            log.error("Exception checking connection {}: {}", conn.getName(), e.getMessage());
            return ConnectionStatus.NOT_CONNECTED;
          }
        }, executor)
        .orTimeout(CONNECTION_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .exceptionally(ex -> {
          if (ex instanceof TimeoutException || (ex.getCause() instanceof TimeoutException)) {
            log.warn("Connection check timed out for {}", conn.getName());
          } else {
            log.error("Async exception during connection check for {}: {}",
                      conn.getName(), ex.getMessage());
          }
          return ConnectionStatus.NOT_CONNECTED;
        })
        .thenAccept(status -> {
          connectionStatusMap.put(conn.getId(), status);
          log.debug("Connection {} check completed with status: {}", conn.getName(), status);
        })
        .whenComplete((result, throwable) -> {
          int completed = completedCount.incrementAndGet();
          SwingUtilities.invokeLater(this::updateConnectionTableStatus);
          log.debug("Connection check progress: {}/{}", completed, totalCount);
        });
  }

  private ConnectionStatus checkConnectionStatus(ru.dimension.ui.model.config.Connection connection) {
    try {
      ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(connection.getId());
      if (connectionInfo == null) {
        log.warn("ConnectionInfo not found for connection id: {}", connection.getId());
        return ConnectionStatus.NOT_CONNECTED;
      }

      ConnectionType type = connection.getType();
      if (type == null) {
        type = ConnectionType.JDBC;
      }

      switch (type) {
        case JDBC:
          return checkJdbcConnection(connectionInfo);
        case HTTP:
          return checkHttpConnection(connectionInfo);
        default:
          log.warn("Unknown connection type: {} for connection: {}", type, connection.getName());
          return ConnectionStatus.NOT_CONNECTED;
      }
    } catch (Exception e) {
      log.error("Error checking connection {}: {}", connection.getName(), e.getMessage());
      return ConnectionStatus.NOT_CONNECTED;
    }
  }

  private ConnectionStatus checkJdbcConnection(ConnectionInfo connectionInfo) {
    try {
      log.debug("Checking JDBC connection: {} (id={})", connectionInfo.getName(), connectionInfo.getId());

      BasicDataSource dataSource = connectionPoolManager.getDatasource(connectionInfo);

      if (dataSource == null) {
        log.warn("Failed to create datasource for connection: {}", connectionInfo.getName());
        return ConnectionStatus.NOT_CONNECTED;
      }

      try (java.sql.Connection testConnection = dataSource.getConnection()) {
        if (testConnection != null && !testConnection.isClosed()) {
          boolean isValid = testConnection.isValid(3);

          if (isValid) {
            log.info("JDBC connection {} is READY", connectionInfo.getName());
            return ConnectionStatus.READY;
          } else {
            log.warn("JDBC connection {} failed validation", connectionInfo.getName());
            return ConnectionStatus.NOT_CONNECTED;
          }
        } else {
          log.warn("JDBC connection {} returned null or closed connection", connectionInfo.getName());
          return ConnectionStatus.NOT_CONNECTED;
        }
      } catch (SQLException sqlEx) {
        log.error("SQL error testing connection {}: {}", connectionInfo.getName(), sqlEx.getMessage());
        return ConnectionStatus.NOT_CONNECTED;
      }
    } catch (Exception e) {
      log.error("Error checking JDBC connection {}: {}", connectionInfo.getName(), e.getMessage());
      return ConnectionStatus.NOT_CONNECTED;
    }
  }

  private ConnectionStatus checkHttpConnection(ConnectionInfo connectionInfo) {
    try {
      String urlString = connectionInfo.getUrl();
      if (urlString == null || urlString.isEmpty()) {
        return ConnectionStatus.NOT_CONNECTED;
      }

      if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
        urlString = "http://" + urlString;
      }

      URI uri = URI.create(urlString);

      HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
      connection.setRequestMethod("HEAD");
      connection.setConnectTimeout(3000);
      connection.setReadTimeout(3000);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty("User-Agent", "Mozilla/5.0");
      connection.setRequestProperty("Accept", "*/*");

      int responseCode = connection.getResponseCode();
      connection.disconnect();

      return (responseCode >= 200 && responseCode < 400)
          ? ConnectionStatus.READY
          : ConnectionStatus.NOT_CONNECTED;

    } catch (Exception e) {
      log.error("Error checking HTTP connection {}: {}", connectionInfo.getName(), e.getMessage());
      return ConnectionStatus.NOT_CONNECTED;
    }
  }

  private void updateConnectionTableStatus() {
    TTTable<ConnectionRow, JXTable> tt = connectionCase.getTypedTable();
    if (tt == null) {
      return;
    }

    for (int i = 0; i < tt.model().getRowCount(); i++) {
      ConnectionRow row = tt.model().itemAt(i);
      if (row != null) {
        ConnectionStatus newStatus = connectionStatusMap.getOrDefault(row.getId(), ConnectionStatus.NOT_CONNECTED);
        row.setStatus(newStatus);
      }
    }

    tt.model().fireTableDataChanged();
  }

  public void fireProfileAdd(ProfileAddEvent event) {
    log.info("Received {} via MBassador", event);

    clearAllTables();
    refillAllTables();
    checkProfileListState();

    resetConnectionStatusCheck();
  }

  public void fireProfileRemove(ProfileRemoveEvent event) {
    log.info("Received {} via MBassador, profileId={}", event, event.profileId());

    clearAllTables();
    refillAllTables();
    checkProfileListState();

    resetConnectionStatusCheck();
  }

  public void fireUpdateMetadataColumns(UpdateMetadataColumnsEvent event) {
    log.info("Received UpdateMetadataColumnsEvent for queryId={}, queryName={}, columns={}",
             event.queryId(), event.queryName(), event.columns().size());
  }

  public void fireUpdateQueryList(UpdateQueryList event) {
    log.info("Received UpdateQueryList for taskId={}",
             event.taskId());
  }

  private void checkProfileListState() {
    boolean hasProfiles = !configurationManager.getConfigList(Profile.class).isEmpty();

    checkboxConfig.setEnabled(hasProfiles);

    if (!hasProfiles) {
      checkboxConfig.setSelected(false);
    }
  }

  private void clearAllTables() {
    profileCase.clearTable();
    taskCase.clearTable();
    connectionCase.clearTable();
    queryCase.clearTable();
  }

  private void refillAllTables() {
    fillProfileModel(Profile.class);
    fillProfileModel(Task.class);
    fillProfileModel(ru.dimension.ui.model.config.Connection.class);
    fillProfileModel(Query.class);
  }

  public void updateConnectionStatus(int connectionId, ConnectionStatus status) {
    connectionStatusMap.put(connectionId, status);
    SwingUtilities.invokeLater(this::updateConnectionTableStatus);
  }

  public ConnectionStatus getConnectionStatus(int connectionId) {
    return connectionStatusMap.getOrDefault(connectionId, ConnectionStatus.NOT_CONNECTED);
  }

  public void resetConnectionStatusCheck() {
    synchronized (this) {
      if (currentCheckFuture != null) {
        currentCheckFuture.cancel(true);
        currentCheckFuture = null;
      }

      statusCheckPerformed = false;
      statusCheckInProgress = false;
      connectionStatusMap.clear();
    }
    log.debug("Connection status check reset");
  }

  public void recheckConnection(int connectionId) {
    ExecutorService executor = getOrCreateExecutor();

    ru.dimension.ui.model.config.Connection connection = configurationManager
        .getConfigList(ru.dimension.ui.model.config.Connection.class)
        .stream()
        .filter(c -> c.getId() == connectionId)
        .findFirst()
        .orElse(null);

    if (connection != null) {
      connectionStatusMap.put(connectionId, ConnectionStatus.CONNECTING);
      SwingUtilities.invokeLater(this::updateConnectionTableStatus);

      CompletableFuture.supplyAsync(() -> {
            try {
              return checkConnectionStatus(connection);
            } catch (Exception e) {
              log.error("Error rechecking connection {}: {}", connection.getName(), e.getMessage());
              return ConnectionStatus.NOT_CONNECTED;
            }
          }, executor)
          .orTimeout(CONNECTION_CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .exceptionally(ex -> {
            log.error("Async exception rechecking connection {}: {}", connectionId, ex.getMessage());
            return ConnectionStatus.NOT_CONNECTED;
          })
          .thenAccept(status -> {
            connectionStatusMap.put(connectionId, status);
            log.info("Recheck completed for connection {} with status: {}", connection.getName(), status);
          })
          .whenComplete((result, throwable) -> {
            SwingUtilities.invokeLater(this::updateConnectionTableStatus);
          });
    }
  }

  public void recheckAllConnections() {
    log.info("Force rechecking all connections");
    resetConnectionStatusCheck();
    startConnectionStatusCheckAsync();
  }
}