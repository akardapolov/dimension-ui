package ru.dimension.ui.component.module.report.chart;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTextArea;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.state.ChartKey;

@Data
@Log4j2
public class ReportChartModel {
  private final Component component;
  private final ChartKey chartKey;
  private final ProfileTaskQueryKey key;
  private final Metric metric;
  private final QueryInfo queryInfo;
  private final ChartInfo chartInfo;
  private final TableInfo tableInfo;
  private final DStore dStore;

  private final JXTextArea description;

  public ReportChartModel(Component component,
                          ChartKey chartKey,
                          ProfileTaskQueryKey key,
                          Metric metric,
                          QueryInfo queryInfo,
                          ChartInfo chartInfo,
                          TableInfo tableInfo,
                          DStore dStore) {
    this.component = component;
    this.chartKey = chartKey;
    this.key = key;
    this.metric = metric;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo.copy();
    this.tableInfo = tableInfo;
    this.dStore = dStore;

    this.description = GUIHelper.getJXTextArea(2, 1);
  }
}