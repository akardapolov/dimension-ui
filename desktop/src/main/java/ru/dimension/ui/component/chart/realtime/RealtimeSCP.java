package ru.dimension.ui.component.chart.realtime;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.FunctionDataHandler;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.table.JXTableCase;

public abstract class RealtimeSCP extends SCP {

  protected FunctionDataHandler dataHandler;
  protected JXTableCase seriesSelectable;
  protected ExecutorService executorService = Executors.newSingleThreadExecutor();

  public RealtimeSCP(ChartConfig config, ProfileTaskQueryKey profileTaskQueryKey) {
    super(config, profileTaskQueryKey);
  }

  protected List<String> getCheckBoxSelected() {
    List<String> selected = new ArrayList<>();
    int modelRowCount = seriesSelectable.getDefaultTableModel().getRowCount();
    for (int modelRow = 0; modelRow < modelRowCount; modelRow++) {
      if ((Boolean) seriesSelectable.getDefaultTableModel().getValueAt(modelRow, 1)) {
        selected.add((String) seriesSelectable.getDefaultTableModel().getValueAt(modelRow, 0));
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

  public abstract Map<CProfile, LinkedHashSet<String>> getFilter();

  @Override
  public void addChartListenerReleaseMouse(IDetailPanel iDetailPanel) {
    stackedChart.addChartListenerReleaseMouse(iDetailPanel);
  }

  @Override
  public void removeChartListenerReleaseMouse(IDetailPanel iDetailPanel) {
    stackedChart.removeChartListenerReleaseMouse(iDetailPanel);
  }
}