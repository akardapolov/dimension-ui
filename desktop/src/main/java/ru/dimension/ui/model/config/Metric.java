package ru.dimension.ui.model.config;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.db.TimestampType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Metric {
  private static final Random RANDOM = new Random();

  private int id;
  private String name;
  private Boolean isDefault;

  private CProfile xAxis; // x-axis
  private CProfile yAxis; // y-axis
  private CProfile group; // group
  private MetricFunction metricFunction; //NONE, SUM, COUNT, AVG
  private ChartType chartType; // linear, stacked

  private List<CProfile> columnGanttList;

  public Metric(TableInfo tableInfo, CProfile cProfile) {
    this.id = RANDOM.nextInt(Integer.MAX_VALUE);
    this.name = cProfile.getColName();
    this.isDefault = false;

    this.xAxis = tableInfo.getCProfiles().stream()
        .filter(f -> f.getCsType().isTimeStamp())
        .findAny()
        .orElseThrow();
    this.yAxis = cProfile;
    this.group = cProfile;

    if (CType.STRING.equals(cProfile.getCsType().getCType())) {
      this.metricFunction = MetricFunction.COUNT;
      this.chartType = ChartType.STACKED;
    } else {
      if (Arrays.stream(TimestampType.values())
          .anyMatch((t) -> t.name().equals(cProfile.getColDbTypeName()))) {
        this.metricFunction = MetricFunction.COUNT;
        this.chartType = ChartType.STACKED;
      } else {
        this.metricFunction = MetricFunction.AVG;
        this.chartType = ChartType.LINEAR;
      }
    }
  }

  public Metric(TableInfo tableInfo, CProfile cProfile, MetricFunction metricFunction, ChartType chartType) {
    this.id = RANDOM.nextInt(Integer.MAX_VALUE);
    this.name = cProfile.getColName();
    this.isDefault = false;

    this.xAxis = tableInfo.getCProfiles().stream()
        .filter(f -> f.getCsType().isTimeStamp())
        .findAny()
        .orElseThrow();
    this.yAxis = cProfile;
    this.group = cProfile;

    this.metricFunction = metricFunction;
    this.chartType = chartType;
  }

  public boolean isStackedYAxisSameCount() {
    return chartType.equals(ChartType.STACKED)
        & yAxis.equals(group)
        & metricFunction.equals(MetricFunction.COUNT);
  }

  public boolean isLinearYAxisSameSum() {
    return chartType.equals(ChartType.LINEAR)
        & yAxis.equals(group)
        & metricFunction.equals(MetricFunction.SUM);
  }

  public boolean isLinearYAxisAvg() {
    return chartType.equals(ChartType.LINEAR)
        & yAxis.equals(group)
        & metricFunction.equals(MetricFunction.AVG);
  }

  public Metric copy() {
    Metric copy = new Metric();
    copy.setId(this.id);
    copy.setName(this.name);
    copy.setIsDefault(this.isDefault);

    copy.setXAxis(this.xAxis);
    copy.setYAxis(this.yAxis);
    copy.setGroup(this.group);

    copy.setMetricFunction(this.metricFunction);
    copy.setChartType(this.chartType);

    if (this.columnGanttList != null) {
      copy.setColumnGanttList(List.copyOf(this.columnGanttList));
    }

    return copy;
  }
}
