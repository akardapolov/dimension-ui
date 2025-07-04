package ru.dimension.ui.connection;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.model.info.ConnectionInfo;


@Log4j2
@Disabled
public class ConnectionTest {

  @Test
  public void put_and_get_test() {
    ConnectionInfo connectionInfo = new ConnectionInfo();

    connectionInfo.setName("Po");
    connectionInfo.setUserName("postgres");
    connectionInfo.setPassword("postgres");
    connectionInfo.setUrl("jdbc:postgresql://localhost:32769/postgres");
    connectionInfo.setJar("C:\\Users\\.temp\\postgresql-42.2.20.jar");
    connectionInfo.setDriver("org.postgresql.Driver");

    try {
      Connection dbConnection = getDatasource(connectionInfo).getConnection();
      Statement s;
      ResultSet rs;
      ResultSetMetaData rsmd;

      s = dbConnection.createStatement();
      s.executeQuery("SELECT now()");
      rs = s.getResultSet();
      rsmd = rs.getMetaData();

      for (int i = 1; i <= rsmd.getColumnCount(); i++) {
        System.out.println(rsmd.getColumnName(i).toUpperCase());
      }

      rs.close();
      s.close();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }


  private BasicDataSource getDatasource(ConnectionInfo connectionInfo) {
    BasicDataSource basicDataSource = null;
    try {
      basicDataSource = new BasicDataSource();
      basicDataSource.setDriverClassLoader(getClassLoader(connectionInfo.getJar()));
      basicDataSource.setDriverClassName(connectionInfo.getDriver());
      basicDataSource.setUrl(connectionInfo.getUrl());
      basicDataSource.setUsername(connectionInfo.getUserName());
      basicDataSource.setPassword((connectionInfo.getPassword()));
      basicDataSource.setInitialSize(2);
      basicDataSource.setMaxTotal(3);
      basicDataSource.setMaxWaitMillis(TimeUnit.SECONDS.toMillis(5));

    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException |
             MalformedURLException e) {
      log.error(e.toString());
    }

    return basicDataSource;
  }

  private ClassLoader getClassLoader(String jar) throws ClassNotFoundException, InstantiationException,
      IllegalAccessException, MalformedURLException {
    URL url = new File(jar.trim()).toURI().toURL();
    URLClassLoader ucl = new URLClassLoader(new URL[]{url});
    return ucl;
  }
}
