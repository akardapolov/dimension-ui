package ru.dimension.ui.view.table.icon;

import java.awt.Color;
import java.util.function.Function;
import ru.dimension.db.metadata.DTGroup;
import ru.dimension.db.metadata.DataType;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CSType;
import ru.dimension.tt.swing.icon.RowIconProvider;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.DBType;
import ru.dimension.ui.model.type.ConnectionStatus;
import ru.dimension.ui.view.table.icon.adhoc.ConnectionStatusIcon;
import ru.dimension.ui.view.table.icon.adhoc.DBTypeIcon;
import ru.dimension.ui.view.table.icon.adhoc.TableIcon;
import ru.dimension.ui.view.table.icon.adhoc.TimestampIcon;
import ru.dimension.ui.view.table.icon.adhoc.ViewIcon;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.ConnectionTemplateRow;
import ru.dimension.ui.view.table.row.Rows.DesignRow;
import ru.dimension.ui.view.table.row.Rows.EntityRow;
import ru.dimension.ui.view.table.row.Rows.MetadataRow;
import ru.dimension.ui.view.table.row.Rows.MetricRow;
import ru.dimension.ui.view.table.row.Rows.PickableQueryRow;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;
import ru.dimension.ui.view.table.row.Rows.QueryTableRow;
import ru.dimension.ui.view.table.row.Rows.TaskLinkRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;
import ru.dimension.ui.view.table.row.Rows.TemplateConnectionRow;
import ru.dimension.ui.view.table.row.Rows.TemplateMetricRow;
import ru.dimension.ui.view.table.row.Rows.TemplateQueryRow;
import ru.dimension.ui.view.table.row.Rows.TemplateTaskRow;
import ru.dimension.ui.view.table.row.Rows.TimestampColumnRow;
import ru.dimension.ui.view.table.row.Rows.TimestampRow;

public class ModelIconProviders {

  private static final Color PROFILE_COLOR = new Color(0x3B82F6);
  private static final Color TASK_COLOR    = new Color(0xEAB308);
  private static final Color QUERY_COLOR   = new Color(0xEF4444);
  private static final Color DESIGN_COLOR  = new Color(0x8B5CF6);
  private static final Color METRIC_COLOR  = new Color(0x10B981);

  private static final RowIconProvider.RowIcon DEFAULT_ROW_ICON =
      new RowIconProvider.RowIcon(new DTGroupIcon(null), null);

  private static final RowIconProvider.RowIcon DEFAULT_CONNECTION_ICON =
      new RowIconProvider.RowIcon(new DBTypeIcon(null), "Database Connection");

  private ModelIconProviders() {}

  public static RowIconProvider<DesignRow> forDesignRow() {
    return row -> new RowIconProvider.RowIcon(
        new VectorIcons.Design(DESIGN_COLOR),
        row != null ? row.getName() : "Design Configuration"
    );
  }

  public static RowIconProvider<ProfileRow> forProfileRow() {
    return row -> new RowIconProvider.RowIcon(
        new VectorIcons.Profile(PROFILE_COLOR),
        row != null ? row.getName() : "Profile"
    );
  }

  public static RowIconProvider<TaskRow> forTaskRow() {
    return row -> new RowIconProvider.RowIcon(
        new VectorIcons.Task(TASK_COLOR),
        row != null ? row.getName() : "Task"
    );
  }

  public static RowIconProvider<TemplateTaskRow> forTemplateTaskRow() {
    return row -> new RowIconProvider.RowIcon(
        new VectorIcons.Task(TASK_COLOR),
        row != null ? row.getName() : "Task"
    );
  }

  public static RowIconProvider<QueryRow> forQueryRow() {
    return row -> new RowIconProvider.RowIcon(
        new VectorIcons.Query(QUERY_COLOR),
        row != null ? row.getName() : "Query"
    );
  }

  public static RowIconProvider<QueryTableRow> forQueryTableRow() {
    return row -> new RowIconProvider.RowIcon(
        new VectorIcons.Query(QUERY_COLOR),
        row != null ? row.getName() : "Query"
    );
  }

  public static RowIconProvider<TemplateQueryRow> forTemplateQueryRow() {
    return row -> new RowIconProvider.RowIcon(
        new VectorIcons.Query(QUERY_COLOR),
        row != null ? row.getName() : "Query"
    );
  }

  public static RowIconProvider<PickableQueryRow> forPickableQueryRow() {
    return row -> new RowIconProvider.RowIcon(
        new VectorIcons.Query(QUERY_COLOR),
        row != null ? row.getName() : "Query"
    );
  }

  public static RowIconProvider<TaskLinkRow> forTaskLinkRow() {
    return row -> new RowIconProvider.RowIcon(
        new VectorIcons.Task(TASK_COLOR),
        row != null ? row.getName() : "Task"
    );
  }

  public static RowIconProvider<TimestampColumnRow> forTimestampColumnRow() {
    return row -> {
      if (row == null) return DEFAULT_ROW_ICON;
      String name = row.getColName();

      if (row.hasOrigin()) {
        DTGroup group = extractDTGroup(row.getOrigin());
        String typeInfo = getTooltipForDTGroup(group);
        String tooltip = (name != null && !name.isBlank())
            ? name + " (" + typeInfo + ")"
            : typeInfo;
        return new RowIconProvider.RowIcon(new DTGroupIcon(group), tooltip);
      }

      return new RowIconProvider.RowIcon(new TimestampIcon(), name);
    };
  }

  public static RowIconProvider<ColumnRow> forColumnRow() {
    return row -> {
      if (row == null) return DEFAULT_ROW_ICON;
      String name = row.getName();

      if (!row.hasOrigin()) {
        return new RowIconProvider.RowIcon(new DTGroupIcon(null), name);
      }

      DTGroup group = extractDTGroup(row.getOrigin());
      String typeInfo = getTooltipForDTGroup(group);
      String tooltip = (name != null && !name.isBlank())
          ? name + " (" + typeInfo + ")"
          : typeInfo;

      return new RowIconProvider.RowIcon(new DTGroupIcon(group), tooltip);
    };
  }

  public static RowIconProvider<MetricRow> forMetricRow() {
    return row -> {
      if (row == null) return DEFAULT_ROW_ICON;
      String name = row.getName();

      if (!row.hasOrigin()) {
        return new RowIconProvider.RowIcon(new DTGroupIcon(null), name);
      }

      Metric metric = row.getOrigin();
      CProfile yAxis = metric.getYAxis();
      if (yAxis == null) return DEFAULT_ROW_ICON;

      DTGroup group = extractDTGroup(yAxis);
      String typeInfo = getTooltipForDTGroup(group);
      String tooltip = (name != null && !name.isBlank())
          ? name + " (" + typeInfo + ")"
          : typeInfo;

      return new RowIconProvider.RowIcon(new DTGroupIcon(group), tooltip);
    };
  }

  public static RowIconProvider<TemplateMetricRow> forTemplateMetricRow() {
    return row -> {
      if (row == null) return DEFAULT_ROW_ICON;
      String name = row.getName();

      if (row.hasOrigin() && row.getOrigin().getYAxis() != null) {
        DTGroup group = extractDTGroup(row.getOrigin().getYAxis());
        String typeInfo = getTooltipForDTGroup(group);
        String tooltip = (name != null && !name.isBlank())
            ? name + " (" + typeInfo + ")"
            : typeInfo;
        return new RowIconProvider.RowIcon(new DTGroupIcon(group), tooltip);
      }

      return new RowIconProvider.RowIcon(
          new VectorIcons.Metric(METRIC_COLOR),
          name
      );
    };
  }

  public static RowIconProvider<MetadataRow> forMetadataRow() {
    return row -> {
      if (row == null) return DEFAULT_ROW_ICON;
      String name = row.getColName();

      if (!row.hasOrigin()) {
        return new RowIconProvider.RowIcon(new DTGroupIcon(null), name);
      }

      DTGroup group = extractDTGroup(row.getOrigin());
      String typeInfo = getTooltipForDTGroup(group);
      String tooltip = (name != null && !name.isBlank())
          ? name + " (" + typeInfo + ")"
          : typeInfo;

      return new RowIconProvider.RowIcon(new DTGroupIcon(group), tooltip);
    };
  }

  public static RowIconProvider<ConnectionRow> forConnectionRow(Function<Integer, DBType> dbTypeLookup) {
    return row -> {
      if (row == null) return DEFAULT_CONNECTION_ICON;

      DBType dbType = dbTypeLookup != null ? dbTypeLookup.apply(row.getId()) : null;
      String name = row.getName();
      String typeInfo = getTooltipForDBType(dbType);
      String tooltip = (name != null && !name.isBlank()) ? name + " [" + typeInfo + "]" : typeInfo;

      return new RowIconProvider.RowIcon(
          new DBTypeIcon(dbType),
          tooltip
      );
    };
  }

  public static RowIconProvider<ConnectionRow> forConnectionRow() {
    return row -> {
      if (row == null) return DEFAULT_CONNECTION_ICON;

      DBType dbType = row.getDbType();
      String name = row.getName();
      String typeInfo = getTooltipForDBType(dbType);
      String tooltip = (name != null && !name.isBlank()) ? name + " [" + typeInfo + "]" : typeInfo;

      return new RowIconProvider.RowIcon(
          new DBTypeIcon(dbType),
          tooltip
      );
    };
  }

  public static RowIconProvider<TemplateConnectionRow> forTemplateConnectionRow() {
    return row -> {
      if (row == null) return DEFAULT_CONNECTION_ICON;

      DBType dbType = row.getDbType();
      if (dbType == null) dbType = DBType.UNKNOWN;

      String name = row.getName();
      String typeInfo = getTooltipForDBType(dbType);
      String tooltip = (name != null && !name.isBlank()) ? name + " [" + typeInfo + "]" : typeInfo;

      return new RowIconProvider.RowIcon(
          new DBTypeIcon(dbType),
          tooltip
      );
    };
  }

  public static RowIconProvider<EntityRow> forTableRow() {
    return row -> new RowIconProvider.RowIcon(
        new TableIcon(),
        row != null ? row.getName() : "Database Table"
    );
  }

  public static RowIconProvider<EntityRow> forViewRow() {
    return row -> new RowIconProvider.RowIcon(
        new ViewIcon(),
        row != null ? row.getName() : "Database View"
    );
  }

  public static RowIconProvider<TimestampRow> forTimestampRow() {
    return row -> new RowIconProvider.RowIcon(
        new TimestampIcon(),
        row != null ? row.getName() : "Timestamp Column"
    );
  }

  public static RowIconProvider<ConnectionTemplateRow> forConnectionTemplateRow() {
    return row -> {
      if (row == null) return DEFAULT_CONNECTION_ICON;

      DBType dbType = guessDbType(row);

      String name = row.getName();
      String typeInfo = getTooltipForDBType(dbType);
      String tooltip = (name != null && !name.isBlank())
          ? name + " [" + typeInfo + "]"
          : typeInfo;

      return new RowIconProvider.RowIcon(new DBTypeIcon(dbType), tooltip);
    };
  }

  public static RowIconProvider.RowIcon forConnectionStatus(ConnectionStatus status) {
    return new RowIconProvider.RowIcon(
        new ConnectionStatusIcon(status),
        ConnectionStatusIcon.getTooltip(status)
    );
  }

  private static DBType guessDbType(ConnectionTemplateRow row) {
    String blob = row.getType() + " " +
        row.getUrl() + " " +
        row.getDriver() + " " +
        row.getJar() + " " +
        row.getHttpMethod();

    String s = blob.toLowerCase();

    if (s.contains("http")) return DBType.HTTP;
    if (s.contains("postgres")) return DBType.POSTGRES;
    if (s.contains("oracle")) return DBType.ORACLE;
    if (s.contains("sqlserver") || s.contains("mssql")) return DBType.MSSQL;
    if (s.contains("clickhouse")) return DBType.CLICKHOUSE;
    if (s.contains("mysql")) return DBType.MYSQL;
    if (s.contains("duckdb")) return DBType.DUCKDB;
    if (s.contains("firebird")) return DBType.FIREBIRD;

    return DBType.UNKNOWN;
  }

  public static DTGroup extractDTGroup(CProfile cProfile) {
    if (cProfile == null) return null;
    CSType csType = cProfile.getCsType();
    if (csType == null) return null;
    DataType dType = csType.getDType();
    if (dType == null) return null;
    return dType.getGroup();
  }

  public static String getTooltipForDTGroup(DTGroup group) {
    if (group == null) return "Unknown type";
    return "Type: " + group.name();
  }

  public static String getTooltipForDBType(DBType dbType) {
    if (dbType == null) return "Unknown Database";
    return switch (dbType) {
      case ORACLE -> "Oracle Database";
      case POSTGRES -> "PostgreSQL";
      case MSSQL -> "Microsoft SQL Server";
      case CLICKHOUSE -> "ClickHouse";
      case MYSQL -> "MySQL";
      case DUCKDB -> "DuckDB";
      case FIREBIRD -> "Firebird";
      case HTTP -> "HTTP/REST API";
      case UNKNOWN -> "Unknown Database";
    };
  }
}