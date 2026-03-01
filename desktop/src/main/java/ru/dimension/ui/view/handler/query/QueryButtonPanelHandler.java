package ru.dimension.ui.view.handler.query;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.QueryListChangedEvent;
import ru.dimension.ui.bus.event.UpdateQueryList;
import ru.dimension.ui.exception.EmptyNameException;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.model.view.handler.LifeCycleStatus;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.dialog.TaskLinkDialog;
import ru.dimension.ui.view.handler.core.ConfigSelectionContext;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.query.MainQueryPanel;
import ru.dimension.ui.view.panel.config.query.MetadataQueryPanel;
import ru.dimension.ui.view.panel.config.query.QueryPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows.QueryRow;

@Log4j2
@Singleton
public final class QueryButtonPanelHandler implements ActionListener {

  private final ProfileManager profileManager;
  private final EventBus eventBus;

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;

  private final QueryPanel queryPanel;
  private final ButtonPanel queryButtonPanel;
  private final ConfigTab configTab;

  private final JTabbedPane mainQuery;
  private final JCheckBox checkboxConfig;

  private final MainQueryPanel mainQueryPanel;
  private final MetadataQueryPanel metadataQueryPanel;

  private final ConfigSelectionContext selectionContext;

  private LifeCycleStatus status;
  private final ResourceBundle bundleDefault;

  private Integer copySourceQueryId;

  @Inject
  public QueryButtonPanelHandler(@Named("profileManager") ProfileManager profileManager,
                                 @Named("eventBus") EventBus eventBus,
                                 @Named("profileConfigCase") JXTableCase profileCase,
                                 @Named("taskConfigCase") JXTableCase taskCase,
                                 @Named("connectionConfigCase") JXTableCase connectionCase,
                                 @Named("queryConfigCase") JXTableCase queryCase,
                                 @Named("queryConfigPanel") QueryPanel queryPanel,
                                 @Named("queryButtonPanel") ButtonPanel queryButtonPanel,
                                 @Named("mainQueryPanel") MainQueryPanel mainQueryPanel,
                                 @Named("metadataQueryPanel") MetadataQueryPanel metadataQueryPanel,
                                 @Named("configTab") ConfigTab configTab,
                                 @Named("mainQueryTab") JTabbedPane mainQuery,
                                 @Named("checkboxConfig") JCheckBox checkboxConfig,
                                 @Named("configSelectionContext") ConfigSelectionContext selectionContext) {
    this.profileManager = profileManager;
    this.eventBus = eventBus;
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.queryPanel = queryPanel;
    this.queryButtonPanel = queryButtonPanel;
    this.mainQueryPanel = mainQueryPanel;
    this.metadataQueryPanel = metadataQueryPanel;
    this.configTab = configTab;
    this.mainQuery = mainQuery;
    this.checkboxConfig = checkboxConfig;
    this.selectionContext = selectionContext;

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.queryButtonPanel.getBtnNew().addActionListener(this);
    this.queryButtonPanel.getBtnCopy().addActionListener(this);
    this.queryButtonPanel.getBtnDel().addActionListener(this);
    this.queryButtonPanel.getBtnEdit().addActionListener(this);
    this.queryButtonPanel.getBtnSave().addActionListener(this);
    this.queryButtonPanel.getBtnCancel().addActionListener(this);

    this.queryButtonPanel.getBtnDel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    this.queryButtonPanel.getBtnDel().getActionMap().put("delete", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        queryButtonPanel.getBtnDel().doClick();
      }
    });

    this.queryButtonPanel.getBtnCancel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    this.queryButtonPanel.getBtnCancel().getActionMap().put("cancel", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        queryButtonPanel.getBtnCancel().doClick();
      }
    });

    this.status = LifeCycleStatus.NONE;
    this.copySourceQueryId = null;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == queryButtonPanel.getBtnNew()) {
      status = LifeCycleStatus.NEW;
      copySourceQueryId = null;
      setPanelView(false);
      newEmptyPanel();
      clearProfileMetadataCase();
      return;
    }

    if (e.getSource() == queryButtonPanel.getBtnCopy()) {
      status = LifeCycleStatus.COPY;

      Integer queryId = selectionContext.getSelectedQueryId();
      if (queryId == null) {
        throw new NotSelectedRowException("The query to copy is not selected. Please select and try again!");
      }

      copySourceQueryId = queryId;

      setPanelView(false);

      QueryInfo query = profileManager.getQueryInfoById(queryId);
      if (query == null) {
        throw new NotFoundException("Not found query: " + queryId);
      }

      mainQueryPanel.getQueryName().setText(query.getName() + "_copy");
      mainQueryPanel.getQueryDescription().setText(query.getDescription() + "_copy");
      mainQueryPanel.getQueryGatherDataComboBox().setSelectedItem(query.getGatherDataMode());
      mainQueryPanel.getQuerySqlText().setText(query.getText());
      return;
    }

    if (e.getSource() == queryButtonPanel.getBtnDel()) {
      Integer queryId = selectionContext.getSelectedQueryId();
      if (queryId == null) {
        JOptionPane.showMessageDialog(null, "Not selected query. Please select and try again!",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      String queryName = getSelectedQueryNameById(queryId);

      int input = JOptionPane.showConfirmDialog(new JDialog(),
                                                "Do you want to delete configuration: " + queryName + "?");
      if (!isNotUsedOnTask(queryId)) {
        JOptionPane.showMessageDialog(null, "Cannot delete this query it is used in the task",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (input == 0) {
        QueryInfo query = profileManager.getQueryInfoById(queryId);
        if (query == null) {
          throw new NotFoundException("Not found query by id: " + queryId);
        }

        profileManager.deleteQuery(query.getId(), query.getName());
        profileManager.deleteTable(query.getName());
        profileManager.deleteChart(query.getId());

        clearQueryCase();
        refillQueryTable();

        if (queryCase.getJxTable().getRowCount() > 0) {
          queryCase.getJxTable().setRowSelectionInterval(0, 0);
        }

        eventBus.publish(new QueryListChangedEvent());
      }
      return;
    }

    if (e.getSource() == queryButtonPanel.getBtnEdit()) {
      Integer queryId = selectionContext.getSelectedQueryId();
      if (queryId == null) {
        throw new NotSelectedRowException("Not selected query. Please select and try again!");
      }
      status = LifeCycleStatus.EDIT;
      copySourceQueryId = null;
      setPanelView(false);
      return;
    }

    if (e.getSource() == queryButtonPanel.getBtnSave()) {
      if (LifeCycleStatus.NEW.equals(status) || LifeCycleStatus.COPY.equals(status)) {
        saveNew();
      } else if (LifeCycleStatus.EDIT.equals(status)) {
        saveEdit();
      }
      return;
    }

    if (e.getSource() == queryButtonPanel.getBtnCancel()) {
      cancelEdit();
    }
  }

  private void saveNew() {
    AtomicInteger queryIdNext = new AtomicInteger();
    profileManager.getQueryInfoList().stream()
        .max(Comparator.comparing(QueryInfo::getId))
        .ifPresentOrElse(query -> queryIdNext.set(query.getId()),
                         () -> queryIdNext.set(0));

    if (queryPanel.getMainQueryPanel().getQueryName().getText().trim().isEmpty()) {
      throw new EmptyNameException("The name field is empty");
    }

    int queryId = queryIdNext.incrementAndGet();
    String newQueryName = queryPanel.getMainQueryPanel().getQueryName().getText();
    checkQueryNameIsBusy(queryId, newQueryName);

    QueryInfo queryInfo = new QueryInfo();
    queryInfo.setId(queryId);
    queryInfo.setName(mainQueryPanel.getQueryName().getText());
    queryInfo.setDescription(mainQueryPanel.getQueryDescription().getText());

    String selectedEnumGatherData = Objects.requireNonNull(mainQueryPanel.getQueryGatherDataComboBox().getSelectedItem()).toString();
    queryInfo.setGatherDataMode(GatherDataMode.valueOf(selectedEnumGatherData));
    queryInfo.setText(mainQueryPanel.getQuerySqlText().getText());

    TableInfo tableInfo = new TableInfo();
    tableInfo.setTableName(queryInfo.getName());

    profileManager.addQuery(queryInfo);
    profileManager.addTable(tableInfo);

    ChartInfo chartInfo;
    if (LifeCycleStatus.COPY.equals(status) && copySourceQueryId != null) {
      ChartInfo source = profileManager.getChartInfoById(copySourceQueryId);
      chartInfo = source != null ? source.copy().setId(queryId) : buildDefaultChartInfo(queryId);
    } else {
      chartInfo = buildDefaultChartInfo(queryId);
    }
    profileManager.addChart(chartInfo);

    Integer selectedTaskId = TaskLinkDialog.show(profileManager.getTaskInfoList(), profileManager.getConnectionInfoList());
    if (selectedTaskId != null) {
      TaskInfo taskInfo = profileManager.getTaskInfoById(selectedTaskId);
      if (taskInfo != null && !taskInfo.getQueryInfoList().contains(queryId)) {
        taskInfo.getQueryInfoList().add(queryId);
        profileManager.updateTask(taskInfo);
        eventBus.publish(new UpdateQueryList(selectedTaskId));
      }
    }

    clearQueryCase();

    List<QueryInfo> allQueries = profileManager.getQueryInfoList();
    List<QueryRow> rows = allQueries.stream()
        .map(q -> new QueryRow(q.getId(), q.getName()))
        .collect(Collectors.toList());

    TTTable<QueryRow, JXTable> tt = queryCase.getTypedTable();
    tt.setItems(rows);

    int selection = 0;
    for (int i = 0; i < rows.size(); i++) {
      if (rows.get(i).getId() == queryInfo.getId()) {
        selection = i;
        break;
      }
    }

    setPanelView(true);
    queryCase.getJxTable().setRowSelectionInterval(selection, selection);

    status = LifeCycleStatus.NONE;
    copySourceQueryId = null;

    eventBus.publish(new QueryListChangedEvent());
  }

  private void saveEdit() {
    Integer queryId = selectionContext.getSelectedQueryId();
    if (queryId == null) {
      throw new NotSelectedRowException("Not selected query. Please select and try again!");
    }

    if (queryPanel.getMainQueryPanel().getQueryName().getText().trim().isEmpty()) {
      throw new EmptyNameException("The name field is empty");
    }

    int selectedIndex = queryCase.getJxTable().getSelectedRow();
    String newQueryName = queryPanel.getMainQueryPanel().getQueryName().getText();
    checkQueryNameIsBusy(queryId, newQueryName);

    QueryInfo oldQuery = profileManager.getQueryInfoById(queryId);
    if (oldQuery == null) {
      throw new NotFoundException("Not found query: " + queryId);
    }

    QueryInfo editQuery = new QueryInfo();
    editQuery.setId(queryId);
    editQuery.setName(mainQueryPanel.getQueryName().getText());
    editQuery.setDescription(mainQueryPanel.getQueryDescription().getText());
    String selectedEnumGatherData = Objects.requireNonNull(mainQueryPanel.getQueryGatherDataComboBox().getSelectedItem()).toString();
    editQuery.setGatherDataMode(GatherDataMode.valueOf(selectedEnumGatherData));
    editQuery.setText(mainQueryPanel.getQuerySqlText().getText());

    clearProfileMetadataCase();

    TableInfo tableInfo = new TableInfo();
    tableInfo.setTableName(editQuery.getName());

    if (!oldQuery.getName().equals(newQueryName)) {
      deleteQueryById(queryId);
      profileManager.addQuery(editQuery);
      profileManager.addTable(tableInfo);
    } else {
      profileManager.updateQuery(editQuery);
    }

    clearQueryCase();
    refillQueryTable();

    setPanelView(true);
    mainQueryPanel.getQueryGatherDataComboBox().setSelectedItem(GatherDataMode.valueOf(selectedEnumGatherData));
    if (selectedIndex >= 0 && queryCase.getJxTable().getRowCount() > 0) {
      queryCase.getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);
    }

    status = LifeCycleStatus.NONE;
    copySourceQueryId = null;

    eventBus.publish(new QueryListChangedEvent());
  }

  private void cancelEdit() {
    if (queryCase.getJxTable().getSelectedRowCount() > 0) {
      int selectedIndex = queryCase.getJxTable().getSelectedRow();
      queryCase.getJxTable().setRowSelectionInterval(0, 0);
      setPanelView(true);
      queryCase.getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);

      Integer queryId = selectionContext.getSelectedQueryId();
      if (queryId == null) {
        status = LifeCycleStatus.NONE;
        copySourceQueryId = null;
        return;
      }

      QueryInfo queryInfo = profileManager.getQueryInfoById(queryId);
      if (queryInfo == null) {
        throw new NotFoundException("Not found query: " + queryId);
      }
      mainQueryPanel.getQueryName().setText(queryInfo.getName());
      mainQueryPanel.getQueryDescription().setText(queryInfo.getDescription());
      mainQueryPanel.getQueryGatherDataComboBox().setSelectedItem(queryInfo.getGatherDataMode());
      mainQueryPanel.getQuerySqlText().setText(queryInfo.getText());
    } else {
      setPanelView(true);
      newEmptyPanel();
      clearProfileMetadataCase();
    }

    status = LifeCycleStatus.NONE;
    copySourceQueryId = null;
  }

  private void newEmptyPanel() {
    mainQueryPanel.getQueryName().setText("");
    mainQueryPanel.getQueryName().setPrompt(bundleDefault.getString("qName"));
    mainQueryPanel.getQueryDescription().setText("");
    mainQueryPanel.getQueryDescription().setPrompt(bundleDefault.getString("qDesc"));
    mainQueryPanel.getQuerySqlText().setText("");
  }

  private void checkQueryNameIsBusy(int id, String newQueryName) {
    for (QueryInfo query : profileManager.getQueryInfoList()) {
      if (query.getName().equals(newQueryName) && query.getId() != id) {
        throw new NotFoundException("Name " + newQueryName + " already exists, please enter another one.");
      }
    }
  }

  private void deleteQueryById(int id) {
    QueryInfo queryDel = profileManager.getQueryInfoById(id);
    if (queryDel == null) {
      throw new NotFoundException("Not found query by id: " + id);
    }
    profileManager.deleteQuery(queryDel.getId(), queryDel.getName());
  }

  private void clearProfileMetadataCase() {
    metadataQueryPanel.getConfigMetadataCase().clearTable();
  }

  private void clearQueryCase() {
    queryCase.clearTable();
  }

  private void refillQueryTable() {
    TTTable<QueryRow, JXTable> tt = queryCase.getTypedTable();
    List<QueryRow> rows = profileManager.getQueryInfoList().stream()
        .map(q -> new QueryRow(q.getId(), q.getName()))
        .collect(Collectors.toList());
    tt.setItems(rows);
  }

  private String getSelectedQueryNameById(int queryId) {
    QueryInfo query = profileManager.getQueryInfoById(queryId);
    return query != null ? query.getName() : "";
  }

  private void setPanelView(Boolean isSelected) {
    queryButtonPanel.setButtonView(isSelected);

    mainQueryPanel.getQueryName().setEditable(!isSelected);
    mainQueryPanel.getQueryDescription().setEditable(!isSelected);
    mainQueryPanel.getQueryGatherDataComboBox().setEnabled(!isSelected);
    mainQueryPanel.getQuerySqlText().setEditable(!isSelected);

    configTab.setEnabledAt(1, isSelected);
    configTab.setEnabledAt(2, isSelected);
    configTab.setEnabledAt(0, isSelected);

    taskCase.getJxTable().setEnabled(isSelected);
    connectionCase.getJxTable().setEnabled(isSelected);
    profileCase.getJxTable().setEnabled(isSelected);
    queryCase.getJxTable().setEnabled(isSelected);

    mainQuery.setEnabledAt(1, isSelected);
    mainQuery.setEnabledAt(2, isSelected);

    checkboxConfig.setEnabled(isSelected);
  }

  private boolean isNotUsedOnTask(int queryId) {
    return profileManager.getTaskInfoList().stream()
        .noneMatch(task -> task.getQueryInfoList().contains(queryId));
  }

  private ChartInfo buildDefaultChartInfo(int chartId) {
    return new ChartInfo()
        .setId(chartId)
        .setRangeRealtime(RangeRealTime.TEN_MIN)
        .setRangeHistory(RangeHistory.DAY)
        .setPullTimeoutClient(3)
        .setPullTimeoutServer(1);
  }
}