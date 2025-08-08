package ru.dimension.ui.component.module;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.module.preview.PreviewModel;
import ru.dimension.ui.component.module.preview.PreviewPresenter;
import ru.dimension.ui.component.module.preview.PreviewView;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.router.listener.CollectStartStopListener;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
@Getter
public class PreviewModule implements CollectStartStopListener {
  private final PreviewModel model;
  private final PreviewView view;
  private final PreviewPresenter presenter;

  public PreviewModule(ProfileTaskQueryKey key,
                       ProfileManager profileManager,
                       SqlQueryState sqlQueryState,
                       DStore dStore) {
    QueryInfo queryInfo = profileManager.getQueryInfoById(key.getQueryId());
    ChartInfo chartInfo = profileManager.getChartInfoById(queryInfo.getId());
    TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());

    this.model = new PreviewModel(key, queryInfo, chartInfo, tableInfo, profileManager, sqlQueryState, dStore);
    this.view = new PreviewView(model);
    this.presenter = new PreviewPresenter(model, view);
  }

  public void show() {
    this.view.packConfig(true);
  }

  @Override
  public void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Start collect for {}", profileTaskQueryKey);
  }

  @Override
  public void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Stop collect for {}", profileTaskQueryKey);

    if (model == null || model.getChartModules() == null) {
      return;
    }

    if (model.getChartModules().isEmpty()) {
      return;
    }

    model.getChartModules().forEach((key, value) -> {
      if (value.isReadyRealTimeUpdate()) {
        value.loadData();
      }
    });
  }
}