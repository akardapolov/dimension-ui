package ru.dimension.ui.model.config;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.db.TimestampType;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.function.NormFunction;
import ru.dimension.ui.model.function.TimeRangeFunction;
import ru.dimension.ui.model.info.TableInfo;

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
  private GroupFunction groupFunction; //NONE, SUM, COUNT, AVG
  private TimeRangeFunction timeRangeFunction = TimeRangeFunction.AUTO; // AUTO, MINUTE, HOUR, DAY, MONTH
  private NormFunction normFunction = NormFunction.SECOND; // NONE, SECOND, MINUTE, HOUR, DAY
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
      this.groupFunction = GroupFunction.COUNT;
      this.chartType = ChartType.STACKED;
    } else {
      if (Arrays.stream(TimestampType.values())
          .anyMatch((t) -> t.name().equals(cProfile.getColDbTypeName()))) {
        this.groupFunction = GroupFunction.COUNT;
        this.chartType = ChartType.STACKED;
      } else {
        this.groupFunction = GroupFunction.AVG;
        this.chartType = ChartType.LINEAR;
      }

      this.timeRangeFunction = TimeRangeFunction.AUTO;
      this.normFunction = NormFunction.SECOND;
    }
  }

  public Metric(TableInfo tableInfo, CProfile cProfile, GroupFunction groupFunction, ChartType chartType) {
    this.id = RANDOM.nextInt(Integer.MAX_VALUE);
    this.name = cProfile.getColName();
    this.isDefault = false;

    this.xAxis = tableInfo.getCProfiles().stream()
        .filter(f -> f.getCsType().isTimeStamp())
        .findAny()
        .orElseThrow();
    this.yAxis = cProfile;
    this.group = cProfile;

    this.groupFunction = groupFunction;
    this.timeRangeFunction = TimeRangeFunction.AUTO;
    this.normFunction = NormFunction.SECOND;
    this.chartType = chartType;
  }

  public Metric copy() {
    Metric copy = new Metric();
    copy.setId(this.id);
    copy.setName(this.name);
    copy.setIsDefault(this.isDefault);

    copy.setXAxis(this.xAxis);
    copy.setYAxis(this.yAxis);
    copy.setGroup(this.group);

    copy.setGroupFunction(this.groupFunction);
    copy.setTimeRangeFunction(this.timeRangeFunction);
    copy.setNormFunction(this.normFunction);
    copy.setChartType(this.chartType);

    if (this.columnGanttList != null) {
      copy.setColumnGanttList(List.copyOf(this.columnGanttList));
    }

    return copy;
  }
}
