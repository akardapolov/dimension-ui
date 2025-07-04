package ru.dimension.ui.view.chart.stacked;

import javax.inject.Inject;
import javax.inject.Named;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.ui.config.prototype.chart.WorkspaceChartModule;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.listener.CollectStartStopListener;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.chart.FunctionDataHandler;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.view.ProcessType;

@Log4j2
public abstract class StackChartPanel extends StackChartPanelCommon implements CollectStartStopListener {

  private final WorkspaceQueryComponent workspaceQueryComponent;

  protected FunctionDataHandler dataHandler;

  @Inject
  @Named("eventListener")
  EventListener eventListener;

  @Inject
  @Named("profileManager")
  ProfileManager profileManager;

  @Inject
  @Named("localDB")
  DStore dStore;

  @Inject
  @Named("sqlQueryState")
  SqlQueryState sqlQueryState;

  public StackChartPanel(WorkspaceQueryComponent workspaceQueryComponent,
                         CategoryTableXYDatasetRealTime chartDataset,
                         ProfileTaskQueryKey profileTaskQueryKey,
                         QueryInfo queryInfo,
                         ChartInfo chartInfo,
                         ProcessType processType,
                         Metric metric) {
    super(chartDataset,
          profileTaskQueryKey,
          queryInfo,
          chartInfo,
          processType,
          metric);
    this.workspaceQueryComponent = workspaceQueryComponent;
    this.workspaceQueryComponent.initChart(new WorkspaceChartModule(this)).inject(this);

    this.dataHandler = initFunctionDataHandler(metric, queryInfo, dStore);

    if (metric.getYAxis().getCsType() == null) {
      throw new NotFoundException("Column storage type is undefined for column profile: " + metric.getYAxis());
    }
  }

  public abstract void initialize();

  public abstract void loadData();

  @Override
  public void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Start collect for " + profileTaskQueryKey);
  }

  @Override
  public void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Stop collect for " + profileTaskQueryKey);

    try {
      this.loadData();
    } catch (Exception e) {
      log.catching(e);
    }
  }

  @Override
  public void addChartListenerReleaseMouse(IDetailPanel l) {
    stackedChart.addChartListenerReleaseMouse(l);
  }

  @Override
  public void removeChartListenerReleaseMouse(IDetailPanel l) {
    stackedChart.removeChartListenerReleaseMouse(l);
  }
}
