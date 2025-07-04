package ru.dimension.ui.view.analyze.chart.adhoc;

import lombok.extern.log4j.Log4j2;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.view.chart.FunctionDataHandler;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.view.chart.stacked.StackChartPanelCommon;

@Log4j2
public abstract class StackChartAdHocPanel extends StackChartPanelCommon {

  protected FunctionDataHandler dataHandler;

  public StackChartAdHocPanel(CategoryTableXYDatasetRealTime chartDataset,
                              ChartInfo chartInfo,
                              QueryInfo queryInfo,
                              Metric metric) {
    super(chartDataset,
          null,
          queryInfo,
          chartInfo,
          ProcessType.ADHOC,
          metric);

    if (metric.getYAxis().getCsType() == null) {
      throw new NotFoundException("Column storage type is undefined for column profile: " + metric.getYAxis());
    }
  }

  @Override
  public abstract void initialize();

  @Override
  public abstract void loadData();

  @Override
  public void addChartListenerReleaseMouse(IDetailPanel l) {
    stackedChart.addChartListenerReleaseMouse(l);
  }

  @Override
  public void removeChartListenerReleaseMouse(IDetailPanel l) {
    stackedChart.removeChartListenerReleaseMouse(l);
  }
}
