package ru.dimension.ui.view.structure.workspace.handler;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.structure.workspace.query.WorkspaceQueryView;
import ru.dimension.ui.view.tab.HistoryTab;
import ru.dimension.ui.view.tab.RealTimeTab;
import ru.dimension.ui.view.tab.TaskTab;
import ru.dimension.ui.config.prototype.task.WorkspaceTaskComponent;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskKey;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.structure.workspace.task.QueryMetricColumnContainer;

@Log4j2
public class QuerySelectionInTaskHandler implements ListSelectionListener {

  private final JXTableCase jXTableCaseQuery;
  private final JPanel workspaceQueryMetadataPanel;
  private final ProfileTaskKey profileTaskKey;
  private final JSplitPane visualizeRealTime;
  private final JSplitPane visualizeHistory;
  private final JSplitPane analyzeSearch;
  private final JPanel analyzeRealTime;
  private final JPanel analyzeHistory;

  private final TaskTab taskTab;
  private final RealTimeTab realTimeTab;
  private final HistoryTab historyTab;

  private final QueryMetricColumnContainer listCardQueryMetricColumn;

  private final WorkspaceTaskComponent workspaceTaskComponent;

  private final EventListener eventListener;
  private final ProfileManager profileManager;

  public QuerySelectionInTaskHandler(JXTableCase jXTableCaseQuery,
                                     QueryMetricColumnContainer listCardQueryMetricColumn,
                                     JPanel workspaceQueryMetadataPanel,
                                     JSplitPane visualizeRealTime,
                                     JSplitPane visualizeHistory,
                                     JPanel analyzeRealTime,
                                     JPanel analyzeHistory,
                                     JSplitPane analyzeSearch,
                                     TaskTab taskTab,
                                     RealTimeTab realTimeTab,
                                     HistoryTab historyTab,
                                     ProfileTaskKey profileTaskKey,
                                     WorkspaceTaskComponent workspaceTaskComponent,
                                     EventListener eventListener,
                                     ProfileManager profileManager) {
    this.jXTableCaseQuery = jXTableCaseQuery;
    this.jXTableCaseQuery.getJxTable().getSelectionModel().addListSelectionListener(this);

    this.listCardQueryMetricColumn = listCardQueryMetricColumn;

    this.workspaceQueryMetadataPanel = workspaceQueryMetadataPanel;
    this.visualizeRealTime = visualizeRealTime;
    this.visualizeHistory = visualizeHistory;
    this.analyzeRealTime = analyzeRealTime;
    this.analyzeHistory = analyzeHistory;
    this.analyzeSearch = analyzeSearch;

    this.taskTab = taskTab;
    this.realTimeTab = realTimeTab;
    this.historyTab = historyTab;

    this.profileTaskKey = profileTaskKey;
    this.workspaceTaskComponent = workspaceTaskComponent;
    this.eventListener = eventListener;
    this.profileManager = profileManager;
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

    // prevents double events
    if (!e.getValueIsAdjusting()) {
      workspaceQueryMetadataPanel.removeAll();

      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing query fields");
      } else {

        this.profileManager
            .getQueryInfoList(profileTaskKey.getProfileId(), profileTaskKey.getTaskId())
            .forEach(queryInfo -> {
              ProfileTaskQueryKey key = new ProfileTaskQueryKey(profileTaskKey.getProfileId(), profileTaskKey.getTaskId(), queryInfo.getId());
              this.eventListener.clearListenerByKey(key);
            });

        int queryId = GUIHelper.getIdByColumnName(this.jXTableCaseQuery.getJxTable(), this.jXTableCaseQuery.getDefaultTableModel(),
                                                  listSelectionModel, QueryColumnNames.ID.getColName());

        ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(profileTaskKey.getProfileId(),
                                                                          profileTaskKey.getTaskId(), queryId);

        WorkspaceQueryView workspaceQueryView = new WorkspaceQueryView(profileTaskQueryKey,
                                                                       visualizeRealTime, visualizeHistory, analyzeRealTime, analyzeHistory, analyzeSearch,
                                                                       taskTab, realTimeTab, historyTab, listCardQueryMetricColumn, workspaceTaskComponent);

        workspaceQueryMetadataPanel.add(workspaceQueryView);

        visualizeRealTime.add(new JPanel(), JSplitPane.TOP);
        visualizeRealTime.add(new JPanel(), JSplitPane.BOTTOM);
        visualizeRealTime.setDividerLocation(250);

        visualizeHistory.add(new JPanel(), JSplitPane.TOP);
        visualizeHistory.add(new JPanel(), JSplitPane.BOTTOM);
        visualizeHistory.setDividerLocation(250);

        analyzeRealTime.removeAll();
        analyzeRealTime.revalidate();
        analyzeRealTime.repaint();
        analyzeHistory.removeAll();
        analyzeHistory.revalidate();
        analyzeHistory.repaint();

        workspaceQueryMetadataPanel.repaint();
        workspaceQueryMetadataPanel.revalidate();

        log.info("Query id: " + queryId);
      }
    }
  }
}
