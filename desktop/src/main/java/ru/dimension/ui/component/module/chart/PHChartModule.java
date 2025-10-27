package ru.dimension.ui.component.module.chart;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.chart.preview.DetailChartContext;
import ru.dimension.ui.component.module.chart.preview.history.PHChartModel;
import ru.dimension.ui.component.module.chart.preview.history.PHChartPresenter;
import ru.dimension.ui.component.module.chart.preview.history.PHChartView;
import ru.dimension.ui.component.module.preview.spi.IHistoryPreviewChart;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class PHChartModule extends JXTaskPane implements IHistoryPreviewChart {

  private final PHChartModel model;
  @Getter
  private final PHChartView view;
  @Getter
  private final PHChartPresenter presenter;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public PHChartModule(MessageBroker.Component component,
                       ChartKey chartKey,
                       ProfileTaskQueryKey key,
                       Metric metric,
                       QueryInfo queryInfo,
                       ChartInfo chartInfo,
                       TableInfo tableInfo,
                       SqlQueryState sqlQueryState,
                       DStore dStore,
                       DetailChartContext detailContext) {
    this.model = new PHChartModel(chartKey, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);
    this.view = new PHChartView(component);
    this.presenter = new PHChartPresenter(component, model, view, executor, detailContext);
    this.presenter.initializePresenter();
    this.presenter.initializeCharts();

    ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder());
    this.setAnimated(false);
  }

  public Runnable initializeUI() {
    view.getConfigChartSplitPane().setBorder(GUIHelper.getConfigureBorder(1));

    return () -> PGHelper.cellXYRemainder(this, view.getConfigChartSplitPane(), 1, false);
  }

  public void loadData() { }

  public void handleLegendChange(ChartLegendState chartLegendState) {
    presenter.handleLegendChange(ChartLegendState.SHOW.equals(chartLegendState));
  }

  public void handleHistoryRangeUI(RangeHistory range) {
    presenter.handleHistoryRangeChangeUI(range);
  }

  public void handleHistoryRange(RangeHistory range) {
    presenter.handleHistoryRangeChange("", range);
  }

  public void handleHistoryCustomRange(ChartRange range) {
    presenter.handleHistoryCustomRange(range);
  }

  public void handleChartConfigState(ChartConfigState chartConfigState) {
    presenter.handleChartConfigState(chartConfigState);
  }

  @Override
  public JXTaskPane asTaskPane() {
    return this;
  }
}