package ru.dimension.ui.model.report;

import java.util.Objects;
import lombok.Data;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.function.GroupFunction;

@Data
public class CProfileReport extends CProfile {
  private String comment;
  private GroupFunction groupFunction;
  private ChartType chartType;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    CProfileReport that = (CProfileReport) o;
    return Objects.equals(comment, that.comment) &&
        groupFunction == that.groupFunction &&
        chartType == that.chartType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), comment, groupFunction, chartType);
  }
}