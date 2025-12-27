package ru.dimension.ui.component.module.model.row;

import lombok.NoArgsConstructor;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.tt.annotation.ColumnKind;
import ru.dimension.tt.annotation.TTColumn;
import ru.dimension.ui.model.config.Metric;

public class Rows {

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

    // NOT transient - we need this to survive
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

    // NOT transient - we need this to survive
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
}