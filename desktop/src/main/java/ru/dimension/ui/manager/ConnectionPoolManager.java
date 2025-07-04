package ru.dimension.ui.manager;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.dbcp2.BasicDataSource;
import ru.dimension.ui.model.ProfileTaskKey;
import ru.dimension.ui.model.info.ConnectionInfo;

public interface ConnectionPoolManager {

  void createDataSource(ConnectionInfo connectionInfo);

  BasicDataSource getDatasource(ConnectionInfo connectionInfo);

  Connection getConnection(ConnectionInfo connectionInfo) throws SQLException;

  Connection getConnection(ConnectionInfo connectionInfo, ProfileTaskKey profileTaskKey) throws SQLException;
}
