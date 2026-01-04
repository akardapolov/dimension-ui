package ru.dimension.ui.view.handler.task;

import static ru.dimension.ui.model.view.handler.LifeCycleStatus.COPY;
import static ru.dimension.ui.model.view.handler.LifeCycleStatus.EDIT;
import static ru.dimension.ui.model.view.handler.LifeCycleStatus.NEW;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
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
import javax.swing.KeyStroke;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.exception.EmptyNameException;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.handler.LifeCycleStatus;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.task.MultiSelectQueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows.QueryTableRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Singleton
public class TaskButtonPanelHandler implements ActionListener {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;
  private final EventListener eventListener;

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final TaskPanel taskPanel;
  private final MultiSelectQueryPanel multiSelectQueryPanel;
  private final ButtonPanel taskButtonPanel;
  private final ConfigTab configTab;
  private final JCheckBox checkboxConfig;

  private LifeCycleStatus status;
  private final ResourceBundle bundleDefault;

  @Inject
  public TaskButtonPanelHandler(@Named("profileManager") ProfileManager profileManager,
                                @Named("templateManager") TemplateManager templateManager,
                                @Named("eventListener") EventListener eventListener,
                                @Named("profileConfigCase") JXTableCase profileCase,
                                @Named("taskConfigCase") JXTableCase taskCase,
                                @Named("connectionConfigCase") JXTableCase connectionCase,
                                @Named("queryConfigCase") JXTableCase queryCase,
                                @Named("taskConfigPanel") TaskPanel taskPanel,
                                @Named("multiSelectQueryPanel") MultiSelectQueryPanel multiSelectQueryPanel,
                                @Named("taskButtonPanel") ButtonPanel taskButtonPanel,
                                @Named("jTabbedPaneConfig") ConfigTab configTab,
                                @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.profileManager = profileManager;
    this.templateManager = templateManager;

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;

    this.taskPanel = taskPanel;
    this.multiSelectQueryPanel = multiSelectQueryPanel;
    this.taskButtonPanel = taskButtonPanel;
    this.configTab = configTab;
    this.checkboxConfig = checkboxConfig;
    this.taskButtonPanel.getBtnNew().addActionListener(this);
    this.taskButtonPanel.getBtnCopy().addActionListener(this);
    this.taskButtonPanel.getBtnDel().addActionListener(this);
    this.taskButtonPanel.getBtnEdit().addActionListener(this);
    this.taskButtonPanel.getBtnSave().addActionListener(this);
    this.taskButtonPanel.getBtnCancel().addActionListener(this);

    this.taskButtonPanel.getBtnDel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    this.taskButtonPanel.getBtnDel().getActionMap().put("delete", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        taskButtonPanel.getBtnDel().doClick();
      }
    });

    this.taskButtonPanel.getBtnCancel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    this.taskButtonPanel.getBtnCancel().getActionMap().put("cancel", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        taskButtonPanel.getBtnCancel().doClick();
      }
    });

    this.taskPanel.getTaskConnectionComboBox().addItemListener(e -> {
      if (NEW.equals(status) || COPY.equals(status) || EDIT.equals(status)) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          fillAvailableQueryList();
        }
      }
    });

    this.status = LifeCycleStatus.NONE;

    this.eventListener = eventListener;
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == taskButtonPanel.getBtnNew()) {
      status = NEW;
      List<ConnectionInfo> connectionAll = profileManager.getConnectionInfoList();
      if (!connectionAll.isEmpty()) {
        setPanelView(false);
        List<List<?>> connectionDataList = new LinkedList<>();

        connectionAll.forEach(connection -> {
          if (connection.getType() != null) {
            connectionDataList.add(
                new ArrayList<>(Arrays.asList(connection.getName(), connection.getUserName(),
                                              connection.getUrl(), connection.getJar(),
                                              connection.getDriver(), connection.getType())));
          } else {
            connectionDataList.add(
                new ArrayList<>(Arrays.asList(connection.getName(), connection.getUserName(),
                                              connection.getUrl(), connection.getJar(),
                                              connection.getDriver(), "JDBC")));
          }
        });

        newEmptyPanel();

        taskPanel.getTaskConnectionComboBox().setTableData(connectionDataList);

        clearSelectedQueryTable();

        fillAvailableQueryList();
      } else {
        JOptionPane.showMessageDialog(null, "No connections were created",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      }

    } else if (e.getSource() == taskButtonPanel.getBtnCopy()) {
      status = COPY;
      if (taskCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("The task to copy is not selected. Please select and try again!");
      } else {
        int taskId = getSelectedTaskId();
        TaskInfo task = profileManager.getTaskInfoById(taskId);
        if (Objects.isNull(task)) {
          throw new NotFoundException("Not found task: " + taskId);
        }
        setPanelView(false);

        taskPanel.getJTextFieldTask().setText(task.getName() + "_copy");
        taskPanel.getJTextFieldDescription().setText(task.getDescription() + "_copy");
        fillAvailableQueryList();
      }

    } else if (e.getSource() == taskButtonPanel.getBtnDel()) {
      if (taskCase.getJxTable().getSelectedRow() == -1) {
        JOptionPane.showMessageDialog(null, "Not selected task. Please select and try again!",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      } else {
        int taskId = getSelectedTaskId();

        TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();
        TaskRow selectedRow = tt.model().itemAt(taskCase.getJxTable().getSelectedRow());
        String taskName = selectedRow != null ? selectedRow.getName() : "Unknown";

        int input = JOptionPane.showConfirmDialog(new JDialog(),
                                                  "Do you want to delete configuration: " + taskName + "?");
        if (isUsedOnTask(taskId)) {
          if (input == 0) {
            TaskInfo task = profileManager.getTaskInfoById(taskId);
            if (Objects.isNull(task)) {
              throw new NotFoundException("Not found task: " + taskId);
            }

            profileManager.deleteTask(task.getId(), task.getName());
            clearTaskCase();
            refillTaskTable();

            if (taskCase.getJxTable().getRowCount() > 0) {
              taskCase.getJxTable().setRowSelectionInterval(0, 0);
            }
          }
        } else {
          JOptionPane.showMessageDialog(null, "Cannot delete this task it is used in the profile",
                                        "General Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    } else if (e.getSource() == taskButtonPanel.getBtnEdit()) {
      if (taskCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("Not selected task. Please select and try again!");
      }
      status = EDIT;
      setPanelView(false);
      fillAvailableQueryList();

    } else if (e.getSource() == taskButtonPanel.getBtnSave()) {

      if (NEW.equals(status) || COPY.equals(status)) {

        AtomicInteger taskIdNext = new AtomicInteger();

        profileManager.getTaskInfoList().stream()
            .max(Comparator.comparing(TaskInfo::getId))
            .ifPresentOrElse(task -> taskIdNext.set(task.getId()),
                             () -> {
                               log.info("Not found Task");
                               taskIdNext.set(0);
                             });
        if (!taskPanel.getJTextFieldTask().getText().trim().isEmpty()) {
          int taskId = taskIdNext.incrementAndGet();
          String newTaskName = taskPanel.getJTextFieldTask().getText();
          checkTaskNameIsBusy(taskId, newTaskName);

          TaskInfo saveTask = getTaskInfo(taskIdNext.incrementAndGet());

          profileManager.addTask(saveTask);

          clearTaskCase();

          List<TaskInfo> allTasks = profileManager.getTaskInfoList();
          List<TaskRow> rows = allTasks.stream()
              .map(t -> new TaskRow(t.getId(), t.getName()))
              .collect(Collectors.toList());

          TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();
          tt.setItems(rows);

          int selection = 0;
          for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getId() == saveTask.getId()) {
              selection = i;
              break;
            }
          }

          setPanelView(true);
          taskCase.getJxTable().setRowSelectionInterval(selection, selection);
          multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);
        } else {
          throw new EmptyNameException("The name field is empty");
        }
      } else if (EDIT.equals(status)) {
        int selectedIndex = taskCase.getJxTable().getSelectedRow();
        int taskId = getSelectedTaskId();

        if (!taskPanel.getJTextFieldTask().getText().trim().isEmpty()) {
          String newTaskName = taskPanel.getJTextFieldTask().getText();
          checkTaskNameIsBusy(taskId, newTaskName);

          TaskInfo oldTask = profileManager.getTaskInfoById(taskId);

          TaskInfo editTask = getTaskInfo(taskId);

          if (!oldTask.getName().equals(newTaskName)) {
            deleteTaskById(taskId);
            profileManager.addTask(editTask);
          } else {
            profileManager.updateTask(editTask);
          }

          clearTaskCase();
          refillTaskTable();

          setPanelView(true);
          taskCase.getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);
          multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);
        } else {
          throw new EmptyNameException("The name field is empty");
        }
      }
    } else if (e.getSource() == taskButtonPanel.getBtnCancel()) {

      if (taskCase.getJxTable().getSelectedRowCount() > 0) {
        int selectedRowTask = taskCase.getJxTable().getSelectedRow();
        taskCase.getJxTable().setRowSelectionInterval(0, 0);
        setPanelView(true);
        taskCase.getJxTable().setRowSelectionInterval(selectedRowTask, selectedRowTask);

        int selectedId = getSelectedTaskId();
        TaskInfo task = profileManager.getTaskInfoById(selectedId);
        if (Objects.isNull(task)) {
          throw new NotFoundException("Not found task: " + selectedId);
        }
        taskPanel.getJTextFieldTask().setText(task.getName());
        taskPanel.getJTextFieldDescription().setText(task.getDescription());

        String selectedConnection = (String) taskPanel.getTaskConnectionComboBox().getSelectedItem();

        int connectionId = profileManager.getConnectionInfoList()
            .stream()
            .filter(f -> f.getName().equalsIgnoreCase(selectedConnection))
            .findAny()
            .orElseThrow(() -> new NotFoundException("Not found connection: " + selectedConnection))
            .getId();

        List<ConnectionInfo> connectionAll = profileManager.getConnectionInfoList();
        List<List<?>> connectionData = new ArrayList<>();

        connectionAll.stream()
            .filter(connection -> connection.getId() == connectionId)
            .forEach(connection -> {
              String type = connection.getType() != null ? String.valueOf(connection.getType()) : "JDBC";
              connectionData.add(0, Arrays.asList(
                  connection.getName(), connection.getUserName(),
                  connection.getUrl(), connection.getJar(),
                  connection.getDriver(), type
              ));
            });

        connectionAll.stream()
            .filter(connection -> connection.getId() != connectionId)
            .forEach(connection -> {
              String type = connection.getType() != null ? String.valueOf(connection.getType()) : "JDBC";
              connectionData.add(Arrays.asList(
                  connection.getName(), connection.getUserName(),
                  connection.getUrl(), connection.getJar(),
                  connection.getDriver(), type
              ));
            });

        taskPanel.getTaskConnectionComboBox().setTableData(connectionData);

        fillAvailableQueryList();
        multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);

      } else {
        setPanelView(true);
        newEmptyPanel();
        fillAvailableQueryList();
      }
    }
  }

  private void newEmptyPanel() {
    taskPanel.getJTextFieldTask().setText("");
    taskPanel.getJTextFieldTask().setPrompt(bundleDefault.getString("tName"));
    taskPanel.getJTextFieldDescription().setText("");
    taskPanel.getJTextFieldDescription().setPrompt(bundleDefault.getString("tDesc"));
  }

  public void checkTaskNameIsBusy(int id, String newTaskName) {
    List<TaskInfo> taskList = profileManager.getTaskInfoList();
    for (TaskInfo task : taskList) {
      if (task.getName().equals(newTaskName) && task.getId() != id) {
        throw new NotFoundException("Name " + newTaskName
                                        + " already exists, please enter another one.");
      }
    }
  }

  public void deleteTaskById(int id) {
    TaskInfo taskDel = profileManager.getTaskInfoById(id);
    if (Objects.isNull(taskDel)) {
      throw new NotFoundException("Not found task by id: " + id);
    }
    profileManager.deleteTask(taskDel.getId(), taskDel.getName());
  }

  private void fillAvailableQueryList() {
    String connName = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(0).toString();
    Object connDriver = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4);

    List<String> listSelectedQueryForExclude = getSelectedQueryNameList();

    List<QueryInfo> queryListOfUnsuitableConnType = profileManager.getQueryInfoList().stream()
        .filter(q -> profileManager.getConnectionInfoList().stream()
            .anyMatch(c -> c.getType() != null &&
                c.getType().equals(ConnectionType.HTTP) &&
                c.getName().equals(q.getName())))
        .toList();

    if (connDriver != null) {
      log.info("Connection's {} driver: {}", connName, connDriver);

      // Configuration
      List<String> connectionDrivers = getConnectionDriverAll();
      connectionDrivers.removeIf(q -> q == null || connDriver.toString().contains(q));

      List<QueryInfo> queryListOfUnsuitableConnDriver = connectionDrivers.stream()
          .flatMap(driver -> profileManager.getQueryInfoListByConnDriver(driver).stream())
          .toList();

      List<QueryTableRow> queryListRows = profileManager.getQueryInfoList().stream()
          .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
          .filter(q -> !queryListOfUnsuitableConnDriver.contains(q))
          .filter(q -> !queryListOfUnsuitableConnType.contains(q))
          .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
          .collect(Collectors.toList());

      TTTable<QueryTableRow, JXTable> queryListTT = multiSelectQueryPanel.getQueryListCase().getTypedTable();
      queryListTT.setItems(queryListRows);

      // Template
      List<QueryTableRow> templateRows = templateManager.getQueryListByConnDriver(connDriver.toString()).stream()
          .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
          .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
          .collect(Collectors.toList());

      TTTable<QueryTableRow, JXTable> templateTT = multiSelectQueryPanel.getTemplateListQueryCase().getTypedTable();
      templateTT.setItems(templateRows);

      // Selected
      List<QueryTableRow> selectedRows = profileManager.getQueryInfoListByConnDriver(connDriver.toString()).stream()
          .filter(q -> listSelectedQueryForExclude.contains(q.getName()))
          .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
          .collect(Collectors.toList());

      TTTable<QueryTableRow, JXTable> selectedTT = multiSelectQueryPanel.getSelectedQueryCase().getTypedTable();
      selectedTT.setItems(selectedRows);

    } else {
      // Configuration
      List<String> connectionDrivers = getConnectionDriverAll();

      List<QueryInfo> queryListOfUnsuitableConnDriver = connectionDrivers.stream()
          .flatMap(driver -> profileManager.getQueryInfoListByConnDriver(driver).stream())
          .toList();

      List<QueryTableRow> queryListRows = profileManager.getQueryInfoList().stream()
          .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
          .filter(q -> !queryListOfUnsuitableConnDriver.contains(q))
          .filter(q -> q.getName().equals(connName))
          .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
          .collect(Collectors.toList());

      TTTable<QueryTableRow, JXTable> queryListTT = multiSelectQueryPanel.getQueryListCase().getTypedTable();
      queryListTT.setItems(queryListRows);

      // Selected
      List<QueryTableRow> selectedRows = queryListOfUnsuitableConnType.stream()
          .filter(q -> listSelectedQueryForExclude.contains(q.getName()))
          .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
          .collect(Collectors.toList());

      TTTable<QueryTableRow, JXTable> selectedTT = multiSelectQueryPanel.getSelectedQueryCase().getTypedTable();
      selectedTT.setItems(selectedRows);
    }
  }

  public int setRadioButtonPullTimeout() {
    int pullTimeout = 0;
    if (taskPanel.getRadioButtonPanel().getJRadioButton1().isSelected()) {
      pullTimeout = 1;
    }
    if (taskPanel.getRadioButtonPanel().getJRadioButton3().isSelected()) {
      pullTimeout = 3;
    }
    if (taskPanel.getRadioButtonPanel().getJRadioButton5().isSelected()) {
      pullTimeout = 5;
    }
    if (taskPanel.getRadioButtonPanel().getJRadioButton10().isSelected()) {
      pullTimeout = 10;
    }
    if (taskPanel.getRadioButtonPanel().getJRadioButton30().isSelected()) {
      pullTimeout = 30;
    }
    return pullTimeout;
  }

  private int getSelectedTaskId() {
    int selectedRow = taskCase.getJxTable().getSelectedRow();
    if (selectedRow < 0) {
      return -1;
    }
    TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();
    TaskRow row = tt.model().itemAt(selectedRow);
    return row != null ? row.getId() : -1;
  }

  private void clearTaskCase() {
    taskCase.clearTable();
  }

  private void clearSelectedQueryTable() {
    TTTable<QueryTableRow, JXTable> selectedTT = multiSelectQueryPanel.getSelectedQueryCase().getTypedTable();
    selectedTT.setItems(new ArrayList<>());
  }

  private void refillTaskTable() {
    TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();
    List<TaskRow> rows = profileManager.getTaskInfoList().stream()
        .map(t -> new TaskRow(t.getId(), t.getName()))
        .collect(Collectors.toList());
    tt.setItems(rows);
  }

  private TaskInfo getTaskInfo(int taskId) {
    TaskInfo task = new TaskInfo();
    task.setId(taskId);
    task.setName(taskPanel.getJTextFieldTask().getText());
    task.setDescription(taskPanel.getJTextFieldDescription().getText());
    task.setPullTimeout(setRadioButtonPullTimeout());
    String selectedConnection = (String) taskPanel.getTaskConnectionComboBox().getSelectedRow().getFirst();

    int connectionId = profileManager.getConnectionInfoList()
        .stream()
        .filter(f -> f.getName().equalsIgnoreCase(selectedConnection))
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found connection: " + selectedConnection))
        .getId();

    task.setConnectionId(connectionId);

    TTTable<QueryTableRow, JXTable> selectedTT = multiSelectQueryPanel.getSelectedQueryCase().getTypedTable();
    List<QueryTableRow> selectedRows = selectedTT.model().items();

    if (!selectedRows.isEmpty()) {
      List<Integer> queryListId = new ArrayList<>();
      for (QueryTableRow row : selectedRows) {
        Integer selectedDataQueryId = row.getId();
        String selectedQueryName = row.getName();

        AtomicInteger queryIdNext = new AtomicInteger();

        profileManager.getQueryInfoList().stream()
            .max(Comparator.comparing(QueryInfo::getId))
            .ifPresentOrElse(query -> queryIdNext.set(query.getId()),
                             () -> log.info("Not found Query"));
        int newId = queryIdNext.incrementAndGet();

        if (!isExistQueryName(selectedQueryName)) {
          List<Query> queryList = templateManager.getConfigList(Query.class);
          Query saveQuery = queryList.stream()
              .filter(s -> s.getId() == selectedDataQueryId)
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found query by id: " + selectedDataQueryId));

          QueryInfo queryInfo = new QueryInfo();
          queryInfo.setId(newId);
          queryInfo.setName(saveQuery.getName());
          queryInfo.setText(saveQuery.getText());
          queryInfo.setDescription(saveQuery.getDescription());
          queryInfo.setGatherDataMode(saveQuery.getGatherDataMode());
          queryInfo.setMetricList(saveQuery.getMetricList());
          profileManager.addQuery(queryInfo);
          TableInfo tableInfo = new TableInfo();
          tableInfo.setTableName(saveQuery.getName());
          profileManager.addTable(tableInfo);

        } else {
          QueryInfo query = profileManager.getQueryInfoList().stream()
              .filter(s -> s.getName().equals(selectedQueryName))
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found query by id: " + selectedDataQueryId));
          newId = query.getId();
        }

        queryListId.add(newId);
      }
      task.setQueryInfoList(queryListId);
    } else {
      task.setQueryInfoList(Collections.emptyList());
    }

    return task;
  }

  private void setPanelView(Boolean isSelected) {
    taskButtonPanel.setButtonView(isSelected);
    taskPanel.getJTextFieldTask().setEditable(!isSelected);
    taskPanel.getJTextFieldDescription().setEditable(!isSelected);
    taskPanel.getTaskConnectionComboBox().setEnabled(!isSelected);
    multiSelectQueryPanel.getUnPickBtn().setEnabled(!isSelected);
    multiSelectQueryPanel.getPickBtn().setEnabled(!isSelected);
    multiSelectQueryPanel.getPickAllBtn().setEnabled(!isSelected);
    multiSelectQueryPanel.getUnPickAllBtn().setEnabled(!isSelected);
    if (isSelected) {
      taskPanel.getRadioButtonPanel().setButtonNotView();
    } else {
      taskPanel.getRadioButtonPanel().setButtonView();
    }
    configTab.setEnabledAt(0, isSelected);
    configTab.setEnabledAt(2, isSelected);
    configTab.setEnabledAt(3, isSelected);

    profileCase.getJxTable().setEnabled(isSelected);
    taskCase.getJxTable().setEnabled(isSelected);
    connectionCase.getJxTable().setEnabled(isSelected);
    queryCase.getJxTable().setEnabled(isSelected);
    checkboxConfig.setEnabled(isSelected);
  }

  private boolean isExistQueryName(String selectedName) {
    return profileManager.getQueryInfoList().stream()
        .anyMatch(query -> query.getName().equals(selectedName));
  }

  private List<String> getSelectedQueryNameList() {
    List<String> queryListNames = new ArrayList<>();
    TTTable<QueryTableRow, JXTable> selectedTT = multiSelectQueryPanel.getSelectedQueryCase().getTypedTable();
    List<QueryTableRow> items = selectedTT.model().items();
    for (QueryTableRow row : items) {
      queryListNames.add(row.getName());
    }
    return queryListNames;
  }

  private List<String> getConnectionDriverAll() {
    List<ConnectionInfo> connectionInfoList = profileManager.getConnectionInfoList();
    return connectionInfoList.stream()
        .map(ConnectionInfo::getDriver)
        .distinct()
        .collect(Collectors.toList());
  }

  private boolean isUsedOnTask(int taskId) {
    return !profileManager.getProfileInfoList().stream()
        .anyMatch(profile -> profile.getTaskInfoList().contains(taskId));
  }
}