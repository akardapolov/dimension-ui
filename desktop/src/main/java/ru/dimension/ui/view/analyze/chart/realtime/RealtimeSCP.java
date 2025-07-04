package ru.dimension.ui.view.analyze.chart.realtime;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ru.dimension.db.model.profile.CProfile;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.view.analyze.chart.ChartConfig;
import ru.dimension.ui.view.analyze.chart.SCP;
import ru.dimension.ui.view.chart.FunctionDataHandler;

public abstract class RealtimeSCP extends SCP {
  protected static final int THRESHOLD_SERIES = 10;
  protected static final int SHOW_SERIES = 5;

  protected FunctionDataHandler dataHandler;
  protected Map.Entry<CProfile, List<String>> filter;
  protected JXTableCase seriesSelectable;
  protected ExecutorService executorService = Executors.newSingleThreadExecutor();

  public RealtimeSCP(ChartConfig config, ProfileTaskQueryKey profileTaskQueryKey) {
    super(config, profileTaskQueryKey);
  }

  public RealtimeSCP(ChartConfig config, ProfileTaskQueryKey profileTaskQueryKey,
                     Map.Entry<CProfile, List<String>> filter) {
    super(config, profileTaskQueryKey);
    this.filter = filter;
  }

  protected void setCustomFilter() {
    if (seriesType == SeriesType.CUSTOM) {
      filter = Map.entry(
          config.getMetric().getYAxis(),
          getCheckBoxSelected()
      );
      dataHandler.setFilter(filter);
    }
  }

  protected List<String> getCheckBoxSelected() {
    List<String> selected = new ArrayList<>();
    for (int i = 0; i < seriesSelectable.getJxTable().getRowCount(); i++) {
      if ((Boolean) seriesSelectable.getJxTable().getValueAt(i, 1)) {
        selected.add((String) seriesSelectable.getJxTable().getValueAt(i, 0));
      }
    }
    return selected;
  }

  protected void initializeWithSeriesTable() {
    this.setLayout(new BorderLayout());
    this.add(stackedChart.getChartPanel(), BorderLayout.CENTER);
    this.add(seriesSelectable.getJScrollPane(), BorderLayout.EAST);
  }

  protected abstract void reloadDataForCurrentRange();

  @Override
  public void addChartListenerReleaseMouse(IDetailPanel iDetailPanel) {
    stackedChart.addChartListenerReleaseMouse(iDetailPanel);
  }

  @Override
  public void removeChartListenerReleaseMouse(IDetailPanel iDetailPanel) {
    stackedChart.removeChartListenerReleaseMouse(iDetailPanel);
  }
}