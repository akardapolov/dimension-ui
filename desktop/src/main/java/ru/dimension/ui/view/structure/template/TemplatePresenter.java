package ru.dimension.ui.view.structure.template;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import lombok.extern.log4j.Log4j2;
import org.apache.hc.core5.http.Method;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTextArea;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swingx.JXTableTables;
import ru.dimension.ui.exception.EmptyNameException;
import ru.dimension.ui.exception.EntityExistException;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.config.ConfigClasses;
import ru.dimension.ui.model.config.ConfigEntity;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Table;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.ConnectionAddEvent;
import ru.dimension.ui.bus.event.ProfileAddEvent;
import ru.dimension.ui.router.listener.TemplateListener;
import ru.dimension.ui.security.EncryptDecrypt;
import ru.dimension.ui.view.structure.TemplateView;
import ru.dimension.ui.view.tab.ConnTypeTab;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.parse.ParseType;
import ru.dimension.ui.model.view.TemplateAction;
import ru.dimension.ui.model.view.TemplateState;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.panel.template.TemplateConnPanel;
import ru.dimension.ui.view.panel.template.TemplateEditPanel;
import ru.dimension.ui.view.panel.template.TemplateHTTPConnPanel;
import ru.dimension.ui.view.table.row.Rows.TemplateConnectionRow;
import ru.dimension.ui.view.table.row.Rows.TemplateMetricRow;
import ru.dimension.ui.view.table.row.Rows.TemplateQueryRow;
import ru.dimension.ui.view.table.row.Rows.TemplateTaskRow;

@Log4j2
@Singleton
public class TemplatePresenter extends WindowAdapter
    implements TemplateListener, ListSelectionListener, ActionListener, FocusListener, CellEditorListener, KeyListener {

  private final TemplateView templateView;
  private final EventListener eventListener;
  private final EventBus eventBus;
  private final ProfileManager profileManager;
  private final ConfigurationManager configurationManager;
  private final TemplateManager templateManager;
  private final EncryptDecrypt encryptDecrypt;

  private final JButton templateLoadJButton;
  private final JButton templateSaveJButton;
  private final TemplateEditPanel templateEditPanel;

  private final TTTable<TemplateTaskRow, JXTable> templateTaskTable;
  private final TTTable<TemplateConnectionRow, JXTable> templateConnTable;
  private final TTTable<TemplateQueryRow, JXTable> templateQueryTable;
  private final TTTable<TemplateMetricRow, JXTable> templateMetricsTable;

  private final TemplateConnPanel templateConnPanel;
  private final TemplateHTTPConnPanel templateHTTPConnPanel;
  private final ConnTypeTab connectionTabPane;

  private final JXTextArea taskDescription;
  private final JXTextArea queryDescription;
  private final RSyntaxTextArea queryText;
  private final DefaultCellEditor cellEditor;
  private List<String> arrText;


  @Inject
  public TemplatePresenter(@Named("templateView") TemplateView templateView,
                           @Named("eventListener") EventListener eventListener,
                           @Named("eventBus") EventBus eventBus,
                           @Named("profileManager") ProfileManager profileManager,
                           @Named("configurationManager") ConfigurationManager configurationManager,
                           @Named("templateManager") TemplateManager templateManager,
                           @Named("encryptDecrypt") EncryptDecrypt encryptDecrypt,
                           @Named("templateLoadJButton") JButton templateLoadJButton,
                           @Named("templateSaveJButton") JButton templateSaveJButton,
                           @Named("templateEditPanel") TemplateEditPanel templateEditPanel,
                           @Named("templateTaskCase") TTTable<TemplateTaskRow, JXTable> templateTaskTable,
                           @Named("templateConnCase") TTTable<TemplateConnectionRow, JXTable> templateConnTable,
                           @Named("templateQueryCase") TTTable<TemplateQueryRow, JXTable> templateQueryTable,
                           @Named("templateMetricsCase") TTTable<TemplateMetricRow, JXTable> templateMetricsTable,
                           @Named("templateConnPanel") TemplateConnPanel templateConnPanel,
                           @Named("templateHTTPConnPanel") TemplateHTTPConnPanel templateHTTPConnPanel,
                           @Named("templateConnectionTab") ConnTypeTab connectionTabPane,
                           @Named("templateTaskDescription") JXTextArea taskDescription,
                           @Named("templateQueryDescription") JXTextArea queryDescription,
                           @Named("templateQueryText") RSyntaxTextArea queryText) {
    this.templateView = templateView;
    this.eventListener = eventListener;
    this.eventBus = eventBus;
    this.profileManager = profileManager;
    this.configurationManager = configurationManager;
    this.templateManager = templateManager;
    this.encryptDecrypt = encryptDecrypt;

    this.templateLoadJButton = templateLoadJButton;
    this.templateSaveJButton = templateSaveJButton;
    this.templateLoadJButton.addActionListener(this);
    this.templateSaveJButton.addActionListener(this);

    this.templateEditPanel = templateEditPanel;

    this.templateTaskTable = templateTaskTable;
    this.templateConnTable = templateConnTable;
    this.templateQueryTable = templateQueryTable;

    this.templateTaskTable.table().getSelectionModel().addListSelectionListener(this);
    this.templateConnTable.table().getSelectionModel().addListSelectionListener(this);
    this.templateQueryTable.table().getSelectionModel().addListSelectionListener(this);

    this.templateConnPanel = templateConnPanel;
    this.templateHTTPConnPanel = templateHTTPConnPanel;
    this.templateMetricsTable = templateMetricsTable;

    this.connectionTabPane = connectionTabPane;

    this.taskDescription = taskDescription;
    this.queryDescription = queryDescription;
    this.queryText = queryText;

    this.eventListener.addTemplateStateListener(this);

    this.templateEditPanel.getProfileName().addFocusListener(this);
    this.templateEditPanel.getTaskName().addFocusListener(this);
    this.templateEditPanel.getConnName().addFocusListener(this);

    this.cellEditor = new DefaultCellEditor(new JTextField());
    cellEditor.addCellEditorListener(this);
    this.templateEditPanel.getTemplateQueryCase().getJxTable().getColumnModel().getColumn(0).setCellEditor(cellEditor);

    arrText = new ArrayList<>();

    this.templateEditPanel.getProfileName().addKeyListener(this);
    this.templateEditPanel.getTaskName().addKeyListener(this);
    this.templateEditPanel.getConnName().addKeyListener(this);
    this.templateEditPanel.getConnUserName().addKeyListener(this);
    this.templateEditPanel.getConnPassword().addKeyListener(this);
    this.templateEditPanel.getConnUrl().addKeyListener(this);
  }

  @Override
  public void fireShowTemplate(TemplateState templateState) {
    if (templateState == TemplateState.SHOW) {
      this.templateView.showTemplate();
    }
    if (templateState == TemplateState.HIDE) {
      this.templateView.hideTemplate();
    }
  }

  @Override
  public void windowClosing(WindowEvent e) {
    log.info("Window template closing event received");
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

    if (!e.getValueIsAdjusting()) {
      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing task fields");
      } else {
        if (e.getSource() == templateTaskTable.table().getSelectionModel()) {
          log.info("Fire on tasks..");
          int selectedRow = templateTaskTable.table().getSelectedRow();
          if (selectedRow == -1) return;
          int modelRow = templateTaskTable.table().convertRowIndexToModel(selectedRow);
          TemplateTaskRow row = templateTaskTable.model().itemAt(modelRow);
          int taskId = row.getId();

          Task task = templateManager.getConfigList(Task.class)
              .stream()
              .filter(f -> f.getId() == taskId)
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found task: " + taskId));

          Connection connection = templateManager.getConfigList(Connection.class)
              .stream()
              .filter(f -> f.getId() == task.getConnectionId())
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found connection: " + task.getConnectionId()));

          List<Query> queryList = templateManager.getConfigList(Query.class);

          List<TemplateQueryRow> queryRows = task.getQueryList().stream()
              .flatMap(queryId -> queryList.stream()
                  .filter(f -> f.getId() == queryId)
                  .map(q -> new TemplateQueryRow(q.getId(), q.getName(), q.getGatherDataMode().name())))
              .toList();
          templateQueryTable.setItems(queryRows);

          taskDescription.setText(task.getDescription());

          List<TemplateConnectionRow> connRows = Collections.singletonList(
              new TemplateConnectionRow(connection.getId(), connection.getName(),
                                        connection.getType() != null ? connection.getType().getName() : ru.dimension.ui.model.type.ConnectionType.JDBC.getName())
          );
          templateConnTable.setItems(connRows);

          if (connection.getType() == null || connection.getType().equals(ru.dimension.ui.model.type.ConnectionType.JDBC)) {
            templateConnPanel.getConnectionName().setText(connection.getName());
            templateConnPanel.getConnectionURL().setText(connection.getUrl());
            templateConnPanel.getConnectionUserName().setText(connection.getUserName());
            templateConnPanel.getConnectionJar().setText(connection.getJar());
            templateConnPanel.getConnectionDriver().setText(connection.getDriver());

            templateHTTPConnPanel.setEmpty();
            connectionTabPane.setSelectedTab(ConnectionTypeTabPane.JDBC);
            connectionTabPane.setEnabledTab(ConnectionTypeTabPane.JDBC, true);
            connectionTabPane.setEnabledTab(ConnectionTypeTabPane.HTTP, false);
          } else if (connection.getType().equals(ru.dimension.ui.model.type.ConnectionType.HTTP)) {
            templateHTTPConnPanel.getConnectionName().setText(connection.getName());
            templateHTTPConnPanel.getConnectionURL().setText(connection.getUrl());

            templateConnPanel.setEmpty();
            connectionTabPane.setSelectedTab(ConnectionTypeTabPane.HTTP);
            connectionTabPane.setEnabledTab(ConnectionTypeTabPane.HTTP, true);
            connectionTabPane.setEnabledTab(ConnectionTypeTabPane.JDBC, false);
          }

          templateConnTable.table().setRowSelectionInterval(0, 0);
          templateQueryTable.table().setRowSelectionInterval(0, 0);

          log.info("Task ID: {}", taskId);
        }

        if (e.getSource() == templateConnTable.table().getSelectionModel()) {
          log.info("Fire on connections..");
        }
        if (e.getSource() == templateQueryTable.table().getSelectionModel()) {
          log.info("Fire on queries..");

          int selectedRow = templateQueryTable.table().getSelectedRow();
          if (selectedRow == -1) return;
          int modelRow = templateQueryTable.table().convertRowIndexToModel(selectedRow);
          TemplateQueryRow row = templateQueryTable.model().itemAt(modelRow);
          int queryId = row.getId();

          Query query = templateManager.getConfigList(Query.class)
              .stream()
              .filter(f -> f.getId() == queryId)
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found query: " + queryId));

          List<TemplateMetricRow> metricRows = query.getMetricList().stream()
              .map(m -> new TemplateMetricRow(
                  m.getId(),
                  m.getName(),
                  m.getIsDefault(),
                  m.getXAxis() != null ? m.getXAxis().getColName() : "",
                  m.getYAxis() != null ? m.getYAxis().getColName() : "",
                  m.getGroup() != null ? m.getGroup().getColName() : "",
                  m.getGroupFunction() != null ? m.getGroupFunction().toString() : "",
                  m.getChartType() != null ? m.getChartType().toString() : ""
              ))
              .toList();
          templateMetricsTable.setItems(metricRows);

          queryDescription.setText(query.getDescription());
          queryText.setText(query.getText());
        }
      }
    }
  }

  public <T> void fillModel(Class<T> clazz) {
    if (ConfigClasses.Task.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Task..");

      List<TemplateTaskRow> taskRows = templateManager.getConfigList(Task.class)
          .stream()
          .map(e -> new TemplateTaskRow(e.getId(), e.getName(), e.getPullTimeout() + " sec."))
          .toList();
      templateTaskTable.setItems(taskRows);

      templateTaskTable.table().setRowSelectionInterval(0, 0);
    }

    if (ConfigClasses.Connection.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Connection..");
    }

    if (ConfigClasses.Query.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Query..");
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    this.arrText.clear();
    int selectedTaskRow = templateTaskTable.table().getSelectedRow();
    if (selectedTaskRow == -1) return;
    int modelTaskRow = templateTaskTable.table().convertRowIndexToModel(selectedTaskRow);
    TemplateTaskRow taskRow = templateTaskTable.model().itemAt(modelTaskRow);
    int taskId = taskRow.getId();

    Task task = templateManager.getConfigList(Task.class)
        .stream()
        .filter(f -> f.getId() == taskId)
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found task: " + taskId));

    int selectedConnRow = templateConnTable.table().getSelectedRow();
    if (selectedConnRow == -1) return;
    int modelConnRow = templateConnTable.table().convertRowIndexToModel(selectedConnRow);
    TemplateConnectionRow connRow = templateConnTable.model().itemAt(modelConnRow);
    int connId = connRow.getId();

    Connection connection = templateManager.getConfigList(Connection.class)
        .stream()
        .filter(f -> f.getId() == connId)
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found connection: " + connId));

    int selectedQueryRow = templateQueryTable.table().getSelectedRow();
    if (selectedQueryRow == -1) return;
    int modelQueryRow = templateQueryTable.table().convertRowIndexToModel(selectedQueryRow);
    TemplateQueryRow queryRow = templateQueryTable.model().itemAt(modelQueryRow);
    int queryId = queryRow.getId();

    Query query = templateManager.getConfigList(Query.class)
        .stream()
        .filter(f -> f.getId() == queryId)
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found query: " + queryId));

    if (e.getActionCommand().equals(TemplateAction.LOAD.name())) {

      templateEditPanel.getProfileName().setText("");
      templateEditPanel.getProfileDesc().setText("");

      templateEditPanel.getTaskName().setText(task.getName());
      templateEditPanel.getTaskDesc().setText(task.getDescription());

      if (connection.getType() == null || connection.getType().equals(ConnectionType.JDBC)) {
        templateEditPanel.getConnName().setText(connection.getName());
        templateEditPanel.getConnUserName().setText(connection.getUserName());
        templateEditPanel.getConnUrl().setText(connection.getUrl());
        templateEditPanel.getConnJar().setText(connection.getJar());

        templateEditPanel.setEmptyHttpPanel();
        templateEditPanel.getConnTypeTab().setSelectedTab(ConnectionTypeTabPane.JDBC);
        templateEditPanel.getConnTypeTab().setEnabledTab(ConnectionTypeTabPane.JDBC, true);
        templateEditPanel.getConnTypeTab().setEnabledTab(ConnectionTypeTabPane.HTTP, false);
      } else if (connection.getType().equals(ConnectionType.HTTP)) {
        templateEditPanel.getJTextFieldHttpName().setText(connection.getName());
        templateEditPanel.getJTextFieldHttpURL().setText(connection.getUrl());
        templateEditPanel.getMethodRadioButtonPanel().setSelectedRadioButton(connection.getHttpMethod());
        templateEditPanel.getParseRadioButtonPanel().setSelectedRadioButton(connection.getParseType());

        templateEditPanel.setEmptyJdbcPanel();
        templateEditPanel.getConnTypeTab().setSelectedTab(ConnectionTypeTabPane.HTTP);
        templateEditPanel.getConnTypeTab().setEnabledTab(ConnectionTypeTabPane.HTTP, true);
        templateEditPanel.getConnTypeTab().setEnabledTab(ConnectionTypeTabPane.JDBC, false);
      }

      templateEditPanel.getQueryName().setText(query.getName());
      templateEditPanel.getQueryDesc().setText(query.getDescription());
      templateEditPanel.getStatusQuery().setText("Query already exist");

      List<Query> queryList = templateManager.getConfigList(Query.class)
          .stream()
          .filter(f -> task.getQueryList().stream().anyMatch(q -> q == f.getId()))
          .toList();
      templateEditPanel.updateModelTemplateEditQueryCase(queryList);

      changeStatusIfEntityExist(Profile.class, templateEditPanel.getProfileName().getText());

      changeStatusIfEntityExist(Task.class, templateEditPanel.getTaskName().getText());

      if (connection.getType() == null || connection.getType().equals(ru.dimension.ui.model.type.ConnectionType.JDBC)) {
        changeStatusIfEntityExist(Connection.class, templateEditPanel.getConnName().getText());
      } else if (connection.getType().equals(ru.dimension.ui.model.type.ConnectionType.HTTP)) {
        changeStatusIfEntityExist(Connection.class, templateEditPanel.getJTextFieldHttpName().getText());
      }

      DefaultTableModel defaultTableModel = templateEditPanel.getTemplateQueryCase().getDefaultTableModel();

      for (int rowIndex = 0; rowIndex < defaultTableModel.getRowCount(); rowIndex++) {
        String queryName = (String) defaultTableModel.getValueAt(rowIndex, 1);
        changeStatusIfEntityExist(Query.class, queryName);
      }
      templateEditPanel.setVisible(true);
    } else if (e.getActionCommand().equals(TemplateAction.SAVE.name())) {

      if (!templateEditPanel.getProfileName().getText().trim().isEmpty() &&
          !templateEditPanel.getTaskName().getText().trim().isEmpty() &&
          (!templateEditPanel.getConnName().getText().trim().isEmpty() ||
              !templateEditPanel.getJTextFieldHttpName().getText().trim().isEmpty())) {
        raiseAnErrorIfEntityExist(Profile.class, templateEditPanel.getProfileName().getText());

        raiseAnErrorIfEntityExist(Task.class, templateEditPanel.getTaskName().getText());

        if (connection.getType() == null || connection.getType().equals(ru.dimension.ui.model.type.ConnectionType.JDBC)) {
          raiseAnErrorIfEntityExist(Connection.class, templateEditPanel.getConnName().getText());
        } else if (connection.getType().equals(ru.dimension.ui.model.type.ConnectionType.HTTP)) {
          raiseAnErrorIfEntityExist(Connection.class, templateEditPanel.getJTextFieldHttpName().getText());
        }

        DefaultTableModel defaultTableModel = templateEditPanel.getTemplateQueryCase().getDefaultTableModel();

        for (int rowIndex = 0; rowIndex < defaultTableModel.getRowCount(); rowIndex++) {
          String queryName = (String) defaultTableModel.getValueAt(rowIndex, 1);
          raiseAnErrorIfEntityExist(Query.class, queryName);
        }

        List<Integer> quiryIdList = new ArrayList<>();

        int queryMaxId = configurationManager.getConfigList(Query.class)
            .stream()
            .map(ru.dimension.ui.model.config.Query::getId)
            .reduce(Integer::max)
            .orElse(0) + 1;

        for (int rowIndex = 0; rowIndex < defaultTableModel.getRowCount(); rowIndex++) {
          Integer queryTemplateId = (Integer) defaultTableModel.getValueAt(rowIndex, 0);
          String queryTemplateName = (String) defaultTableModel.getValueAt(rowIndex, 1);
          String queryTemplateDesc = (String) defaultTableModel.getValueAt(rowIndex, 2);

          Query saveQuery = templateManager.getConfigList(Query.class)
              .stream()
              .filter(f -> f.getId() == queryTemplateId)
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found query template by queryId: " + queryTemplateId));

          saveQuery.setId(queryMaxId);
          saveQuery.setName(queryTemplateName);
          saveQuery.setDescription(queryTemplateDesc);

          configurationManager.addConfig(saveQuery, Query.class);

          Table table = new Table();
          table.setTableName(saveQuery.getName());
          configurationManager.addConfig(table, Table.class);

          quiryIdList.add(queryMaxId);

          queryMaxId++;
        }

        int connMaxId = configurationManager.getConfigList(Connection.class)
            .stream()
            .map(ru.dimension.ui.model.config.Connection::getId)
            .reduce(Integer::max)
            .orElse(0) + 1;

        connection.setId(connMaxId);

        if (connection.getType() == null || connection.getType().equals(ru.dimension.ui.model.type.ConnectionType.JDBC)) {
          connection.setName(templateEditPanel.getConnName().getText());
          connection.setUrl(templateEditPanel.getConnUrl().getText());
          connection.setJar(templateEditPanel.getConnJar().getText());
          connection.setUserName(templateEditPanel.getConnUserName().getText());
          connection.setPassword(encryptDecrypt.encrypt(String.valueOf(templateEditPanel.getConnPassword()
                                                                           .getPassword())));
        } else if (connection.getType().equals(ru.dimension.ui.model.type.ConnectionType.HTTP)) {
          connection.setName(templateEditPanel.getJTextFieldHttpName().getText());
          connection.setUrl(templateEditPanel.getJTextFieldHttpURL().getText());

          JRadioButton selectedMethod = GUIHelper.getSelectedButton(templateEditPanel.getMethodRadioButtonPanel()
                                                                        .getButtonGroup());
          connection.setHttpMethod(Method.normalizedValueOf(selectedMethod.getText()));

          JRadioButton selectedParse = GUIHelper.getSelectedButton(templateEditPanel.getParseRadioButtonPanel()
                                                                       .getButtonGroup());
          ParseType parseType = ParseType.valueOf(selectedParse.getText().toUpperCase());
          connection.setParseType(parseType);
        }

        configurationManager.addConfig(connection, Connection.class);

        eventBus.publish(new ConnectionAddEvent(connection.getId(), connection.getName(), connection.getType()));

        int taskMaxId = configurationManager.getConfigList(Task.class)
            .stream()
            .map(ru.dimension.ui.model.config.Task::getId)
            .reduce(Integer::max)
            .orElse(0) + 1;

        task.setId(taskMaxId);
        task.setName(templateEditPanel.getTaskName().getText());
        task.setDescription(templateEditPanel.getTaskDesc().getText());
        task.setConnectionId(connMaxId);
        task.setQueryList(quiryIdList);

        configurationManager.addConfig(task, Task.class);

        int profileMaxId = configurationManager.getConfigList(Profile.class)
            .stream()
            .map(Profile::getId)
            .reduce(Integer::max)
            .orElse(0) + 1;

        Profile profile = new Profile();
        profile.setId(profileMaxId);
        profile.setName(templateEditPanel.getProfileName().getText());
        profile.setDescription(templateEditPanel.getProfileDesc().getText());
        profile.setTaskList(Collections.singletonList(taskMaxId));

        configurationManager.addConfig(profile, Profile.class);

        profileManager.updateCache();

        eventBus.publish(new ProfileAddEvent());

        templateEditPanel.setVisible(false);
      } else {
        throw new EmptyNameException("The name field is empty");
      }
    }
  }

  private <T> void raiseAnErrorIfEntityExist(Class<? extends ConfigEntity> clazz,
                                             String entityName) {
    configurationManager.getConfigList(clazz)
        .stream()
        .filter(f -> f.getName().equalsIgnoreCase(entityName))
        .findAny()
        .ifPresentOrElse(profile -> {
          throw new EntityExistException("Entity " + clazz.getSimpleName() + " with name " + entityName
                                             + " already exist. Choose another one..");
        }, () -> {
        });
  }

  private <T> void changeStatusIfEntityExist(Class<? extends ConfigEntity> clazz,
                                             String entityName) {
    configurationManager.getConfigList(clazz)
        .stream()
        .filter(f -> f.getName().trim().equalsIgnoreCase(entityName.trim()))
        .findAny()
        .ifPresentOrElse(profile -> {
          viewStatus(clazz, entityName);
          templateEditPanel.getTemplateSaveJButton().setEnabled(false);
        }, () -> {
          hideStatus(clazz, entityName);
        });
  }

  private void viewStatus(Class<? extends ConfigEntity> clazz,
                          String entityName) {
    if (clazz.getSimpleName().equalsIgnoreCase("Task")) {
      templateEditPanel.getStatusTask().setText("Task " + entityName.trim() + " already exist");
      templateEditPanel.getStatusTask().setVisible(true);

    } else if (clazz.getSimpleName().equalsIgnoreCase("Connection")) {
      templateEditPanel.getStatusConn().setText("Connection " + entityName.trim() + " already exist");
      templateEditPanel.getStatusConn().setVisible(true);

    } else if (clazz.getSimpleName().equalsIgnoreCase("Query")) {

      templateEditPanel.getStatusQuery().setVisible(true);

      if (arrText.size() == 0) {
        arrText.add("Query already exist : ");
        arrText.add("\n" + entityName);
      } else {
        arrText.add("\n" + entityName);
      }
      String text = "";
      for (String arr : arrText) {
        text = text + arr;
      }
      templateEditPanel.getStatusQuery().setText(text);
    }
  }

  private void hideStatus(Class<? extends ConfigEntity> clazz,
                          String entityName) {
    if (clazz.getSimpleName().equalsIgnoreCase("Task")) {
      templateEditPanel.getStatusTask().setVisible(false);
    } else if (clazz.getSimpleName().equalsIgnoreCase("Connection")) {
      templateEditPanel.getStatusConn().setVisible(false);
    } else if (clazz.getSimpleName().equalsIgnoreCase("Query")) {
      if (arrText.size() < 1) {
        templateEditPanel.getStatusQuery().setVisible(false);
      }
    }

    if (templateEditPanel.isVisible()
        && !templateEditPanel.getStatusTask().isVisible()
        && !templateEditPanel.getStatusConn().isVisible()
        && !templateEditPanel.getStatusQuery().isVisible()) {
      templateEditPanel.getTemplateSaveJButton().setEnabled(true);
    }
  }

  @Override
  public void focusGained(FocusEvent focusEvent) {
  }

  @Override
  public void focusLost(FocusEvent focusEvent) {
    changeStatusIfEntityExist(Profile.class, templateEditPanel.getProfileName().getText());
    changeStatusIfEntityExist(Task.class, templateEditPanel.getTaskName().getText());
    changeStatusIfEntityExist(Connection.class, templateEditPanel.getConnName().getText());
  }

  @Override
  public void editingStopped(ChangeEvent e) {
    DefaultTableModel defaultTableModel = templateEditPanel.getTemplateQueryCase().getDefaultTableModel();
    arrText.clear();
    for (int rowIndex = 0; rowIndex < defaultTableModel.getRowCount(); rowIndex++) {
      String queryName = (String) defaultTableModel.getValueAt(rowIndex, 1);
      changeStatusIfEntityExist(Query.class, queryName);
    }
  }

  @Override
  public void editingCanceled(ChangeEvent e) {
  }

  @Override
  public void keyTyped(KeyEvent keyEvent) {
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
      if (templateEditPanel.getProfileName().hasFocus()) {
        templateEditPanel.getProfileDesc().requestFocus();
      }
      if (templateEditPanel.getTaskName().hasFocus()) {
        templateEditPanel.getTaskDesc().requestFocus();
      }
      if (templateEditPanel.getConnName().hasFocus()) {
        templateEditPanel.getConnUserName().requestFocus();
      }
      if (templateEditPanel.getConnUserName().hasFocus()) {
        templateEditPanel.getConnPassword().requestFocus();
      }
      if (templateEditPanel.getConnPassword().hasFocus()) {
        templateEditPanel.getConnUrl().requestFocus();
      }
      if (templateEditPanel.getConnUrl().hasFocus()) {
        templateEditPanel.getConnJar().requestFocus();
      }
    }
  }

  @Override
  public void keyReleased(KeyEvent keyEvent) {
  }

  public TTTable<TemplateTaskRow, JXTable> getTemplateTaskTable() { return templateTaskTable; }
  public TTTable<TemplateConnectionRow, JXTable> getTemplateConnTable() { return templateConnTable; }
  public TTTable<TemplateQueryRow, JXTable> getTemplateQueryTable() { return templateQueryTable; }
  public TTTable<TemplateMetricRow, JXTable> getTemplateMetricsTable() { return templateMetricsTable; }
}