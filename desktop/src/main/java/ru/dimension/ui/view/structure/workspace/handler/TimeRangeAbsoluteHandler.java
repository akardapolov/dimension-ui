package ru.dimension.ui.view.structure.workspace.handler;

import static ru.dimension.ui.model.view.ProcessType.HISTORY;
import static ru.dimension.ui.model.view.ProcessTypeWorkspace.VISUALIZE;
import static ru.dimension.ui.model.view.RangeHistory.CUSTOM;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.cache.AppCache;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.structure.workspace.query.DetailsControlPanel;
import ru.dimension.ui.view.tab.HistoryTab;
import ru.dimension.ui.view.tab.RealTimeTab;
import ru.dimension.ui.view.tab.TaskTab;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.info.gui.RangeInfo;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.panel.TimeRangeAbsolutePanel;

@Log4j2
public class TimeRangeAbsoluteHandler extends ChartHandler implements ActionListener {

  private final TimeRangeAbsolutePanel timeRangeAbsolutePanel;

  private LocalDateTime begin;
  private LocalDateTime end;

  @Inject
  @Named("eventListener")
  EventListener eventListener;

  @Inject
  @Named("appCache")
  AppCache appCache;

  public TimeRangeAbsoluteHandler(JXTableCase jxTableCaseMetrics,
                                  JXTableCase jxTableCaseColumns,
                                  TimeRangeAbsolutePanel timeRangeAbsolutePanel,
                                  TaskTab taskTab,
                                  RealTimeTab realTimeTab,
                                  HistoryTab historyTab,
                                  ProfileTaskQueryKey profileTaskQueryKey,
                                  JSplitPane visualizeRealTime,
                                  JSplitPane visualizeHistory,
                                  JPanel analyzeRealTime,
                                  JPanel analyzeHistory,
                                  QueryInfo queryInfo,
                                  TableInfo tableInfo,
                                  ChartInfo chartInfo,
                                  DetailsControlPanel detailsControlPanel,
                                  WorkspaceQueryComponent workspaceQueryComponent) {
    super(taskTab, realTimeTab, historyTab, jxTableCaseMetrics, jxTableCaseColumns, tableInfo, queryInfo, chartInfo,
          profileTaskQueryKey, visualizeRealTime, visualizeHistory, analyzeRealTime, analyzeHistory, detailsControlPanel, workspaceQueryComponent);

    this.timeRangeAbsolutePanel = timeRangeAbsolutePanel;

    this.begin = LocalDateTime.now();
    this.end = LocalDateTime.now();

    this.timeRangeAbsolutePanel.getJButtonFrom().addActionListener(this);
    this.timeRangeAbsolutePanel.getJButtonTo().addActionListener(this);
    this.timeRangeAbsolutePanel.getJButtonGo().addActionListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == timeRangeAbsolutePanel.getJButtonFrom()) {
      timeRangeAbsolutePanel.getDatePickerFrom().setDate(LocalDate.now());
    }

    if (e.getSource() == timeRangeAbsolutePanel.getJButtonTo()) {
      timeRangeAbsolutePanel.getDatePickerTo().setDate(LocalDate.now());
    }

    if (e.getSource() == timeRangeAbsolutePanel.getJButtonGo()) {

      taskTab.setSelectedTab(HISTORY);
      historyTab.setSelectedTab(VISUALIZE);
      chartInfo.setRangeHistory(CUSTOM);

      LocalDate dateBegin = timeRangeAbsolutePanel.getDatePickerFrom().getDate();
      LocalDate dateEnd = timeRangeAbsolutePanel.getDatePickerTo().getDate();

      LocalTime startOfDay = LocalTime.MIN;
      LocalTime endOfDay = LocalTime.MAX;

      begin = LocalDateTime.of(dateBegin, startOfDay);
      end = LocalDateTime.of(dateEnd, endOfDay);

      this.chartInfo.setCustomBegin(begin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
      this.chartInfo.setCustomEnd(end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

      this.loadChart(HISTORY);

      RangeInfo rangeInfo = new RangeInfo();
      long selectionIndex = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
      rangeInfo.setCreatedAt(selectionIndex);
      rangeInfo.setBegin(begin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
      rangeInfo.setEnd(end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
      rangeInfo.setSourceTab("A");

      appCache.putRangeInfo(profileTaskQueryKey, rangeInfo);

      eventListener.fireOnAddToAppCache(profileTaskQueryKey);
    }
  }
}