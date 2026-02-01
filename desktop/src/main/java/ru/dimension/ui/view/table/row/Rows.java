package ru.dimension.ui.view.table.row;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hc.core5.http.Method;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CSType;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.db.model.profile.cstype.SType;
import ru.dimension.tt.annotation.ColumnKind;
import ru.dimension.tt.annotation.TTColumn;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.DBType;
import ru.dimension.ui.model.parse.ParseType;
import ru.dimension.ui.model.type.ConnectionStatus;
import ru.dimension.ui.model.type.ConnectionType;

public class Rows {

  @NoArgsConstructor
  public static class DesignRow {
    @TTColumn(name = "Design name", order = 1, editable = false)
    private String name;

    public DesignRow(String name) {
      this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  @NoArgsConstructor
  public static class ProfileRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1)
    private String name;

    public ProfileRow(int id, String name) {
      this.id = id;
      this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  @NoArgsConstructor
  public static class TaskRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1)
    private String name;

    public TaskRow(int id, String name) {
      this.id = id;
      this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  @NoArgsConstructor
  public static class QueryRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1)
    private String name;

    public QueryRow(int id, String name) {
      this.id = id;
      this.name = name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  @NoArgsConstructor
  public static class PickableQueryRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(
        order = 2,
        name = "Pick",
        kind = ColumnKind.CHECKBOX,
        editable = true,
        minWidth = 30,
        maxWidth = 35,
        preferredWidth = 30
    )
    private boolean pick;

    public PickableQueryRow(int id, String name) {
      this(id, name, false);
    }

    public PickableQueryRow(int id, String name, boolean pick) {
      this.id = id;
      this.name = name;
      this.pick = pick;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isPick() { return pick; }
    public void setPick(boolean pick) { this.pick = pick; }
  }

  @NoArgsConstructor
  public static class QueryTableRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(name = "Description", order = 2, editable = false)
    private String description;

    @TTColumn(name = "Text", order = 3, editable = false)
    private String text;

    public QueryTableRow(int id, String name, String description, String text) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.text = text;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
  }

  @NoArgsConstructor
  public static class ConnectionRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(name = "Type", order = 2, editable = false, minWidth = 40, maxWidth = 50, preferredWidth = 40)
    private String type;

    @TTColumn(name = "Status", order = 3, editable = false, minWidth = 40, maxWidth = 50, preferredWidth = 40)
    private ConnectionStatus status;

    private DBType dbType;

    public ConnectionRow(int id, String name) {
      this(id, name, (ConnectionType) null);
    }

    public ConnectionRow(int id, String name, ConnectionType connectionType) {
      this.id = id;
      this.name = name;
      this.type = connectionType != null ? connectionType.getName() : ConnectionType.JDBC.getName();
      this.status = ConnectionStatus.NOT_CONNECTED;
    }

    public ConnectionRow(int id, String name, String type) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.status = ConnectionStatus.NOT_CONNECTED;
    }

    public ConnectionRow(int id, String name, ConnectionType connectionType, DBType dbType) {
      this.id = id;
      this.name = name;
      this.type = connectionType != null ? connectionType.getName() : ConnectionType.JDBC.getName();
      this.dbType = dbType;
      this.status = ConnectionStatus.NOT_CONNECTED;
    }

    public ConnectionRow(int id, String name, ConnectionType connectionType, DBType dbType, ConnectionStatus status) {
      this.id = id;
      this.name = name;
      this.type = connectionType != null ? connectionType.getName() : ConnectionType.JDBC.getName();
      this.dbType = dbType;
      this.status = status != null ? status : ConnectionStatus.NOT_CONNECTED;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public DBType getDbType() { return dbType; }
    public void setDbType(DBType dbType) { this.dbType = dbType; }
    public ConnectionStatus getStatus() { return status; }
    public void setStatus(ConnectionStatus status) { this.status = status; }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ConnectionTemplateRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(name = "User Name", order = 2, editable = false)
    private String userName;

    @TTColumn(name = "Password", order = 3, editable = false, visible = false)
    private String password;

    @TTColumn(name = "URL", order = 4, editable = false)
    private String url;

    @TTColumn(name = "Jar", order = 5, editable = false)
    private String jar;

    @TTColumn(name = "Driver", order = 6, editable = false)
    private String driver;

    @TTColumn(name = "Type", order = 7, editable = false)
    private String type;

    @TTColumn(name = "HTTP Method", order = 8, editable = false)
    private String httpMethod;

    @TTColumn(name = "HTTP Parse Type", order = 9, editable = false)
    private String httpParseType;

    public ConnectionTemplateRow(ru.dimension.ui.model.config.Connection c) {
      this.id = c.getId();
      this.name = c.getName();
      this.userName = c.getUserName();
      this.password = c.getPassword();
      this.url = c.getUrl();
      this.jar = c.getJar();
      this.driver = c.getDriver();

      ConnectionType connType = c.getType() != null ? c.getType() : ConnectionType.JDBC;
      this.type = connType.getName();

      Method method = c.getHttpMethod();
      this.httpMethod = method != null ? method.name() : "";

      ParseType parseType = c.getParseType();
      this.httpParseType = parseType != null ? parseType.name() : "";
    }
  }

  public static ConnectionTemplateRow mapConnection(Connection connection) {
    if (connection == null) {
      return null;
    }

    ConnectionTemplateRow row = new ConnectionTemplateRow();
    row.setId(connection.getId());
    row.setName(connection.getName());
    row.setUserName(connection.getUserName());
    row.setPassword(connection.getPassword());
    row.setUrl(connection.getUrl());
    row.setJar(connection.getJar());
    row.setDriver(connection.getDriver());

    ConnectionType connType = connection.getType() != null ? connection.getType() : ConnectionType.JDBC;
    row.setType(connType.getName());

    Method httpMethod = connection.getHttpMethod();
    row.setHttpMethod(httpMethod != null ? httpMethod.name() : "");

    ParseType parseType = connection.getParseType();
    row.setHttpParseType(parseType != null ? parseType.name() : "");

    return row;
  }

  public static java.util.List<ConnectionTemplateRow> mapList(java.util.Collection<Connection> connections) {
    if (connections == null) {
      return java.util.Collections.emptyList();
    }

    return connections.stream()
        .map(Rows::mapConnection)
        .filter(java.util.Objects::nonNull)
        .collect(java.util.stream.Collectors.toList());
  }

  @NoArgsConstructor
  public static class EntityRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(
        order = 2,
        name = "Pick",
        kind = ColumnKind.CHECKBOX,
        editable = true,
        minWidth = 30,
        maxWidth = 40,
        preferredWidth = 30
    )
    private boolean pick;

    public EntityRow(int id, String name) {
      this(id, name, false);
    }

    public EntityRow(int id, String name, boolean pick) {
      this.id = id;
      this.name = name;
      this.pick = pick;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isPick() { return pick; }
    public void setPick(boolean pick) { this.pick = pick; }
  }

  @NoArgsConstructor
  public static class TimestampRow {
    @TTColumn(name = "Name", order = 0, editable = false)
    private String name;

    public TimestampRow(String name) {
      this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  @NoArgsConstructor
  public static class ColumnRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(
        order = 2,
        name = "Pick",
        kind = ColumnKind.CHECKBOX,
        editable = true,
        minWidth = 30,
        maxWidth = 30,
        preferredWidth = 30
    )
    private boolean pick;

    private CProfile origin;

    public ColumnRow(CProfile p, boolean pick) {
      if (p == null) {
        throw new IllegalArgumentException("CProfile cannot be null");
      }
      this.id = p.getColId();
      this.name = p.getColName();
      this.pick = pick;
      this.origin = p;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isPick() { return pick; }
    public void setPick(boolean pick) { this.pick = pick; }

    public CProfile getOrigin() {
      if (origin == null) {
        throw new IllegalStateException("Origin CProfile is null for ColumnRow id=" + id + ", name=" + name);
      }
      return origin;
    }

    public boolean hasOrigin() {
      return origin != null;
    }
  }

  @NoArgsConstructor
  public static class MetricRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(
        order = 2,
        name = "Pick",
        kind = ColumnKind.CHECKBOX,
        editable = true,
        minWidth = 30,
        maxWidth = 30,
        preferredWidth = 30
    )
    private boolean pick;

    private Metric origin;

    public MetricRow(Metric m, boolean pick) {
      if (m == null) {
        throw new IllegalArgumentException("Metric cannot be null");
      }
      this.id = m.getId();
      this.name = m.getName();
      this.pick = pick;
      this.origin = m;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isPick() { return pick; }
    public void setPick(boolean pick) { this.pick = pick; }

    public Metric getOrigin() {
      if (origin == null) {
        throw new IllegalStateException("Origin Metric is null for MetricRow id=" + id + ", name=" + name);
      }
      return origin;
    }

    public boolean hasOrigin() {
      return origin != null;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MetadataRow {

    @TTColumn(name = "Column ID", order = 0, visible = false)
    private int colId;

    @TTColumn(name = "Column ID SQL", order = 1, visible = false)
    private int colIdSql;

    @TTColumn(name = "Column", order = 2, editable = false)
    private String colName;

    @TTColumn(name = "DB Type", order = 3, editable = false)
    private String colDbTypeName;

    @TTColumn(name = "Storage", order = 4, editable = false)
    private SType storageType;

    @TTColumn(name = "Column Type", order = 5, editable = false)
    private CType columnType;

    @TTColumn(
        name = "Dimension",
        order = 6,
        kind = ColumnKind.CHECKBOX,
        editable = true,
        minWidth = 50,
        maxWidth = 70,
        preferredWidth = 60
    )
    private boolean dimension;

    private CProfile origin;

    public MetadataRow(CProfile cProfile, boolean dimension) {
      if (cProfile == null) {
        throw new IllegalArgumentException("CProfile cannot be null");
      }
      this.colId = cProfile.getColId();
      this.colIdSql = cProfile.getColIdSql();
      this.colName = cProfile.getColName();
      this.colDbTypeName = cProfile.getColDbTypeName();
      CSType csType = cProfile.getCsType();
      if (csType != null) {
        this.storageType = csType.getSType();
        this.columnType = csType.getCType();
      }
      this.dimension = dimension;
      this.origin = cProfile;
    }

    public boolean hasOrigin() {
      return origin != null;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TemplateTaskRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(name = "Timeout", order = 2, editable = false)
    private String timeout;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TemplateConnectionRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(name = "Type", order = 2, editable = false)
    private String type;

    private DBType dbType;

    public TemplateConnectionRow(int id, String name, String type) {
      this(id, name, type, DBType.UNKNOWN);
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TemplateQueryRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(name = "Gather Data Mode", order = 2, editable = false)
    private String gatherDataMode;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TemplateMetricRow {
    @TTColumn(name = "ID", order = 0, visible = false)
    private int id;

    @TTColumn(name = "Name", order = 1, editable = false)
    private String name;

    @TTColumn(name = "Is Default", order = 2, editable = false)
    private boolean isDefault;

    @TTColumn(name = "X Axis", order = 3, editable = false)
    private String xAxis;

    @TTColumn(name = "Y Axis", order = 4, editable = false)
    private String yAxis;

    @TTColumn(name = "Group", order = 5, editable = false)
    private String group;

    @TTColumn(name = "Group Function", order = 6, editable = false)
    private String groupFunction;

    @TTColumn(name = "Chart Type", order = 7, editable = false)
    private String chartType;

    private Metric origin;

    public TemplateMetricRow(int id, String name, boolean isDefault, String xAxis, String yAxis, String group, String groupFunction, String chartType) {
      this(id, name, isDefault, xAxis, yAxis, group, groupFunction, chartType, null);
    }

    public boolean hasOrigin() {
      return origin != null;
    }
  }
}