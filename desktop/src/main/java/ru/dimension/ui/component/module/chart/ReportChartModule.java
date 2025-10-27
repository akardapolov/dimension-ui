package ru.dimension.ui.component.module.chart;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.module.chart.report.ReportChartModel;
import ru.dimension.ui.component.module.chart.report.ReportChartPresenter;
import ru.dimension.ui.component.module.chart.report.ReportChartView;
import ru.dimension.ui.component.panel.popup.DescriptionPanel;
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

@Log4j2
public class ReportChartModule extends JXTaskPane {

  private final MessageBroker.Component component;

  @Getter
  private final ReportChartModel model;
  @Getter
  private final ReportChartView view;
  @Getter
  private final ReportChartPresenter presenter;

  public ReportChartModule(MessageBroker.Component component,
                           ChartKey chartKey,
                           ProfileTaskQueryKey key,
                           Metric metric,
                           QueryInfo queryInfo,
                           ChartInfo chartInfo,
                           TableInfo tableInfo,
                           DStore dStore) {
    this.component = component;

    this.model = new ReportChartModel(component, chartKey, key, metric, queryInfo, chartInfo, tableInfo, dStore);

    DescriptionPanel descriptionPanel = new DescriptionPanel(component,
                                                             chartKey.getProfileTaskQueryKey(),
                                                             chartKey.getCProfile(),
                                                             model.getDescription());

    this.view = new ReportChartView(component, descriptionPanel);
    this.presenter = new ReportChartPresenter(component, model, view);

    ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder());

    this.setAnimated(false);
  }

  public Runnable initializeUI() {
    presenter.initializePresenter();
    presenter.initializeCharts();

    view.getConfigChartSplitPane().setBorder(GUIHelper.getConfigureBorder(1));

    return () -> PGHelper.cellXYRemainder(this, view.getConfigChartSplitPane(), 1, false);
  }

  public void handleLegendChange(ChartLegendState chartLegendState) {
    presenter.handleLegendChangeAll(chartLegendState);
  }

  public void updateHistoryRange(RangeHistory range) {
    presenter.updateHistoryRange(range);
  }

  public void updateHistoryCustomRange(ChartRange range) {
    presenter.updateHistoryCustomRange(range);
  }
}