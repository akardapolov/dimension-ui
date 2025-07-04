package ru.dimension.ui.view.structure.workspace.handler;

import static ru.dimension.ui.model.view.ProcessType.HISTORY;
import static ru.dimension.ui.model.view.ProcessType.REAL_TIME;
import static ru.dimension.ui.model.view.RangeChartRealTime.FIVE_MIN;
import static ru.dimension.ui.model.view.RangeChartRealTime.SIXTY_MIN;
import static ru.dimension.ui.model.view.RangeChartRealTime.TEN_MIN;
import static ru.dimension.ui.model.view.RangeChartRealTime.THIRTY_MIN;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.state.GUIState;
import ru.dimension.ui.view.chart.HelperChart;
import ru.dimension.ui.view.structure.workspace.query.CustomHistoryPanel;
import ru.dimension.ui.view.structure.workspace.query.DetailsControlPanel;
import ru.dimension.ui.view.tab.HistoryTab;
import ru.dimension.ui.view.tab.RealTimeTab;
import ru.dimension.ui.view.tab.TaskTab;
import ru.dimension.ui.cache.AppCache;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.info.gui.RangeInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeChartRealTime;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.view.panel.RangeChartHistoryPanel;
import ru.dimension.ui.view.panel.RangeChartRealTimePanel;

@Log4j2
public class TimeRangeQuickHandler extends ChartHandler
    implements ActionListener, HelperChart {

  private final RangeChartRealTimePanel rangeChartRealTimePanel;
  private final RangeChartHistoryPanel rangeChartHistoryPanel;

  private final CustomHistoryPanel customHistoryPanel;

  @Inject
  @Named("appCache")
  AppCache appCache;

  public TimeRangeQuickHandler(JXTableCase jxTableCaseMetrics,
                               JXTableCase jxTableCaseColumns,
                               RangeChartRealTimePanel rangeChartRealTimePanel,
                               RangeChartHistoryPanel rangeChartHistoryPanel,
                               JSplitPane visualizeRealTime,
                               JSplitPane visualizeHistory,
                               JPanel analyzeRealTime,
                               JPanel analyzeHistory,
                               TaskTab taskTab,
                               RealTimeTab realTimeTab,
                               HistoryTab historyTab,
                               CustomHistoryPanel customHistoryPanel,
                               ProfileTaskQueryKey profileTaskQueryKey,
                               QueryInfo queryInfo,
                               TableInfo tableInfo,
                               ChartInfo chartInfo,
                               DetailsControlPanel detailsControlPanel,
                               WorkspaceQueryComponent workspaceQueryComponent) {
    super(taskTab, realTimeTab, historyTab, jxTableCaseMetrics, jxTableCaseColumns, tableInfo, queryInfo, chartInfo,
          profileTaskQueryKey, visualizeRealTime, visualizeHistory, analyzeRealTime, analyzeHistory, detailsControlPanel, workspaceQueryComponent);

    this.customHistoryPanel = customHistoryPanel;

    this.rangeChartRealTimePanel = rangeChartRealTimePanel;
    this.rangeChartRealTimePanel.addActionListener(this);
    this.rangeChartHistoryPanel = rangeChartHistoryPanel;
    this.rangeChartHistoryPanel.addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    log.info("Action command here: " + e.getActionCommand());
    String name = e.getActionCommand().replace("Last ", "");

    if (Stream.of(RangeChartRealTime.values()).anyMatch(v -> v.getName().equals(name))) {

      if (FIVE_MIN.getName().equalsIgnoreCase(name)) {
        this.chartInfo.setRangeRealtime(RangeRealTime.FIVE_MIN);
        colorButton(5);
      }
      if (TEN_MIN.getName().equalsIgnoreCase(name)) {
        this.chartInfo.setRangeRealtime(RangeRealTime.TEN_MIN);
        colorButton(10);
      }
      if (THIRTY_MIN.getName().equalsIgnoreCase(name)) {
        this.chartInfo.setRangeRealtime(RangeRealTime.THIRTY_MIN);
        colorButton(30);
      }
      if (SIXTY_MIN.getName().equalsIgnoreCase(name)) {
        this.chartInfo.setRangeRealtime(RangeRealTime.SIXTY_MIN);
        colorButton(60);
      }

      this.loadChart(REAL_TIME);
    }

    Stream.of(RangeHistory.values())
        .filter(v -> v.getName().equals(name))
        .forEach(this.chartInfo::setRangeHistory);

    if (Stream.of(RangeHistory.values()).anyMatch(v -> v.getName().equals(name))) {
      taskTab.setSelectedTab(HISTORY);
      historyTab.setSelectedTab(GUIState.getInstance().getHistoryTabState());
      colorButtonHistory(name);

      if (name.equals(RangeHistory.CUSTOM.getName())) {
        customHistoryPanel.setVisible(true);
        return;
      }

      ChartRange chartRange = getChartRange(chartInfo);
      long begin = chartRange.getBegin();
      long end = chartRange.getEnd();

      RangeInfo rangeInfo = new RangeInfo();
      long selectionIndex = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
      rangeInfo.setCreatedAt(selectionIndex);
      rangeInfo.setBegin(begin);
      rangeInfo.setEnd(end);
      rangeInfo.setSourceTab("Q");

      appCache.putRangeInfo(profileTaskQueryKey, rangeInfo);

      eventListener.fireOnAddToAppCache(profileTaskQueryKey);

      this.loadChart(HISTORY);
    }
  }

  private void colorButtonHistory(String name) {
    switch (name) {
      case "Day" -> this.rangeChartHistoryPanel.setButtonColor(colorBlack, colorBlue, colorBlue, colorBlue);
      case "Week" -> this.rangeChartHistoryPanel.setButtonColor(colorBlue, colorBlack, colorBlue, colorBlue);
      case "Month" -> this.rangeChartHistoryPanel.setButtonColor(colorBlue, colorBlue, colorBlack, colorBlue);
      case "Custom" -> this.rangeChartHistoryPanel.setButtonColor(colorBlue, colorBlue, colorBlue, colorBlack);
    }
  }

  private void colorButton(int numMin) {
    switch (numMin) {
      case 5 -> this.rangeChartRealTimePanel.setButtonColor(colorBlack, colorBlue, colorBlue, colorBlue);
      case 10 -> this.rangeChartRealTimePanel.setButtonColor(colorBlue, colorBlack, colorBlue, colorBlue);
      case 30 -> this.rangeChartRealTimePanel.setButtonColor(colorBlue, colorBlue, colorBlack, colorBlue);
      case 60 -> this.rangeChartRealTimePanel.setButtonColor(colorBlue, colorBlue, colorBlue, colorBlack);
    }
  }
}
