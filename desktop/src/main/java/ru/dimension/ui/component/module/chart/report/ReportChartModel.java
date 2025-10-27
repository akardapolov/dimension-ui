package ru.dimension.ui.component.module.chart.report;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTextArea;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.module.chart.main.ChartModel;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.state.ChartKey;

@Log4j2
public class ReportChartModel extends ChartModel {

  @Getter
  private final Component component;

  @Getter
  private final JXTextArea description;

  public ReportChartModel(Component component,
                          ChartKey chartKey,
                          ProfileTaskQueryKey key,
                          Metric metric,
                          QueryInfo queryInfo,
                          ChartInfo chartInfo,
                          TableInfo tableInfo,
                          DStore dStore) {
    super(chartKey, key, metric, queryInfo, chartInfo, tableInfo, null, dStore);
    this.component = component;
    this.description = GUIHelper.getJXTextArea(2, 1);
  }
}