package ru.dimension.ui.component.chart;

import lombok.Getter;
import lombok.Setter;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.state.ChartKey;

@Getter
@Setter
public class ChartConfig {
  private ChartKey chartKey;

  private String title;
  private String xAxisLabel;
  private String yAxisLabel;

  private int legendFontSize = 12;

  private boolean legend = false;
  private boolean tooltips = true;
  private boolean urls = false;

  private Metric metric;
  private ChartInfo chartInfo;
  private QueryInfo queryInfo;

  private boolean realTime = true;
  private int maxPointsPerGraph = 300;
}
