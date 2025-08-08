package ru.dimension.ui.component.module.adhoc;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.module.adhoc.chart.AdHocChartModel;
import ru.dimension.ui.component.module.adhoc.chart.AdHocChartPresenter;
import ru.dimension.ui.component.module.adhoc.chart.AdHocChartView;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.component.model.ChartLegendState;
import ru.dimension.ui.component.model.DetailState;

@Log4j2
public class AdHocChartModule extends JXTaskPane {
  @Getter
  private final AdHocChartView view;
  private final AdHocChartModel model;
  @Getter
  private final AdHocChartPresenter presenter;

  public AdHocChartModule(AdHocKey adHocKey,
                          Metric metric,
                          QueryInfo queryInfo,
                          ChartInfo chartInfo,
                          TableInfo tableInfo,
                          DStore dStore) {
    this.model = new AdHocChartModel(adHocKey, metric, queryInfo, chartInfo, tableInfo, dStore);
    this.view = new AdHocChartView();
    this.presenter = new AdHocChartPresenter(model, view);

    this.setAnimated(false);
  }

  public Runnable initializeUI() {
    presenter.initializeChart();
    return () -> PGHelper.cellXYRemainder(this, view.getTabbedPane(), false);
  }

  public void loadData() {
    this.presenter.initializeChart();
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