package ru.dimension.ui.component.module.model.icon;

import java.awt.Color;
import ru.dimension.db.metadata.DTGroup;
import ru.dimension.db.metadata.DataType;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CSType;
import ru.dimension.tt.swing.icon.RowIconProvider;
import ru.dimension.ui.component.module.model.row.Rows.ColumnRow;
import ru.dimension.ui.component.module.model.row.Rows.MetricRow;
import ru.dimension.ui.component.module.model.row.Rows.ProfileRow;
import ru.dimension.ui.component.module.model.row.Rows.QueryRow;
import ru.dimension.ui.component.module.model.row.Rows.TaskRow;
import ru.dimension.ui.model.config.Metric;

/**
 * Factory for creating RowIconProvider instances for all model row types.
 *
 * Icon selection logic:
 * - ColumnRow: based on DTGroup from CProfile.csType.dType
 * - MetricRow: based on DTGroup from Metric.yAxis.csType.dType
 * - ProfileRow, TaskRow, QueryRow: simple colored icons
 */
public class ModelIconProviders {

  // Static colors for entity types
  private static final Color PROFILE_COLOR = new Color(0x1976D2);  // Blue
  private static final Color TASK_COLOR    = new Color(0x388E3C);  // Green
  private static final Color QUERY_COLOR   = new Color(0xF57C00);  // Orange

  // Default icon for unknown/null DTGroup
  private static final RowIconProvider.RowIcon DEFAULT_ROW_ICON =
      new RowIconProvider.RowIcon(new DTGroupIcon(null), "Unknown type");

  private ModelIconProviders() {
    // Utility class
  }

  /**
   * Creates an icon provider for ColumnRow based on CProfile's DTGroup.
   */
  public static RowIconProvider<ColumnRow> forColumnRow() {
    return row -> {
      if (row == null || !row.hasOrigin()) {
        return DEFAULT_ROW_ICON;
      }
      DTGroup group = extractDTGroup(row.getOrigin());
      return new RowIconProvider.RowIcon(
          new DTGroupIcon(group),
          getTooltipForDTGroup(group)
      );
    };
  }

  /**
   * Creates an icon provider for MetricRow based on Metric's yAxis DTGroup.
   */
  public static RowIconProvider<MetricRow> forMetricRow() {
    return row -> {
      if (row == null || !row.hasOrigin()) {
        return DEFAULT_ROW_ICON;
      }
      Metric metric = row.getOrigin();
      CProfile yAxis = metric.getYAxis();
      if (yAxis == null) {
        return DEFAULT_ROW_ICON;
      }
      DTGroup group = extractDTGroup(yAxis);
      return new RowIconProvider.RowIcon(
          new DTGroupIcon(group),
          getTooltipForDTGroup(group)
      );
    };
  }

  /**
   * Creates a simple colored icon provider for ProfileRow.
   */
  public static RowIconProvider<ProfileRow> forProfileRow() {
    return row -> new RowIconProvider.RowIcon(
        new SimpleColorIcon(PROFILE_COLOR),
        "Profile"
    );
  }

  /**
   * Creates a simple colored icon provider for TaskRow.
   */
  public static RowIconProvider<TaskRow> forTaskRow() {
    return row -> new RowIconProvider.RowIcon(
        new SimpleColorIcon(TASK_COLOR),
        "Task"
    );
  }

  /**
   * Creates a simple colored icon provider for QueryRow.
   */
  public static RowIconProvider<QueryRow> forQueryRow() {
    return row -> new RowIconProvider.RowIcon(
        new SimpleColorIcon(QUERY_COLOR),
        "Query"
    );
  }

  /**
   * Extracts DTGroup from CProfile.
   *
   * @param cProfile the column profile
   * @return DTGroup or null if cannot be determined
   */
  public static DTGroup extractDTGroup(CProfile cProfile) {
    if (cProfile == null) {
      return null;
    }
    CSType csType = cProfile.getCsType();
    if (csType == null) {
      return null;
    }
    DataType dType = csType.getDType();
    if (dType == null) {
      return null;
    }
    return dType.getGroup();
  }

  /**
   * Extracts DTGroup from Metric's yAxis.
   *
   * @param metric the metric
   * @return DTGroup or null if cannot be determined
   */
  public static DTGroup extractDTGroupFromMetric(Metric metric) {
    if (metric == null) {
      return null;
    }
    return extractDTGroup(metric.getYAxis());
  }

  /**
   * Gets tooltip text for DTGroup.
   */
  public static String getTooltipForDTGroup(DTGroup group) {
    if (group == null) {
      return "Unknown type";
    }
    return "Type: " + group.name();
  }
}