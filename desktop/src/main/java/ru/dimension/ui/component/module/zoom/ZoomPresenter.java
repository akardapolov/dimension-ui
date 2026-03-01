package ru.dimension.ui.component.module.zoom;

import java.util.concurrent.ConcurrentMap;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.module.chart.PRChartModule;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.router.listener.CollectStartStopListener;

@Log4j2
public class ZoomPresenter implements CollectStartStopListener {

  private final ZoomModel model;
  private final ZoomView view;

  private ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, PRChartModule>> chartPanesRef;

  public ZoomPresenter(ZoomModel model, ZoomView view) {
    this.model = model;
    this.view = view;
    view.setOnOpenAction(this::openZoomDashboard);
  }

  public void setChartPanesRef(
      ConcurrentMap<ProfileTaskQueryKey, ConcurrentMap<CProfile, PRChartModule>> chartPanes) {
    this.chartPanesRef = chartPanes;
  }

  private void openZoomDashboard() {
    if (chartPanesRef != null) {
      view.openDashboard(chartPanesRef);
    }
  }

  @Override
  public void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Zoom: Start collect for {}", profileTaskQueryKey);
  }

  @Override
  public void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    if (!view.isZoomDashboardOpen()) {
      return;
    }
    view.markDirty(profileTaskQueryKey);
  }
}