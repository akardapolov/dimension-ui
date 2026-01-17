package ru.dimension.ui.view.handler.connection;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.File;
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
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import lombok.extern.log4j.Log4j2;
import org.apache.hc.core5.http.Method;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.ConnectionAddEvent;
import ru.dimension.ui.bus.event.ConnectionRemoveEvent;
import ru.dimension.ui.exception.EmptyNameException;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.parse.ParseType;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.handler.LifeCycleStatus;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.security.EncryptDecrypt;
import ru.dimension.ui.view.handler.core.ConfigSelectionContext;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.connection.ConnectionPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;

@Log4j2
@Singleton
public final class ConnectionButtonPanelHandler implements ChangeListener {

  private final ProfileManager profileManager;
  private final EventBus eventBus;
  private final EncryptDecrypt encryptDecrypt;
  private final ConfigSelectionContext context;

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final JXTableCase connectionTemplateCase;

  private final ConnectionPanel connectionPanel;
  private final ButtonPanel connectionButtonPanel;
  private final ConfigTab configTab;
  private final JCheckBox checkboxConfig;

  private final JFileChooser jarFC;
  private final ResourceBundle bundleDefault;

  private LifeCycleStatus status;
  private boolean isPasswordChanged;
  private ConnectionInfo oldFileConnection;

  private ConnectionTypeTabPane openedTab;

  @Inject
  public ConnectionButtonPanelHandler(@Named("profileManager") ProfileManager profileManager,
                                      @Named("eventBus") EventBus eventBus,
                                      @Named("encryptDecrypt") EncryptDecrypt encryptDecrypt,
                                      @Named("configSelectionContext") ConfigSelectionContext context,
                                      @Named("profileConfigCase") JXTableCase profileCase,
                                      @Named("taskConfigCase") JXTableCase taskCase,
                                      @Named("connectionConfigCase") JXTableCase connectionCase,
                                      @Named("connectionTemplateCase") JXTableCase connectionTemplateCase,
                                      @Named("queryConfigCase") JXTableCase queryCase,
                                      @Named("connectionConfigPanel") ConnectionPanel connectionPanel,
                                      @Named("connectionButtonPanel") ButtonPanel connectionButtonPanel,
                                      @Named("configTab") ConfigTab configTab,
                                      @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.profileManager = profileManager;
    this.eventBus = eventBus;
    this.encryptDecrypt = encryptDecrypt;
    this.context = context;

    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.connectionTemplateCase = connectionTemplateCase;
    this.queryCase = queryCase;

    this.connectionPanel = connectionPanel;
    this.connectionButtonPanel = connectionButtonPanel;
    this.configTab = configTab;
    this.checkboxConfig = checkboxConfig;

    this.bundleDefault = Internationalization.getInternationalizationBundle();
    this.jarFC = new JFileChooser();

    this.status = LifeCycleStatus.NONE;
    this.isPasswordChanged = false;
    this.openedTab = ConnectionTypeTabPane.JDBC;

    this.connectionPanel.getConnTypeTab().addChangeListener(this);

    this.connectionButtonPanel.getBtnNew().addActionListener(e -> onNew());
    this.connectionButtonPanel.getBtnCopy().addActionListener(e -> onCopy());
    this.connectionButtonPanel.getBtnDel().addActionListener(e -> onDelete());
    this.connectionButtonPanel.getBtnEdit().addActionListener(e -> onEdit());
    this.connectionButtonPanel.getBtnSave().addActionListener(e -> onSave());
    this.connectionButtonPanel.getBtnCancel().addActionListener(e -> onCancel());

    this.connectionPanel.getJarButton().addActionListener(e -> onPickJar());

    this.connectionButtonPanel.getBtnDel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    this.connectionButtonPanel.getBtnDel().getActionMap().put("delete", new AbstractAction() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        connectionButtonPanel.getBtnDel().doClick();
      }
    });

    this.connectionButtonPanel.getBtnCancel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    this.connectionButtonPanel.getBtnCancel().getActionMap().put("cancel", new AbstractAction() {
      @Override
      public void actionPerformed(java.awt.event.ActionEvent e) {
        connectionButtonPanel.getBtnCancel().doClick();
      }
    });

    this.connectionPanel.getJTextFieldConnectionPassword()
        .getDocument()
        .addDocumentListener((PasswordDocumentListener) e -> {
          if (connectionPanel.getJTextFieldConnectionPassword().isEditable()) {
            isPasswordChanged = true;
          }
        });
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (connectionPanel.getConnTypeTab().getSelectedIndex() == 0) {
      openedTab = ConnectionTypeTabPane.JDBC;
    } else if (connectionPanel.getConnTypeTab().getSelectedIndex() == 1) {
      openedTab = ConnectionTypeTabPane.HTTP;
    }
  }

  private void onPickJar() {
    if (!connectionPanel.getJarButton().isEnabled()) {
      return;
    }

    connectionPanel.getJarButton().setEnabled(false);
    try {
      Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
      int returnVal = jarFC.showOpenDialog(activeWindow);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = jarFC.getSelectedFile();
        connectionPanel.getJTextFieldConnectionJar().setText(file.getAbsolutePath());
      }
    } finally {
      connectionPanel.getJarButton().setEnabled(true);
    }
  }

  private void onNew() {
    status = LifeCycleStatus.NEW;

    clearForm(ConnectionTypeTabPane.JDBC);
    clearForm(ConnectionTypeTabPane.HTTP);

    setEditModeForTab(openedTab, true);
    connectionPanel.getConnTypeTab().setEnabledTab(ConnectionTypeTabPane.JDBC, true);
    connectionPanel.getConnTypeTab().setEnabledTab(ConnectionTypeTabPane.HTTP, true);

    isPasswordChanged = true;
  }

  private void onCopy() {
    status = LifeCycleStatus.COPY;

    Integer id = context.getSelectedConnectionId();
    if (id == null) {
      throw new NotSelectedRowException("The connection to copy is not selected. Please select and try again!");
    }

    ConnectionInfo connection = profileManager.getConnectionInfoById(id);
    if (connection == null) {
      throw new NotFoundException("Not found connection: " + id);
    }

    ConnectionType type = connection.getType() != null ? connection.getType() : ConnectionType.JDBC;
    ConnectionTypeTabPane needTab = ConnectionType.HTTP.equals(type) ? ConnectionTypeTabPane.HTTP : ConnectionTypeTabPane.JDBC;

    if (!needTab.getName().equals(openedTab.getName())) {
      int input = JOptionPane.showOptionDialog(null,
                                               "The " + needTab.getName() + " connection is selected in the table, "
                                                   + "and you are on the " + openedTab.getName() + " tab."
                                                   + " Go to the " + needTab.getName() + " tab and make a copy?",
                                               "Information", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                               null, new String[]{"Yes", "No"}, "Yes");
      if (input != 0) {
        return;
      }
      openedTab = needTab;
    }

    setEditModeForTab(needTab, true);

    if (needTab.equals(ConnectionTypeTabPane.JDBC)) {
      connectionPanel.getJTextFieldConnectionName().setText(connection.getName() + "_copy");
      connectionPanel.getJTextFieldConnectionURL().setText(connection.getUrl());
      connectionPanel.getJTextFieldConnectionUserName().setText(connection.getUserName());
      connectionPanel.getJTextFieldConnectionPassword().setText("");
      connectionPanel.getJTextFieldConnectionJar().setText(connection.getJar());
      connectionPanel.getJTextFieldConnectionDriver().setText(connection.getDriver());
      isPasswordChanged = true;
    } else {
      connectionPanel.getJTextFieldHttpName().setText(connection.getName() + "_copy");
      connectionPanel.getJTextFieldHttpURL().setText(connection.getUrl());
    }
  }

  private void onDelete() {
    Integer id = context.getSelectedConnectionId();
    if (id == null) {
      JOptionPane.showMessageDialog(null, "Not selected connection. Please select and try again!",
                                    "General Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    ConnectionInfo connection = profileManager.getConnectionInfoById(id);
    if (connection == null) {
      throw new NotFoundException("Not found connection: " + id);
    }

    int input = JOptionPane.showConfirmDialog(new JDialog(),
                                              "Do you want to delete configuration: " + connection.getName() + "?");
    if (input != 0) {
      return;
    }

    if (!isNotUsedOnTask(id)) {
      JOptionPane.showMessageDialog(null, "Cannot delete this connection it is used in the task",
                                    "General Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    profileManager.deleteConnection(connection.getId(), connection.getName());
    eventBus.publish(new ConnectionRemoveEvent(id));

    if (ConnectionType.HTTP.equals(connection.getType())) {
      deleteHttpRelatedQuery(connection.getName());
    }

    refillConnectionTableAndSelectFirst();
  }

  private void onEdit() {
    status = LifeCycleStatus.EDIT;

    Integer id = context.getSelectedConnectionId();
    if (id == null) {
      JOptionPane.showMessageDialog(null, "Not selected connection. Please select and try again!",
                                    "General Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    oldFileConnection = profileManager.getConnectionInfoById(id);
    if (oldFileConnection == null) {
      throw new NotFoundException("Not found connection: " + id);
    }

    ConnectionType type = oldFileConnection.getType() != null ? oldFileConnection.getType() : ConnectionType.JDBC;
    ConnectionTypeTabPane needTab = ConnectionType.HTTP.equals(type) ? ConnectionTypeTabPane.HTTP : ConnectionTypeTabPane.JDBC;

    if (!needTab.getName().equals(openedTab.getName())) {
      int input = JOptionPane.showOptionDialog(null,
                                               "The " + needTab.getName() + " connection is selected in the table, "
                                                   + "and you are on the " + openedTab.getName() + " tab."
                                                   + " Go to the " + needTab.getName() + " tab and edit the connection?",
                                               "Information", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                               null, new String[]{"Yes", "No"}, "Yes");
      if (input != 0) {
        return;
      }
      openedTab = needTab;
    }

    setEditModeForTab(needTab, true);

    if (needTab.equals(ConnectionTypeTabPane.JDBC)) {
      tryDecryptPasswordOrForceReset(id);
    }
  }

  private void onSave() {
    if (LifeCycleStatus.NEW.equals(status) || LifeCycleStatus.COPY.equals(status)) {
      saveNew();
      return;
    }
    if (LifeCycleStatus.EDIT.equals(status)) {
      saveEdit();
    }
  }

  private void saveNew() {
    if (!hasNameForOpenedTab()) {
      throw new EmptyNameException("The name field is empty");
    }

    int newId = nextConnectionId();

    String name = openedTab.equals(ConnectionTypeTabPane.JDBC)
        ? connectionPanel.getJTextFieldConnectionName().getText()
        : connectionPanel.getJTextFieldHttpName().getText();

    checkConnectionNameIsBusy(newId, name);

    ConnectionInfo saveConnection = new ConnectionInfo();
    saveConnection.setId(newId);
    saveConnection.setType(ConnectionType.valueOf(openedTab.getName()));

    if (openedTab.equals(ConnectionTypeTabPane.JDBC)) {
      saveConnection.setName(connectionPanel.getJTextFieldConnectionName().getText());
      saveConnection.setUrl(connectionPanel.getJTextFieldConnectionURL().getText());
      saveConnection.setUserName(connectionPanel.getJTextFieldConnectionUserName().getText());
      saveConnection.setJar(connectionPanel.getJTextFieldConnectionJar().getText());
      saveConnection.setDriver(connectionPanel.getJTextFieldConnectionDriver().getText());
      saveConnection.setPassword(encryptDecrypt.encrypt(
          String.valueOf(connectionPanel.getJTextFieldConnectionPassword().getPassword())));
    } else {
      saveConnection.setName(connectionPanel.getJTextFieldHttpName().getText());
      saveConnection.setUrl(connectionPanel.getJTextFieldHttpURL().getText());

      JRadioButton selectedMethod = GUIHelper.getSelectedButton(connectionPanel.getMethodRadioButtonPanel().getButtonGroup());
      saveConnection.setHttpMethod(Method.normalizedValueOf(selectedMethod.getText()));

      JRadioButton selectedParse = GUIHelper.getSelectedButton(connectionPanel.getParseRadioButtonPanel().getButtonGroup());
      saveConnection.setParseType(ParseType.valueOf(selectedParse.getText().toUpperCase()));

      QueryInfo qi = new QueryInfo();
      qi.setId(nextQueryId());
      qi.setName(saveConnection.getName());

      TableInfo tableInfo = new TableInfo();
      tableInfo.setTableName(qi.getName());

      profileManager.addTable(tableInfo);
      profileManager.addQuery(qi);
      updateQueryCaseScrollToBottom();
    }

    profileManager.addConnection(saveConnection);
    eventBus.publish(new ConnectionAddEvent(saveConnection.getId(), saveConnection.getName(), saveConnection.getType()));

    isPasswordChanged = false;
    refillConnectionTableAndSelectId(saveConnection.getId());
    setEditModeForTab(openedTab, false);
  }

  private void saveEdit() {
    Integer id = context.getSelectedConnectionId();
    if (id == null) {
      throw new NotSelectedRowException("Not selected connection. Please select and try again!");
    }

    if (!hasNameForOpenedTab()) {
      throw new EmptyNameException("The name field is empty");
    }

    String name = openedTab.equals(ConnectionTypeTabPane.JDBC)
        ? connectionPanel.getJTextFieldConnectionName().getText()
        : connectionPanel.getJTextFieldHttpName().getText();

    checkConnectionNameIsBusy(id, name);

    ConnectionInfo old = profileManager.getConnectionInfoById(id);
    if (old == null) {
      throw new NotFoundException("Not found connection: " + id);
    }

    ConnectionInfo edit = new ConnectionInfo();
    edit.setId(id);
    edit.setType(ConnectionType.valueOf(openedTab.getName()));

    if (openedTab.equals(ConnectionTypeTabPane.JDBC)) {
      edit.setName(connectionPanel.getJTextFieldConnectionName().getText());
      edit.setUserName(connectionPanel.getJTextFieldConnectionUserName().getText());
      edit.setUrl(connectionPanel.getJTextFieldConnectionURL().getText());
      edit.setJar(connectionPanel.getJTextFieldConnectionJar().getText());
      edit.setDriver(connectionPanel.getJTextFieldConnectionDriver().getText());

      if (isPasswordChanged) {
        edit.setPassword(encryptDecrypt.encrypt(
            String.valueOf(connectionPanel.getJTextFieldConnectionPassword().getPassword())));
      } else {
        edit.setPassword(old.getPassword());
      }
      isPasswordChanged = false;
    } else {
      edit.setName(connectionPanel.getJTextFieldHttpName().getText());
      edit.setUrl(connectionPanel.getJTextFieldHttpURL().getText());

      JRadioButton selectedMethod = GUIHelper.getSelectedButton(connectionPanel.getMethodRadioButtonPanel().getButtonGroup());
      edit.setHttpMethod(Method.normalizedValueOf(selectedMethod.getText()));

      JRadioButton selectedParse = GUIHelper.getSelectedButton(connectionPanel.getParseRadioButtonPanel().getButtonGroup());
      edit.setParseType(ParseType.valueOf(selectedParse.getText().toUpperCase()));
    }

    if (!Objects.equals(old.getName(), name)) {
      profileManager.deleteConnection(old.getId(), old.getName());
      eventBus.publish(new ConnectionRemoveEvent(old.getId()));

      profileManager.addConnection(edit);
      eventBus.publish(new ConnectionAddEvent(edit.getId(), edit.getName(), edit.getType()));

      if (openedTab.equals(ConnectionTypeTabPane.HTTP)) {
        updateHttpRelatedQuery(old.getName(), edit.getName());
      }
    } else {
      profileManager.updateConnection(edit);
    }

    refillConnectionTableAndSelectId(id);
    setEditModeForTab(openedTab, false);
  }

  private void onCancel() {
    if (connectionPanel.getJButtonTemplate().isEnabled()) {
      connectionPanel.getJButtonTemplate().setEnabled(false);
      connectionPanel.getConnectionTemplateCase().getJxTable().clearSelection();

      Integer id = context.getSelectedConnectionId();
      if (id != null) {
        setEditModeForTab(openedTab, false);
        refillConnectionTableAndSelectId(id);
      } else {
        setEditModeForTab(openedTab, false);
        clearForm(openedTab);
      }
      return;
    }

    Integer id = context.getSelectedConnectionId();
    if (id == null) {
      setEditModeForTab(openedTab, false);
      clearForm(openedTab);
      return;
    }

    ConnectionInfo info = profileManager.getConnectionInfoById(id);
    if (info == null) {
      throw new NotFoundException("Not found connection: " + id);
    }

    ConnectionType type = info.getType() != null ? info.getType() : ConnectionType.JDBC;
    ConnectionTypeTabPane tab = ConnectionType.HTTP.equals(type) ? ConnectionTypeTabPane.HTTP : ConnectionTypeTabPane.JDBC;
    openedTab = tab;

    fillForm(info, tab);
    setEditModeForTab(tab, false);
  }

  private void fillForm(ConnectionInfo info, ConnectionTypeTabPane tab) {
    if (tab.equals(ConnectionTypeTabPane.JDBC)) {
      connectionPanel.getJTextFieldConnectionName().setText(info.getName());
      connectionPanel.getJTextFieldConnectionUserName().setText(info.getUserName());
      connectionPanel.getJTextFieldConnectionURL().setText(info.getUrl());
      connectionPanel.getJTextFieldConnectionPassword().setText(info.getPassword());
      connectionPanel.getJTextFieldConnectionJar().setText(info.getJar());
      connectionPanel.getJTextFieldConnectionDriver().setText(info.getDriver());
      connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.JDBC);
    } else {
      connectionPanel.getJTextFieldHttpName().setText(info.getName());
      connectionPanel.getJTextFieldHttpURL().setText(info.getUrl());
      if (info.getHttpMethod() != null) {
        connectionPanel.getMethodRadioButtonPanel().setSelectedRadioButton(info.getHttpMethod());
      }
      if (info.getParseType() != null) {
        connectionPanel.getParseRadioButtonPanel().setSelectedRadioButton(info.getParseType());
      }
      connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.HTTP);
    }
  }

  private boolean hasNameForOpenedTab() {
    if (openedTab.equals(ConnectionTypeTabPane.JDBC)) {
      return !connectionPanel.getJTextFieldConnectionName().getText().trim().isEmpty();
    }
    return !connectionPanel.getJTextFieldHttpName().getText().trim().isEmpty();
  }

  private int nextConnectionId() {
    AtomicInteger next = new AtomicInteger(0);
    profileManager.getConnectionInfoList().stream()
        .max(Comparator.comparing(ConnectionInfo::getId))
        .ifPresent(c -> next.set(c.getId()));
    return next.incrementAndGet();
  }

  private int nextQueryId() {
    AtomicInteger next = new AtomicInteger(0);
    profileManager.getQueryInfoList().stream()
        .max(Comparator.comparing(QueryInfo::getId))
        .ifPresent(q -> next.set(q.getId()));
    return next.incrementAndGet();
  }

  private void checkConnectionNameIsBusy(int id, String newConnectionName) {
    for (ConnectionInfo c : profileManager.getConnectionInfoList()) {
      if (Objects.equals(c.getName(), newConnectionName) && c.getId() != id) {
        throw new NotFoundException("Name " + newConnectionName + " already exists, please enter another one.");
      }
    }
  }

  private boolean isNotUsedOnTask(int connectionId) {
    return profileManager.getTaskInfoList().stream().noneMatch(t -> t.getConnectionId() == connectionId);
  }

  private void refillConnectionTableAndSelectFirst() {
    refillConnectionTableAndSelectId(null);
  }

  private void refillConnectionTableAndSelectId(Integer selectId) {
    TTTable<ConnectionRow, JXTable> tt = connectionCase.getTypedTable();
    List<ConnectionRow> rows = profileManager.getConnectionInfoList().stream()
        .map(c -> {
          ConnectionInfo info = profileManager.getConnectionInfoById(c.getId());
          ConnectionType type = c.getType() != null ? c.getType() : ConnectionType.JDBC;
          return new ConnectionRow(c.getId(), c.getName(), type, info != null ? info.getDbType() : null);
        })
        .collect(Collectors.toList());
    tt.setItems(rows);

    if (rows.isEmpty()) {
      context.setSelectedConnectionId(null);
      return;
    }

    int idx = 0;
    if (selectId != null) {
      for (int i = 0; i < rows.size(); i++) {
        if (rows.get(i).getId() == selectId) {
          idx = i;
          break;
        }
      }
    }
    connectionCase.getJxTable().setRowSelectionInterval(idx, idx);
  }

  private void setEditModeForTab(ConnectionTypeTabPane tab, boolean edit) {
    connectionButtonPanel.setButtonView(!edit);

    boolean enableTables = !edit;
    profileCase.getJxTable().setEnabled(enableTables);
    taskCase.getJxTable().setEnabled(enableTables);
    connectionCase.getJxTable().setEnabled(enableTables);
    queryCase.getJxTable().setEnabled(enableTables);
    connectionTemplateCase.getJxTable().setEnabled(enableTables);

    checkboxConfig.setEnabled(enableTables);

    configTab.setEnabledAt(0, enableTables);
    configTab.setEnabledAt(1, enableTables);
    configTab.setEnabledAt(2, true);
    configTab.setEnabledAt(3, enableTables);

    if (tab.equals(ConnectionTypeTabPane.JDBC)) {
      connectionPanel.getConnTypeTab().setSelectedTab(ConnectionTypeTabPane.JDBC);
      connectionPanel.getConnTypeTab().setEnabledTab(ConnectionTypeTabPane.HTTP, !edit);

      connectionPanel.getJTextFieldConnectionName().setEditable(edit);
      connectionPanel.getJTextFieldConnectionUserName().setEditable(edit);
      connectionPanel.getJTextFieldConnectionPassword().setEditable(edit);
      connectionPanel.getJTextFieldConnectionDriver().setEditable(edit);
      connectionPanel.getJTextFieldConnectionJar().setEditable(edit);
      connectionPanel.getJTextFieldConnectionURL().setEditable(edit);
      connectionPanel.getJarButton().setEnabled(edit);

      connectionPanel.getBtnLoadHttp().setEnabled(false);
      connectionPanel.getMethodRadioButtonPanel().setButtonNotView();
      connectionPanel.getParseRadioButtonPanel().setButtonNotView();
    } else {
      connectionPanel.getConnTypeTab().setSelectedTab(ConnectionTypeTabPane.HTTP);
      connectionPanel.getConnTypeTab().setEnabledTab(ConnectionTypeTabPane.JDBC, !edit);

      connectionPanel.getJTextFieldHttpName().setEditable(edit);
      connectionPanel.getJTextFieldHttpURL().setEditable(edit);
      connectionPanel.getBtnLoadHttp().setEnabled(edit);

      if (edit) {
        connectionPanel.getMethodRadioButtonPanel().setButtonView();
        connectionPanel.getParseRadioButtonPanel().setButtonView();
      } else {
        connectionPanel.getMethodRadioButtonPanel().setButtonNotView();
        connectionPanel.getParseRadioButtonPanel().setButtonNotView();
      }

      connectionPanel.getJarButton().setEnabled(false);
    }
  }

  private void clearForm(ConnectionTypeTabPane tab) {
    if (tab.equals(ConnectionTypeTabPane.JDBC)) {
      connectionPanel.getJTextFieldConnectionName().setText("");
      connectionPanel.getJTextFieldConnectionName().setPrompt(bundleDefault.getString("cName"));
      connectionPanel.getJTextFieldConnectionUserName().setText("");
      connectionPanel.getJTextFieldConnectionUserName().setPrompt(bundleDefault.getString("cUserName"));
      connectionPanel.getJTextFieldConnectionURL().setText("");
      connectionPanel.getJTextFieldConnectionURL().setPrompt(bundleDefault.getString("cURL"));
      connectionPanel.getJTextFieldConnectionPassword().setText("");
      connectionPanel.getJTextFieldConnectionDriver().setText("");
      connectionPanel.getJTextFieldConnectionDriver().setPrompt(bundleDefault.getString("cDriver"));
      connectionPanel.getJTextFieldConnectionJar().setText("");
      connectionPanel.getJTextFieldConnectionJar().setPrompt(bundleDefault.getString("cJar"));
    } else {
      connectionPanel.getJTextFieldHttpName().setText("");
      connectionPanel.getJTextFieldHttpName().setPrompt(bundleDefault.getString("cName"));
      connectionPanel.getJTextFieldHttpURL().setText("");
      connectionPanel.getJTextFieldHttpURL().setPrompt(bundleDefault.getString("cURL"));
    }
  }

  private void tryDecryptPasswordOrForceReset(int connectionId) {
    String enc = String.valueOf(connectionPanel.getJTextFieldConnectionPassword().getPassword());
    if (enc == null || enc.isEmpty()) {
      return;
    }
    try {
      encryptDecrypt.decrypt(enc);
    } catch (Exception ex) {
      isPasswordChanged = true;
      connectionPanel.getJTextFieldConnectionPassword().setText("");

      ConnectionInfo file = profileManager.getConnectionInfoById(connectionId);
      if (file != null) {
        file.setPassword("");
        profileManager.updateConnection(file);
        profileManager.updateCache();
      }

      JOptionPane.showMessageDialog(connectionPanel,
                                    "Need to set new password. Perhaps the configuration was copied from another computer",
                                    "Password decryption issue", JOptionPane.WARNING_MESSAGE);
    }
  }

  private void deleteHttpRelatedQuery(String connectionName) {
    QueryInfo qi = profileManager.getQueryInfoList().stream()
        .filter(q -> Objects.equals(q.getName(), connectionName))
        .findFirst()
        .orElse(null);
    if (qi == null) {
      return;
    }
    profileManager.deleteQuery(qi.getId(), qi.getName());
    profileManager.deleteTable(connectionName);
    updateQueryCaseScrollToBottom();
  }

  private void updateHttpRelatedQuery(String oldName, String newName) {
    QueryInfo qi = profileManager.getQueryInfoList().stream()
        .filter(q -> Objects.equals(q.getName(), oldName))
        .findFirst()
        .orElse(null);
    if (qi == null) {
      return;
    }

    int id = qi.getId();
    profileManager.deleteQuery(id, oldName);
    profileManager.deleteTable(oldName);

    QueryInfo newQi = new QueryInfo();
    newQi.setId(id);
    newQi.setName(newName);

    TableInfo tableInfo = new TableInfo();
    tableInfo.setTableName(newName);

    profileManager.addTable(tableInfo);
    profileManager.addQuery(newQi);
    updateQueryCaseScrollToBottom();
  }

  private void updateQueryCaseScrollToBottom() {
    queryCase.clearTable();

    TTTable<QueryRow, JXTable> tt = queryCase.getTypedTable();
    List<QueryRow> rows = profileManager.getQueryInfoList().stream()
        .map(q -> new QueryRow(q.getId(), q.getName()))
        .collect(Collectors.toList());
    tt.setItems(rows);

    int rowCount = tt.model().getRowCount();
    if (rowCount > 0) {
      queryCase.getJxTable().setRowSelectionInterval(rowCount - 1, rowCount - 1);
      JScrollBar verticalScrollBar = queryCase.getJScrollPane().getVerticalScrollBar();
      verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }
  }
}