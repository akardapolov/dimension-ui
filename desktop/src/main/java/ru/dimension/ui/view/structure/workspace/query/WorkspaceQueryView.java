package ru.dimension.ui.view.structure.workspace.query;

import java.awt.event.KeyEvent;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.table.TableColumn;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryModule;
import ru.dimension.ui.config.prototype.task.WorkspaceTaskComponent;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.DialogHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.RunStatus;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.column.QueryMetadataColumnNames;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.panel.QuerySearchButtonPanel;
import ru.dimension.ui.view.panel.RangeChartCustomPanel;
import ru.dimension.ui.view.panel.RangeChartHistoryPanel;
import ru.dimension.ui.view.panel.RangeChartRealTimePanel;
import ru.dimension.ui.view.panel.TimeRangeAbsolutePanel;
import ru.dimension.ui.view.panel.TimeRangeRecentPanel;
import ru.dimension.ui.view.panel.TimeRangeRelativePanel;
import ru.dimension.ui.view.structure.workspace.handler.DetailsControlPanelHandler;
import ru.dimension.ui.view.structure.workspace.handler.MetricColumnSelectionHandler;
import ru.dimension.ui.view.structure.workspace.handler.QuerySearchHandler;
import ru.dimension.ui.view.structure.workspace.handler.TimeRangeAbsoluteHandler;
import ru.dimension.ui.view.structure.workspace.handler.TimeRangeQuickHandler;
import ru.dimension.ui.view.structure.workspace.handler.TimeRangeRecentHandler;
import ru.dimension.ui.view.structure.workspace.handler.TimeRangeRelativeHandler;
import ru.dimension.ui.view.structure.workspace.task.QueryMetricColumnContainer;
import ru.dimension.ui.view.tab.HistoryTab;
import ru.dimension.ui.view.tab.RealTimeTab;
import ru.dimension.ui.view.tab.TaskTab;

@Log4j2
public class WorkspaceQueryView extends JPanel {

  private final ProfileTaskQueryKey profileTaskQueryKey;
  private final JSplitPane visualizeRealTime;
  private final JSplitPane visualizeHistory;
  private final JPanel analyzeRealTime;
  private final JPanel analyzeHistory;
  private final JSplitPane analyzeSearch;

  private final QueryMetricColumnContainer listCardQueryMetricColumn;

  private final JXTableCase jxTableCaseMetrics;
  private final JXTableCase jxTableCaseColumns;
  private final JXTableCase jxTableCaseRecent;

  private final DetailsControlPanel detailsControlPanel;
  private final DetailsControlPanelHandler detailsControlPanelHandler;

  private final WorkspaceTaskComponent workspaceTaskComponent;
  private final WorkspaceQueryComponent workspaceQueryComponent;

  private final RangeChartRealTimePanel rangeChartRealTimePanel;
  private final RangeChartHistoryPanel rangeChartHistoryPanel;
  private final RangeChartCustomPanel rangeChartCustomPanel;
  private final TimeRangeRelativePanel timeRangeRelativePanel;
  private final TimeRangeAbsolutePanel timeRangeAbsolutePanel;
  private final TimeRangeRecentPanel timeRangeRecentPanel;
  private final QuerySearchButtonPanel querySearchButtonPanel;

  private final MetricColumnSelectionHandler metricColumnSelectionHandler;
  private final TimeRangeQuickHandler timeRangeQuickHandler;
  private final TimeRangeRelativeHandler timeRangeRelativeHandler;
  private final TimeRangeAbsoluteHandler timeRangeAbsoluteHandler;
  private final TimeRangeRecentHandler timeRangeRecentHandler;
  private final QuerySearchHandler querySearchHandler;
  private final CustomHistoryPanel customHistoryPanel;
  private final JTabbedPane jTabbedPane;

  @Inject
  @Named("eventListener")
  EventListener eventListener;

  @Inject
  @Named("profileManager")
  ProfileManager profileManager;

  public WorkspaceQueryView(ProfileTaskQueryKey profileTaskQueryKey,
                            JSplitPane visualizeRealTime,
                            JSplitPane visualizeHistory,
                            JPanel analyzeRealTime,
                            JPanel analyzeHistory,
                            JSplitPane analyzeSearch,
                            TaskTab taskTab,
                            RealTimeTab realTimeTab,
                            HistoryTab historyTab,
                            QueryMetricColumnContainer listCardQueryMetricColumn,
                            WorkspaceTaskComponent workspaceTaskComponent) {
    this.workspaceTaskComponent = workspaceTaskComponent;
    this.workspaceQueryComponent = this.workspaceTaskComponent.initQuery(new WorkspaceQueryModule(this));
    this.workspaceQueryComponent.inject(this);

    this.listCardQueryMetricColumn = listCardQueryMetricColumn;

    this.profileTaskQueryKey = profileTaskQueryKey;

    this.visualizeRealTime = visualizeRealTime;
    this.visualizeHistory = visualizeHistory;
    this.analyzeRealTime = analyzeRealTime;
    this.analyzeHistory = analyzeHistory;
    this.analyzeSearch = analyzeSearch;

    this.jxTableCaseMetrics = getJXTableCaseMetrics();
    this.jxTableCaseColumns = getJXTableCaseColumns();
    this.jxTableCaseRecent = getJXTableCaseRecent();

    this.detailsControlPanel = new DetailsControlPanel();
    this.detailsControlPanelHandler = new DetailsControlPanelHandler(this.detailsControlPanel);

    this.rangeChartRealTimePanel = new RangeChartRealTimePanel();
    this.rangeChartRealTimePanel.setEnabled(true);
    this.rangeChartHistoryPanel = new RangeChartHistoryPanel();
    this.rangeChartHistoryPanel.setEnabled(true);
    this.rangeChartCustomPanel = new RangeChartCustomPanel();
    this.rangeChartCustomPanel.setEnabled(true);
    this.timeRangeRelativePanel = new TimeRangeRelativePanel();
    this.timeRangeRelativePanel.setEnabled(true);
    this.timeRangeAbsolutePanel = new TimeRangeAbsolutePanel();
    this.timeRangeAbsolutePanel.setEnabled(true);
    this.timeRangeRecentPanel = new TimeRangeRecentPanel(this.jxTableCaseRecent);
    this.timeRangeRecentPanel.setEnabled(true);
    this.querySearchButtonPanel = new QuerySearchButtonPanel();
    this.querySearchButtonPanel.setEnabled(true);

    this.listCardQueryMetricColumn.addMetricToCard(this.jxTableCaseMetrics);
    this.listCardQueryMetricColumn.addColumnToCard(this.jxTableCaseColumns);

    this.customHistoryPanel = new CustomHistoryPanel();
    this.jTabbedPane = new JTabbedPane();

    ProfileInfo profileInfo = profileManager.getProfileInfoById(profileTaskQueryKey.getProfileId());

    QueryInfo queryInfo = profileManager.getQueryInfoById(profileTaskQueryKey.getQueryId());
    if (Objects.isNull(queryInfo)) {
      throw new NotFoundException(String.format("Query info with id=%s not found",
                                                profileTaskQueryKey.getQueryId()));
    }

    if (RunStatus.NOT_RUNNING.equals(profileInfo.getStatus()) & queryInfo.getDeltaLocalServerTime() == 0) {
      try {
        if (!GatherDataMode.BY_CLIENT_HTTP.equals(queryInfo.getGatherDataMode())) {
          profileManager.loadDeltaLocalServerTime(profileTaskQueryKey);
        }
      } catch (Exception e) {
        DialogHelper.showErrorDialog(null, e.getMessage(), "General Connection Error", e);
      }

      queryInfo = profileManager.getQueryInfoById(profileTaskQueryKey.getQueryId());
    }

    TableInfo tableInfo = profileManager.getTableInfoByTableName(queryInfo.getName());
    if (Objects.isNull(tableInfo)) {
      throw new NotFoundException(String.format("Table info with id=%s not found",
                                                queryInfo.getName()));
    }

    ChartInfo chartInfo = profileManager.getChartInfoById(queryInfo.getId());
    if (Objects.isNull(chartInfo)) {
      throw new NotFoundException(String.format("Chart info with id=%s not found",
                                                queryInfo.getId()));
    }

    chartInfo.setRangeRealtime(RangeRealTime.TEN_MIN);
    profileManager.updateChart(chartInfo);

    this.metricColumnSelectionHandler = new MetricColumnSelectionHandler(taskTab, realTimeTab, historyTab,
                                                                         profileTaskQueryKey, queryInfo, tableInfo, chartInfo,
                                                                         jxTableCaseMetrics, jxTableCaseColumns,
                                                                         this.visualizeRealTime, this.visualizeHistory, this.analyzeRealTime, this.analyzeHistory,
                                                                         detailsControlPanel, detailsControlPanelHandler, workspaceQueryComponent);

    this.timeRangeQuickHandler = new TimeRangeQuickHandler(jxTableCaseMetrics, jxTableCaseColumns,
                                                           rangeChartRealTimePanel, rangeChartHistoryPanel,
                                                           this.visualizeRealTime, this.visualizeHistory, this.analyzeRealTime, this.analyzeHistory,
                                                           taskTab, realTimeTab, historyTab, customHistoryPanel, profileTaskQueryKey, queryInfo, tableInfo, chartInfo,
                                                           detailsControlPanel, workspaceQueryComponent);

    this.timeRangeRelativeHandler = new TimeRangeRelativeHandler(jxTableCaseMetrics, jxTableCaseColumns,
                                                                 timeRangeRelativePanel, taskTab, realTimeTab, historyTab,
                                                                 this.visualizeRealTime, this.visualizeHistory, this.analyzeRealTime, this.analyzeHistory,
                                                                 profileTaskQueryKey, queryInfo, tableInfo, chartInfo, detailsControlPanel, workspaceQueryComponent);

    this.timeRangeAbsoluteHandler = new TimeRangeAbsoluteHandler(jxTableCaseMetrics, jxTableCaseColumns,
                                                                 timeRangeAbsolutePanel, taskTab, realTimeTab, historyTab, profileTaskQueryKey,
                                                                 this.visualizeRealTime, this.visualizeHistory, this.analyzeRealTime, this.analyzeHistory,
                                                                 queryInfo, tableInfo, chartInfo, detailsControlPanel, workspaceQueryComponent);

    this.eventListener.clearListenerAppCacheByKey(profileTaskQueryKey);
    this.timeRangeRecentHandler = new TimeRangeRecentHandler(jxTableCaseMetrics, jxTableCaseColumns, jxTableCaseRecent,
                                                             profileTaskQueryKey, taskTab, realTimeTab, historyTab,
                                                             this.visualizeRealTime, this.visualizeHistory, this.analyzeRealTime, this.analyzeHistory,
                                                             chartInfo, queryInfo, tableInfo, detailsControlPanel, workspaceQueryComponent);
    this.eventListener.addAppCacheAddListener(profileTaskQueryKey, this.timeRangeRecentHandler);

    this.querySearchHandler = new QuerySearchHandler(analyzeSearch, querySearchButtonPanel,
                                                     profileTaskQueryKey, queryInfo, tableInfo, chartInfo, workspaceQueryComponent);

    this.loadColumns(queryInfo, tableInfo);

    this.workspaceQueryComponent.injectMetricColumnSelectionHandler(this.metricColumnSelectionHandler);
    this.workspaceQueryComponent.injectTimeRangeQuickHandler(this.timeRangeQuickHandler);
    this.workspaceQueryComponent.injectTimeRangeRelativeHandler(this.timeRangeRelativeHandler);
    this.workspaceQueryComponent.injectTimeRangeAbsoluteHandler(this.timeRangeAbsoluteHandler);
    this.workspaceQueryComponent.injectTimeRangeRecentHandler(this.timeRangeRecentHandler);
    this.workspaceQueryComponent.injectSearchHandler(this.querySearchHandler);

    this.fillCustomHistoryPanel();

    JPanel panelEntities = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(panelEntities, PGHelper.getPGConfig(0), false);

    gbl.row()
        .cellX(new JXTitledSeparator("Metrics"), 2).fillX(4)
        .cellX(new JXTitledSeparator("Columns"), 2).fillX(4)
        .cellX(new JXTitledSeparator("Details"), 2).fillX(4)
        .cellX(new JXTitledSeparator("Real-time"), 1).fillX(1)
        .cellX(new JXTitledSeparator("History"), 1).fillX(1);

    gbl.row()
        .cellX(listCardQueryMetricColumn.getJXTableCaseMetric().getJScrollPane(), 2).fillXY(4, 3)
        .cellX(listCardQueryMetricColumn.getJXTableCaseColumn().getJScrollPane(), 2).fillXY(4, 3)
        .cellX(detailsControlPanel, 2).fillXY(4, 3)
        .cellX(rangeChartRealTimePanel, 1).fillXY(1, 1)
        .cellX(rangeChartHistoryPanel, 1).fillXY(1, 1);

    gbl.done();

    PainlessGridBag gblThis = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gblThis.row().cellXRemainder(panelEntities).fillXY();

    gblThis.done();

    this.eventListener.fireOnAddToAppCache(profileTaskQueryKey);
  }

  private void fillCustomHistoryPanel() {
    jTabbedPane.add("Relative", timeRangeRelativePanel);
    jTabbedPane.add("Absolute", timeRangeAbsolutePanel);
    jTabbedPane.add("Recent", timeRangeRecentPanel);
    jTabbedPane.setMnemonicAt(0, KeyEvent.VK_R);
    jTabbedPane.setMnemonicAt(1, KeyEvent.VK_A);
    jTabbedPane.setMnemonicAt(2, KeyEvent.VK_E);

    customHistoryPanel.add(jTabbedPane);
  }

  private JXTableCase getJXTableCaseMetrics() {
    JXTableCase jxTableCase =
        GUIHelper.getJXTableCase(3,
                                 new String[]{
                                     MetricsColumnNames.ID.getColName(),
                                     MetricsColumnNames.NAME.getColName()
                                 });
    jxTableCase.getJxTable().setSortable(false);

    return jxTableCase;
  }

  private JXTableCase getJXTableCaseColumns() {
    JXTableCase jxTableCase =
        GUIHelper.getJXTableCase(7,
                                 new String[]{
                                     QueryMetadataColumnNames.ID.getColName(),
                                     QueryMetadataColumnNames.NAME.getColName()
                                 });
    jxTableCase.getJxTable().setSortable(false);

    return jxTableCase;
  }

  private JXTableCase getJXTableCaseRecent() {
    JXTableCase jxTableCase = GUIHelper.getJXTableCase(4, new String[]{"Created at", "S", "Begin", "End"});
    jxTableCase.getJxTable().getColumnExt(0).setVisible(false);

    TableColumn col = jxTableCase.getJxTable().getColumnModel().getColumn(0);
    col.setMinWidth(10);
    col.setMaxWidth(15);

    return jxTableCase;
  }

  private void loadColumns(QueryInfo queryInfo,
                           TableInfo tableInfo) {
    jxTableCaseMetrics.getJxTable().getColumnExt(0).setVisible(false);
    jxTableCaseColumns.getJxTable().getColumnExt(0).setVisible(false);

    jxTableCaseMetrics.getJxTable().getColumnModel().getColumn(0)
        .setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());
    jxTableCaseColumns.getJxTable().getColumnModel().getColumn(0)
        .setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());

    if (queryInfo.getMetricList() != null && !queryInfo.getMetricList().isEmpty()) {
      queryInfo.getMetricList()
          .forEach(metric -> jxTableCaseMetrics.getDefaultTableModel()
              .addRow(new Object[]{metric.getId(), metric.getName()}));
    } else {
      log.warn("No metrics found for " + queryInfo);
    }

    if (tableInfo.getCProfiles() != null) {
      tableInfo.getCProfiles().stream()
          .filter(f -> !f.getCsType().isTimeStamp())
          .forEach(cProfile -> jxTableCaseColumns.getDefaultTableModel()
              .addRow(new Object[]{cProfile.getColId(), cProfile.getColName()}));
    } else {
      log.warn("Not columns found for " + queryInfo);
    }

    jxTableCaseMetrics.getJxTable().packAll();
    jxTableCaseColumns.getJxTable().packAll();
  }
}
