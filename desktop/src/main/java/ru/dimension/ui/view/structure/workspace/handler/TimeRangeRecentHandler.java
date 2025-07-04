package ru.dimension.ui.view.structure.workspace.handler;

import static ru.dimension.ui.model.view.ProcessType.HISTORY;
import static ru.dimension.ui.model.view.ProcessTypeWorkspace.VISUALIZE;
import static ru.dimension.ui.model.view.RangeHistory.CUSTOM;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.router.listener.AppCacheAddListener;
import ru.dimension.ui.view.structure.workspace.query.DetailsControlPanel;
import ru.dimension.ui.view.tab.HistoryTab;
import ru.dimension.ui.view.tab.RealTimeTab;
import ru.dimension.ui.view.tab.TaskTab;
import ru.dimension.ui.cache.AppCache;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.info.gui.RangeInfo;

@Log4j2
public class TimeRangeRecentHandler extends ChartHandler
    implements ListSelectionListener, AppCacheAddListener {

  private final JXTableCase jxTableCaseRecent;

  private LocalDateTime begin;
  private LocalDateTime end;

  @Inject
  @Named("appCache")
  AppCache appCache;

  public TimeRangeRecentHandler(JXTableCase jxTableCaseMetrics,
                                JXTableCase jxTableCaseColumns,
                                JXTableCase jxTableCaseRecent,
                                ProfileTaskQueryKey profileTaskQueryKey,
                                TaskTab taskTab,
                                RealTimeTab realTimeTab,
                                HistoryTab historyTab,
                                JSplitPane visualizeRealTime,
                                JSplitPane visualizeHistory,
                                JPanel analyzeRealTime,
                                JPanel analyzeHistory,
                                ChartInfo chartInfo,
                                QueryInfo queryInfo,
                                TableInfo tableInfo,
                                DetailsControlPanel detailsControlPanel,
                                WorkspaceQueryComponent workspaceQueryComponent) {
    super(taskTab, realTimeTab, historyTab, jxTableCaseMetrics, jxTableCaseColumns, tableInfo, queryInfo, chartInfo,
          profileTaskQueryKey, visualizeRealTime, visualizeHistory, analyzeRealTime, analyzeHistory, detailsControlPanel, workspaceQueryComponent);

    this.jxTableCaseRecent = jxTableCaseRecent;
    this.jxTableCaseRecent.getJxTable().getSelectionModel().addListSelectionListener(this);
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();
    if (listSelectionModel.equals(this.jxTableCaseRecent.getJxTable().getSelectionModel())) {
      if (!e.getValueIsAdjusting()) {
        int[] selectedRows = this.jxTableCaseRecent.getJxTable().getSelectedRows();

        for (int i = 0; i < selectedRows.length; i++) {
          int selIndex = selectedRows[i];
          Object value = this.jxTableCaseRecent
              .getJxTable().getModel().getValueAt(selIndex, 0);
          String beginS = this.jxTableCaseRecent
              .getJxTable().getModel().getValueAt(selIndex, 2).toString();
          String endS = this.jxTableCaseRecent
              .getJxTable().getModel().getValueAt(selIndex, 3).toString();

          DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss",
                                                                    Locale.ENGLISH);
          begin = LocalDateTime.parse(beginS, formatter);
          end = LocalDateTime.parse(endS, formatter);
        }

        if (listSelectionModel.isSelectionEmpty()) {
          log.info("Clearing range fields");
        } else {
          taskTab.setSelectedTab(HISTORY);
          historyTab.setSelectedTab(VISUALIZE);
          chartInfo.setRangeHistory(CUSTOM);

          this.chartInfo.setCustomBegin(
              begin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
          this.chartInfo.setCustomEnd(end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

          this.loadChart(HISTORY);
        }
      }
    }
  }

  @Override
  public void fireOnAddToAppCache(ProfileTaskQueryKey profileTaskQueryKey) {
    List<RangeInfo> rangeInfoList = appCache.getRangeInfo(profileTaskQueryKey);

    this.jxTableCaseRecent.getDefaultTableModel().getDataVector().removeAllElements();
    this.jxTableCaseRecent.getDefaultTableModel().fireTableDataChanged();

    List<RangeInfo> reverseInfoList = reverseList(rangeInfoList);

    for (RangeInfo range : reverseInfoList) {
      this.jxTableCaseRecent.getDefaultTableModel()
          .addRow(new Object[]{LocalDateTime.ofInstant(Instant.ofEpochMilli(range.getCreatedAt()),
                                                       TimeZone.getDefault().toZoneId())
              .format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss", Locale.ENGLISH)),
              range.getSourceTab(), LocalDateTime.ofInstant(Instant.ofEpochMilli(range.getBegin()),
                                                            TimeZone.getDefault().toZoneId())
              .format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss", Locale.ENGLISH)),
              LocalDateTime.ofInstant(Instant.ofEpochMilli(range.getEnd()), TimeZone.getDefault().toZoneId())
                  .format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss", Locale.ENGLISH))});
    }
  }

  private static <T> List<T> reverseList(List<T> list) {
    List<T> reverse = new ArrayList<>(list);
    Collections.reverse(reverse);
    return reverse;
  }
}

