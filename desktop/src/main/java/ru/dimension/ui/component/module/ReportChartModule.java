package ru.dimension.ui.component.module;

import java.awt.Dimension;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.model.DetailState;
import ru.dimension.ui.component.module.report.chart.ReportChartModel;
import ru.dimension.ui.component.module.report.chart.ReportChartPresenter;
import ru.dimension.ui.component.module.report.chart.ReportChartView;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.ChartKey;

@Log4j2
public class ReportChartModule extends JXTaskPane {
  private final Component component;

  @Getter
  private final ReportChartModel model;
  @Getter
  private final ReportChartView view;
  @Getter
  private final ReportChartPresenter presenter;

  public ReportChartModule(Component component,
                           ChartKey chartKey,
                           ProfileTaskQueryKey key,
                           Metric metric,
                           QueryInfo queryInfo,
                           ChartInfo chartInfo,
                           TableInfo tableInfo,
                           DStore dStore) {
    this.component = component;
    this.model = new ReportChartModel(component, chartKey, key, metric, queryInfo, chartInfo, tableInfo, dStore);
    this.view = new ReportChartView(model);
    this.presenter = new ReportChartPresenter(model, view);

    this.setAnimated(false);
  }

  public Runnable initializeUI() {
    presenter.initializeCharts();

    Dimension availableSize = this.getSize();
    if (availableSize.width == 0) {
      availableSize = this.getPreferredSize();
    }

    view.setDimension(new Dimension(availableSize.width, 600));

    return () -> PGHelper.cellXYRemainder(this, view.getHistoryConfigChartDetail(), false);
  }

  public void handleLegendChange(ChartLegendState chartLegendState) {
    presenter.handleLegendChange(ChartLegendState.SHOW.equals(chartLegendState));
  }

  public void setDetailState(DetailState detailState) {
    this.presenter.setDetailState(detailState);
  }

  public void updateHistoryRange(RangeHistory range) {
    this.presenter.updateHistoryRange(range);
  }

  public void updateHistoryCustomRange(ChartRange range) {
    this.presenter.updateHistoryCustomRange(range);
  }
}