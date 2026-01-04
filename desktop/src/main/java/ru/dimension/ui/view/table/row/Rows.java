package ru.dimension.ui.view.table.row;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CSType;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.db.model.profile.cstype.SType;
import ru.dimension.tt.annotation.ColumnKind;
import ru.dimension.tt.annotation.TTColumn;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.DBType;
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

    @TTColumn(name = "Type", order = 2, editable = false)
    private String type;

    private DBType dbType;

    public ConnectionRow(int id, String name) {
      this(id, name, (ConnectionType) null);
    }

    public ConnectionRow(int id, String name, ConnectionType connectionType) {
      this.id = id;
      this.name = name;
      this.type = connectionType != null ? connectionType.getName() : ConnectionType.JDBC.getName();
    }

    public ConnectionRow(int id, String name, String type) {
      this.id = id;
      this.name = name;
      this.type = type;
    }

    public ConnectionRow(int id, String name, ConnectionType connectionType, DBType dbType) {
      this.id = id;
      this.name = name;
      this.type = connectionType != null ? connectionType.getName() : ConnectionType.JDBC.getName();
      this.dbType = dbType;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public DBType getDbType() { return dbType; }
    public void setDbType(DBType dbType) { this.dbType = dbType; }
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
}