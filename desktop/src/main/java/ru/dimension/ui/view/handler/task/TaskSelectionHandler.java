package ru.dimension.ui.view.handler.task;

import static ru.dimension.ui.model.view.tab.ConnectionTypeTabPane.JDBC;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.tab.ConfigEditTabPane;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.handler.MouseListenerImpl;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.task.MultiSelectQueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;
import ru.dimension.ui.view.table.row.Rows.QueryTableRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Singleton
public class TaskSelectionHandler extends MouseListenerImpl
    implements ListSelectionListener, ItemListener, ChangeListener {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;

  private final JXTableCase taskCase;
  private final JXTableCase profileCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;

  private final ConfigTab configTab;
  private final TaskPanel taskPanel;
  private final MultiSelectQueryPanel multiSelectQueryPanel;
  private final ButtonPanel taskButtonPanel;
  private final JCheckBox checkboxConfig;
  private Boolean isSelected;
  private List<List<?>> connectionDataList;
  private int taskId = -1;
  private final ResourceBundle bundleDefault;

  @Inject
  public TaskSelectionHandler(@Named("profileManager") ProfileManager profileManager,
                              @Named("templateManager") TemplateManager templateManager,
                              @Named("taskConfigCase") JXTableCase taskCase,
                              @Named("profileConfigCase") JXTableCase profileCase,
                              @Named("connectionConfigCase") JXTableCase connectionCase,
                              @Named("queryConfigCase") JXTableCase queryCase,
                              @Named("jTabbedPaneConfig") ConfigTab configTab,
                              @Named("taskConfigPanel") TaskPanel taskPanel,
                              @Named("multiSelectQueryPanel") MultiSelectQueryPanel multiSelectQueryPanel,
                              @Named("taskButtonPanel") ButtonPanel taskButtonPanel,
                              @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.profileManager = profileManager;
    this.templateManager = templateManager;

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.taskCase = taskCase;
    this.profileCase = profileCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.taskCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    this.taskCase.getJxTable().addMouseListener(this);

    this.configTab = configTab;
    this.taskPanel = taskPanel;
    this.multiSelectQueryPanel = multiSelectQueryPanel;
    this.taskButtonPanel = taskButtonPanel;

    this.checkboxConfig = checkboxConfig;
    this.checkboxConfig.addItemListener(this);
    this.isSelected = false;

    this.configTab.addChangeListener(this);
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

    if (configTab.isEnabledAt(1)) {
      configTab.setSelectedTab(ConfigEditTabPane.TASK);
    }

    if (!e.getValueIsAdjusting()) {
      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing profile fields");

        clearMultiSelectionPanel();
        taskId = -1;

        multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);

        taskPanel.getJTextFieldTask().setEditable(false);
        taskPanel.getJTextFieldDescription().setEditable(false);
        taskPanel.getTaskConnectionComboBox().setEnabled(false);
        taskPanel.getRadioButtonPanel().setButtonNotView();
        multiSelectQueryPanel.getUnPickBtn().setEnabled(false);
        multiSelectQueryPanel.getPickBtn().setEnabled(false);
        multiSelectQueryPanel.getPickAllBtn().setEnabled(false);
        multiSelectQueryPanel.getUnPickAllBtn().setEnabled(false);

        List<ConnectionInfo> connectionAll = profileManager.getConnectionInfoList();
        this.connectionDataList = new LinkedList<>();
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

        taskPanel.getJTextFieldTask().setText("");
        taskPanel.getJTextFieldTask().setPrompt(bundleDefault.getString("tName"));
        taskPanel.getJTextFieldDescription().setText("");
        taskPanel.getJTextFieldDescription().setPrompt(bundleDefault.getString("tDesc"));
        taskPanel.getTaskConnectionComboBox().setTableData(connectionDataList);
        taskPanel.getTaskConnectionComboBox().setEnabled(false);

      } else {
        clearMultiSelectionPanel();

        taskId = getSelectedTaskId();

        if (taskId == -1) {
          return;
        }

        TaskInfo taskInfo = profileManager.getTaskInfoById(taskId);
        if (Objects.isNull(taskInfo)) {
          throw new NotFoundException("Not found task: " + taskId);
        }

        taskPanel.getJTextFieldTask().setText(taskInfo.getName());
        taskPanel.getJTextFieldDescription().setText(taskInfo.getDescription());
        taskPanel.getRadioButtonPanel().setSelectedRadioButton(taskInfo.getPullTimeout() + " sec");

        ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(taskInfo.getConnectionId());
        if (Objects.isNull(connectionInfo)) {
          throw new NotFoundException("Not found connection: " + taskInfo.getConnectionId());
        }
        List<ConnectionInfo> connectionAll = profileManager.getConnectionInfoList();
        if (connectionInfo.getType() == null) {
          connectionInfo.setType(ConnectionType.JDBC);
        }
        List<List<?>> connectionData = new LinkedList<>();
        connectionData.add(
            new ArrayList<>(Arrays.asList(connectionInfo.getName(), connectionInfo.getUserName(),
                                          connectionInfo.getUrl(), connectionInfo.getJar(),
                                          connectionInfo.getDriver(), connectionInfo.getType())));
        connectionAll.stream()
            .filter(f -> f.getId() != connectionInfo.getId())
            .forEach(connection -> {
              String type = connection.getType() != null ? String.valueOf(connection.getType()) : "JDBC";
              connectionData.add(Arrays.asList(
                  connection.getName(), connection.getUserName(),
                  connection.getUrl(), connection.getJar(),
                  connection.getDriver(), type
              ));
            });

        taskPanel.getTaskConnectionComboBox().setTableData(connectionData);

        if (Objects.isNull(profileManager.getTaskInfoById(taskId))) {
          throw new NotFoundException("Not found task: " + taskId);
        } else {
          List<QueryTableRow> selectedRows = profileManager.getTaskInfoById(taskId)
              .getQueryInfoList()
              .stream()
              .map(queryId -> {
                QueryInfo queryInfo = profileManager.getQueryInfoById(queryId);
                if (Objects.isNull(queryInfo)) {
                  throw new NotFoundException("Not found query: " + queryId);
                }
                return new QueryTableRow(
                    queryInfo.getId(),
                    queryInfo.getName(),
                    queryInfo.getDescription(),
                    queryInfo.getText()
                );
              })
              .collect(Collectors.toList());

          TTTable<QueryTableRow, JXTable> selectedTT = multiSelectQueryPanel.getSelectedQueryCase().getTypedTable();
          selectedTT.setItems(selectedRows);
        }

        String connName = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(0).toString();
        Object connDriver = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4);

        if (connDriver != null) {
          log.info("Connection's {} driver: {}", connName, connDriver);
          fillAvailableQueryList(connDriver.toString());
        }
        fillConnectionCheckboxIsSelected(isSelected);
        fillQueryCheckboxIsSelected(isSelected);

        GUIHelper.disableButton(taskButtonPanel, !isSelected);
      }
    }
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

  private void clearMultiSelectionPanel() {
    TTTable<QueryTableRow, JXTable> selectedTT = multiSelectQueryPanel.getSelectedQueryCase().getTypedTable();
    TTTable<QueryTableRow, JXTable> queryListTT = multiSelectQueryPanel.getQueryListCase().getTypedTable();
    TTTable<QueryTableRow, JXTable> templateTT = multiSelectQueryPanel.getTemplateListQueryCase().getTypedTable();

    selectedTT.setItems(new ArrayList<>());
    queryListTT.setItems(new ArrayList<>());
    templateTT.setItems(new ArrayList<>());
  }

  private void fillAvailableQueryList(String connDriver) {
    List<String> listSelectedQueryForExclude = getSelectedQueryNameList();

    // Configuration
    List<String> connectionDrivers = getConnectionDriverAll();
    connectionDrivers.removeIf(q -> q == null || connDriver.contains(q));

    List<QueryInfo> queryListOfUnsuitableConnDriver = connectionDrivers.stream()
        .flatMap(driver -> profileManager.getQueryInfoListByConnDriver(driver).stream())
        .collect(Collectors.toList());

    List<QueryInfo> queryListOfUnsuitableConnType = profileManager.getQueryInfoList().stream()
        .filter(q -> profileManager.getConnectionInfoList().stream()
            .anyMatch(c -> c.getType() != null &&
                c.getType().equals(ConnectionType.HTTP) &&
                c.getName().equals(q.getName())))
        .collect(Collectors.toList());

    List<QueryTableRow> queryListRows = profileManager.getQueryInfoList().stream()
        .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
        .filter(q -> !queryListOfUnsuitableConnDriver.contains(q))
        .filter(q -> !queryListOfUnsuitableConnType.contains(q))
        .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
        .collect(Collectors.toList());

    TTTable<QueryTableRow, JXTable> queryListTT = multiSelectQueryPanel.getQueryListCase().getTypedTable();
    queryListTT.setItems(queryListRows);

    // Template
    List<QueryTableRow> templateRows = templateManager.getQueryListByConnDriver(connDriver).stream()
        .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
        .collect(Collectors.toList());

    TTTable<QueryTableRow, JXTable> templateTT = multiSelectQueryPanel.getTemplateListQueryCase().getTypedTable();
    templateTT.setItems(templateRows);
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

  @Override
  public void mouseClicked(MouseEvent e) {
    if (configTab.isEnabledAt(1)) {
      configTab.setSelectedTab(ConfigEditTabPane.TASK);
    }
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      GUIHelper.disableButton(taskButtonPanel, false);
      isSelected = true;
      fillConnectionCheckboxIsSelected(true);
      fillQueryCheckboxIsSelected(true);
    } else {
      GUIHelper.disableButton(taskButtonPanel, true);
      isSelected = false;
      fillConnectionCheckboxIsSelected(false);
      fillQueryCheckboxIsSelected(false);
    }
  }

  public void fillConnectionCheckboxIsSelected(Boolean isSelected) {
    connectionCase.clearTable();
    TTTable<ConnectionRow, JXTable> tt = connectionCase.getTypedTable();
    List<ConnectionRow> rows = new ArrayList<>();

    if (taskCase.getJxTable().getRowCount() > 0) {
      if (taskCase.getJxTable().getSelectedRow() == -1) {
        taskCase.getJxTable().setRowSelectionInterval(0, 0);
      }
    }

    if (isSelected) {
      if (profileCase.getJxTable().getRowCount() > 0) {
        int taskId = getSelectedTaskId();
        if (taskId != -1) {
          TaskInfo task = profileManager.getTaskInfoById(taskId);
          if (Objects.isNull(task)) {
            throw new NotFoundException("Not found task: " + taskId);
          }

          ConnectionInfo connection = profileManager.getConnectionInfoById(task.getConnectionId());
          if (Objects.isNull(connection)) {
            throw new NotFoundException("Not found connection: " + task.getConnectionId());
          }

          ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(connection.getId());

          rows.add(new ConnectionRow(
              connection.getId(),
              connection.getName(),
              connection.getType(),
              connectionInfo.getDbType()
          ));
        }
      }
    } else {
      rows = profileManager.getConnectionInfoList().stream()
          .map(c -> {
            ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(c.getId());
            return new ConnectionRow(
                c.getId(),
                c.getName(),
                c.getType(),
                connectionInfo.getDbType()
            );
          })
          .collect(Collectors.toList());
    }

    tt.setItems(rows);
  }

  public void fillQueryCheckboxIsSelected(Boolean isSelected) {
    queryCase.clearTable();
    TTTable<QueryRow, JXTable> tt = queryCase.getTypedTable();
    List<QueryRow> rows = new ArrayList<>();

    if (taskCase.getJxTable().getRowCount() > 0) {
      if (taskCase.getJxTable().getSelectedRow() == -1) {
        taskCase.getJxTable().setRowSelectionInterval(0, 0);
      }
    }

    if (isSelected) {
      if (taskCase.getJxTable().getRowCount() > 0) {
        int taskId = getSelectedTaskId();
        if (taskId != -1) {
          if (Objects.isNull(profileManager.getTaskInfoById(taskId))) {
            throw new NotFoundException("Not found task: " + taskId);
          } else {
            rows = profileManager.getTaskInfoById(taskId).getQueryInfoList().stream()
                .map(queryId -> {
                  QueryInfo queryIn = profileManager.getQueryInfoById(queryId);
                  if (Objects.isNull(queryIn)) {
                    throw new NotFoundException("Not found query: " + queryId);
                  }
                  return new QueryRow(queryIn.getId(), queryIn.getName());
                })
                .collect(Collectors.toList());
          }
        }
      }
    } else {
      rows = profileManager.getQueryInfoList().stream()
          .map(e -> new QueryRow(e.getId(), e.getName()))
          .collect(Collectors.toList());
    }

    tt.setItems(rows);
  }

  @Override
  public void stateChanged(ChangeEvent changeEvent) {
    if (!isSelected) {
      if (configTab.getSelectedIndex() == 1) {
        if (taskId == -1) {
          clearMultiSelectionPanel();

          multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);

          taskPanel.getJTextFieldTask().setEditable(false);
          taskPanel.getJTextFieldDescription().setEditable(false);
          taskPanel.getTaskConnectionComboBox().setEnabled(false);
          taskPanel.getRadioButtonPanel().setButtonNotView();
          multiSelectQueryPanel.getUnPickBtn().setEnabled(false);
          multiSelectQueryPanel.getPickBtn().setEnabled(false);
          multiSelectQueryPanel.getPickAllBtn().setEnabled(false);
          multiSelectQueryPanel.getUnPickAllBtn().setEnabled(false);

          List<ConnectionInfo> connectionAll = profileManager.getConnectionInfoList();
          if (!connectionAll.isEmpty()) {
            this.connectionDataList = new LinkedList<>();
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

            taskPanel.getJTextFieldTask().setText("");
            taskPanel.getJTextFieldTask().setPrompt(bundleDefault.getString("tName"));
            taskPanel.getJTextFieldDescription().setText("");
            taskPanel.getJTextFieldDescription().setPrompt(bundleDefault.getString("tDesc"));
            taskPanel.getTaskConnectionComboBox().setTableData(connectionDataList);
            taskPanel.getTaskConnectionComboBox().setEnabled(false);

            String connName = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(0).toString();
            Object connDriver = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4);

            if (connDriver != null) {
              fillAvailableQueryList(connDriver.toString());
            }

          } else {
            taskPanel.getTaskConnectionComboBox().setTableData(java.util.Collections.emptyList());
          }
        } else {
          clearMultiSelectionPanel();

          TaskInfo taskInfo = profileManager.getTaskInfoById(taskId);
          if (Objects.isNull(taskInfo)) {
            throw new NotFoundException("Not found task: " + taskId);
          }

          taskPanel.getJTextFieldTask().setText(taskInfo.getName());
          taskPanel.getJTextFieldDescription().setText(taskInfo.getDescription());
          taskPanel.getRadioButtonPanel().setSelectedRadioButton(taskInfo.getPullTimeout() + " sec");

          ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(taskInfo.getConnectionId());
          if (Objects.isNull(connectionInfo)) {
            throw new NotFoundException("Not found connection: " + taskInfo.getConnectionId());
          }
          List<ConnectionInfo> connectionAll = profileManager.getConnectionInfoList();
          List<List<?>> connectionData = new LinkedList<>();
          if (connectionInfo.getType() != null) {
            connectionData.add(
                new ArrayList<>(Arrays.asList(connectionInfo.getName(), connectionInfo.getUserName(),
                                              connectionInfo.getUrl(), connectionInfo.getJar(),
                                              connectionInfo.getDriver(), connectionInfo.getType())));
          } else {
            connectionData.add(
                new ArrayList<>(Arrays.asList(connectionInfo.getName(), connectionInfo.getUserName(),
                                              connectionInfo.getUrl(), connectionInfo.getJar(),
                                              connectionInfo.getDriver(), JDBC)));
          }
          connectionAll.stream()
              .filter(f -> f.getId() != connectionInfo.getId())
              .forEach(connection -> {
                String type = connection.getType() != null ? String.valueOf(connection.getType()) : JDBC.getName();
                connectionData.add(Arrays.asList(
                    connection.getName(), connection.getUserName(),
                    connection.getUrl(), connection.getJar(),
                    connection.getDriver(), type
                ));
              });

          taskPanel.getTaskConnectionComboBox().setTableData(connectionData);

          List<QueryTableRow> selectedRows = profileManager.getTaskInfoById(taskId)
              .getQueryInfoList()
              .stream()
              .map(queryId -> {
                QueryInfo queryInfo = profileManager.getQueryInfoById(queryId);
                if (Objects.isNull(queryInfo)) {
                  throw new NotFoundException("Not found query: " + queryId);
                }
                return new QueryTableRow(
                    queryInfo.getId(),
                    queryInfo.getName(),
                    queryInfo.getDescription(),
                    queryInfo.getText()
                );
              })
              .collect(Collectors.toList());

          TTTable<QueryTableRow, JXTable> selectedTT = multiSelectQueryPanel.getSelectedQueryCase().getTypedTable();
          selectedTT.setItems(selectedRows);

          String connName = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(0).toString();
          Object connDriver = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4);

          if (connDriver != null) {
            log.info("Connection's {} driver: {}", connName, connDriver);
            fillAvailableQueryList(connDriver.toString());
          }
          fillConnectionCheckboxIsSelected(isSelected);
          fillQueryCheckboxIsSelected(isSelected);

          GUIHelper.disableButton(taskButtonPanel, !isSelected);
        }
      }
    }
  }
}