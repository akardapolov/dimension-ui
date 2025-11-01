package ru.dimension.ui.component.module;

import java.awt.Component;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.module.chart.preview.DetailChartContext;
import ru.dimension.ui.component.module.preview.PreviewModel;
import ru.dimension.ui.component.module.preview.PreviewPresenter;
import ru.dimension.ui.component.module.preview.PreviewView;
import ru.dimension.ui.component.module.preview.container.IPreviewContainer;
import ru.dimension.ui.component.module.preview.container.PreviewDialogContainer;
import ru.dimension.ui.component.module.preview.container.PreviewPanelContainer;
import ru.dimension.ui.component.module.preview.spi.IRealTimePreviewChart;
import ru.dimension.ui.component.module.preview.spi.PreviewChartFactory;
import ru.dimension.ui.component.module.preview.spi.PreviewMode;
import ru.dimension.ui.component.module.preview.spi.RunMode;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.router.listener.CollectStartStopListener;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
@Getter
public class PreviewModule implements CollectStartStopListener {
  private final PreviewModel model;
  private final IPreviewContainer container;
  private final PreviewPresenter presenter;

  public PreviewModule(ProfileTaskQueryKey key,
                       ProfileManager profileManager,
                       SqlQueryState sqlQueryState,
                       DStore dStore) {
    this(PreviewMode.PREVIEW, key, profileManager, sqlQueryState, dStore);
  }

  public PreviewModule(PreviewMode mode,
                       ProfileTaskQueryKey key,
                       ProfileManager profileManager,
                       SqlQueryState sqlQueryState,
                       DStore dStore) {
    QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
    ChartInfo chartInfo = profileManager.getChartInfoById(queryInfo.getId());
    TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());

    this.model = new PreviewModel(RunMode.REALTIME, key, null, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);

    PreviewView previewView = new PreviewView(PreviewMode.PREVIEW, model);

    if (mode == PreviewMode.PREVIEW) {
      ProfileInfo profileInfo = profileManager.getProfileInfoById(key.getProfileId());
      TaskInfo taskInfo = profileManager.getTaskInfoById(key.getTaskId());
      String title = "Preview -> Profile: " + profileInfo.getName() + " >>> Task: " + taskInfo.getName() + " >>> Query: " + queryInfo.getName();
      this.container = new PreviewDialogContainer(title, previewView);
    } else {
      this.container = new PreviewPanelContainer(previewView);
    }

    PreviewChartFactory factory = (mode == PreviewMode.PREVIEW)
        ? PreviewChartFactory.realTime()
        : PreviewChartFactory.history();
    this.presenter = new PreviewPresenter(model, previewView, mode, factory);
  }

  public PreviewModule(RunMode runMode,
                       ProfileTaskQueryKey key,
                       Metric metric,
                       QueryInfo queryInfo,
                       ChartInfo chartInfo,
                       TableInfo tableInfo,
                       SqlQueryState sqlQueryState,
                       DStore dStore,
                       DetailChartContext detailContext) {
    this.model = new PreviewModel(runMode, key, metric, queryInfo, chartInfo, tableInfo, sqlQueryState, dStore);

    PreviewView previewView = new PreviewView(PreviewMode.DETAIL, model);

    this.container = new PreviewPanelContainer(previewView);

    PreviewChartFactory factory = PreviewChartFactory.history(detailContext);
    this.presenter = new PreviewPresenter(model, previewView, PreviewMode.DETAIL, factory);
  }

  public PreviewModule(RunMode runMode,
                       AdHocKey key,
                       Metric metric,
                       QueryInfo queryInfo,
                       ChartInfo chartInfo,
                       TableInfo tableInfo,
                       DStore dStore,
                       DetailChartContext detailContext) {
    this.model = new PreviewModel(runMode, key, metric, queryInfo, chartInfo, tableInfo, null, dStore);

    PreviewView previewView = new PreviewView(PreviewMode.ADHOC, model);

    this.container = new PreviewPanelContainer(previewView);

    PreviewChartFactory factory = PreviewChartFactory.historyAdHoc(detailContext);
    this.presenter = new PreviewPresenter(model, previewView, PreviewMode.ADHOC, factory);
  }

  public void show() {
    this.container.show();
  }

  public Component getComponent() {
    return container.getComponent();
  }

  @Override
  public void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Start collect for {}", profileTaskQueryKey);
  }

  @Override
  public void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Stop collect for {}", profileTaskQueryKey);

    if (model == null || model.getChartModules() == null) return;
    if (model.getKey() instanceof ProfileTaskQueryKey && !model.getKey().equals(profileTaskQueryKey)) return;
    if (model.getChartModules().isEmpty()) return;

    model.getChartModules().forEach((cProfile, chart) -> {
      if (chart instanceof IRealTimePreviewChart rt && rt.isReadyRealTimeUpdate()) {
        try {
          chart.loadData();
        } catch (Exception e) {
          log.error("Error loading data", e);
        }
      }
    });
  }
}