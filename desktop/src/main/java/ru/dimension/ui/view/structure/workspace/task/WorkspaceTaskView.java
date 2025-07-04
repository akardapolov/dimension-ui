package ru.dimension.ui.view.structure.workspace.task;

import java.awt.GridLayout;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.config.prototype.profile.WorkspaceProfileComponent;
import ru.dimension.ui.config.prototype.task.WorkspaceTaskComponent;
import ru.dimension.ui.config.prototype.task.WorkspaceTaskModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.ProfileTaskKey;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.structure.workspace.handler.QuerySelectionInTaskHandler;
import ru.dimension.ui.view.tab.HistoryTab;
import ru.dimension.ui.view.tab.RealTimeTab;
import ru.dimension.ui.view.tab.TaskTab;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.view.ProcessTypeWorkspace;
import ru.dimension.ui.view.chart.stacked.StackChartPanel;

@Log4j2
public class WorkspaceTaskView extends JPanel {

  private final JSplitPane sqlPane;
  private final ProfileTaskKey profileTaskKey;
  private final JSplitPane queryListAndMetadataPanel;

  private final JSplitPane visualizeRealTime;
  private final JSplitPane visualizeHistory;
  private final JPanel analyzeRealTime;
  private final JPanel analyzeHistory;
  private final JSplitPane analyzeSearch;

  private final JXTableCase jXTableCaseQuery;

  private final TaskTab taskTab;

  private final JPanel queryMetadataPanel;
  private final QuerySelectionInTaskHandler querySelectionInTaskHandler;

  private final QueryMetricColumnContainer queryMetricColumnContainer;

  private final WorkspaceProfileComponent workspaceProfileComponent;
  private final WorkspaceTaskComponent workspaceTaskComponent;
  private final RealTimeTab realTimeTab;
  private final HistoryTab historyTab;

  @Inject
  @Named("eventListener")
  EventListener eventListener;

  @Inject
  @Named("profileManager")
  ProfileManager profileManager;

  public WorkspaceTaskView(JSplitPane sqlPane,
                           ProfileTaskKey profileTaskKey,
                           WorkspaceProfileComponent workspaceProfileComponent) {
    this.workspaceProfileComponent = workspaceProfileComponent;
    this.workspaceTaskComponent = this.workspaceProfileComponent.initTask(new WorkspaceTaskModule(this));
    this.workspaceTaskComponent.inject(this);

    this.sqlPane = sqlPane;

    this.profileTaskKey = profileTaskKey;

    this.jXTableCaseQuery = getJXTableCase();

    this.queryMetadataPanel = new JPanel(new GridLayout(1, 1));
    this.queryMetadataPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    this.queryMetadataPanel.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(LafColorGroup.CHART_PANEL, queryMetadataPanel);

    this.queryListAndMetadataPanel = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 120);
    this.visualizeRealTime = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 250);
    this.visualizeHistory = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 250);
    this.analyzeRealTime = getPanelLaF();
    this.analyzeHistory = getPanelLaF();

    this.analyzeSearch = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 40);

    JPanel analyzeSearchTop = new JPanel();
    analyzeSearchTop.setBorder(new EtchedBorder());
    JPanel analyzeSearchBottom = getPanelLaF();
    this.analyzeSearch.add(analyzeSearchTop, JSplitPane.TOP);
    this.analyzeSearch.add(analyzeSearchBottom, JSplitPane.BOTTOM);
    this.analyzeSearch.setDividerLocation(40);

    this.realTimeTab = new RealTimeTab();
    this.realTimeTab.add(ProcessTypeWorkspace.VISUALIZE.getName(), visualizeRealTime);
    this.realTimeTab.add(ProcessTypeWorkspace.ANALYZE.getName(), analyzeRealTime);
    this.realTimeTab.add(ProcessTypeWorkspace.SEARCH.getName(), analyzeSearch);

    this.historyTab = new HistoryTab();
    this.historyTab.add(ProcessTypeWorkspace.VISUALIZE.getName(), visualizeHistory);
    this.historyTab.add(ProcessTypeWorkspace.ANALYZE.getName(), analyzeHistory);

    this.taskTab = new TaskTab();
    this.taskTab.add("Real-time", realTimeTab);
    this.taskTab.add("History", historyTab);

    this.queryMetricColumnContainer = new QueryMetricColumnContainer();
    this.queryMetricColumnContainer.addQueryToCard(jXTableCaseQuery);

    this.queryListAndMetadataPanel.add(queryMetricColumnContainer.getJXTableCaseQuery()
                                           .getJScrollPane(), JSplitPane.LEFT);
    this.queryListAndMetadataPanel.add(queryMetadataPanel, JSplitPane.RIGHT);

    this.sqlPane.add(queryListAndMetadataPanel, JSplitPane.TOP);
    this.sqlPane.add(taskTab, JSplitPane.BOTTOM);

    this.eventListener.clearListener(StackChartPanel.class);

    this.querySelectionInTaskHandler =
        new QuerySelectionInTaskHandler(jXTableCaseQuery, queryMetricColumnContainer, queryMetadataPanel,
                                        visualizeRealTime, visualizeHistory, analyzeRealTime, analyzeHistory, analyzeSearch,
                                        taskTab, realTimeTab, historyTab, profileTaskKey, this.workspaceTaskComponent,
                                        this.eventListener, this.profileManager);
  }

  public void loadSql() {
    jXTableCaseQuery.getJxTable().getColumnExt(0).setVisible(false);
    jXTableCaseQuery.getJxTable().getColumnModel().getColumn(0)
        .setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());

    profileManager.getQueryInfoList(profileTaskKey.getProfileId(), profileTaskKey.getTaskId())
        .forEach(queryInfo -> jXTableCaseQuery.getDefaultTableModel()
            .addRow(new Object[]{queryInfo.getId(), queryInfo.getName()}));

    JPanel topRealTime = getPanelLaF();
    JPanel bottomRealTime = getPanelLaF();

    visualizeRealTime.add(topRealTime, JSplitPane.TOP);
    visualizeRealTime.add(bottomRealTime, JSplitPane.BOTTOM);

    JPanel topHistory = getPanelLaF();
    JPanel bottomHistory = getPanelLaF();

    visualizeHistory.add(topHistory, JSplitPane.TOP);
    visualizeHistory.add(bottomHistory, JSplitPane.BOTTOM);

    analyzeRealTime.removeAll();

    JPanel analyzeHistoryLeft = getPanelLaF();
    JPanel analyzeHistoryRight = getPanelLaF();

    analyzeHistory.add(analyzeHistoryLeft, JSplitPane.LEFT);
    analyzeHistory.add(analyzeHistoryRight, JSplitPane.RIGHT);

    visualizeRealTime.setDividerLocation(250);
  }

  private JXTableCase getJXTableCase() {
    JXTableCase jxTableCase = GUIHelper.getJXTableCase(5,
                                                       new String[]{QueryColumnNames.ID.getColName(),
                                                           QueryColumnNames.FULL_NAME.getColName()});
    jxTableCase.getJxTable().getTableHeader().setVisible(true);
    jxTableCase.getJxTable().setSortable(false);

    return jxTableCase;
  }

  private JPanel getPanelLaF() {
    JPanel panel = new JPanel();
    panel.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(LafColorGroup.CHART_PANEL, panel);
    return panel;
  }
}
