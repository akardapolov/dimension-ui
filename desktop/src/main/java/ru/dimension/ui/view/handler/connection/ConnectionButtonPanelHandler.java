package ru.dimension.ui.view.handler.connection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import lombok.extern.log4j.Log4j2;
import org.apache.hc.core5.http.Method;
import ru.dimension.ui.exception.EmptyNameException;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.handler.LifeCycleStatus;
import ru.dimension.ui.model.view.tab.ConfigEditTabPane;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.security.EncryptDecrypt;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.connection.ConnectionPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.parse.ParseType;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.router.event.EventListener;

@Log4j2
@Singleton
public class ConnectionButtonPanelHandler implements ActionListener, ChangeListener {

  private final ProfileManager profileManager;
  private final EventListener eventListener;
  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final JXTableCase connectionTemplateCase;
  private final ConnectionPanel connectionPanel;
  private final ButtonPanel connectionButtonPanel;
  private final ConfigTab configTab;
  private final JCheckBox checkboxConfig;
  private final EncryptDecrypt encryptDecrypt;
  private LifeCycleStatus status;
  private boolean isPasswordChanged = false;
  private ConnectionInfo oldFileConnection;

  private final JFileChooser jarFC;
  private JFrame jFrame;
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private final ResourceBundle bundleDefault;
  private ConnectionTypeTabPane openedTab;

  @Inject
  public ConnectionButtonPanelHandler(@Named("profileManager") ProfileManager profileManager,
                                      @Named("eventListener") EventListener eventListener,
                                      @Named("encryptDecrypt") EncryptDecrypt encryptDecrypt,
                                      @Named("profileConfigCase") JXTableCase profileCase,
                                      @Named("taskConfigCase") JXTableCase taskCase,
                                      @Named("connectionConfigCase") JXTableCase connectionCase,
                                      @Named("connectionTemplateCase") JXTableCase connectionTemplateCase,
                                      @Named("queryConfigCase") JXTableCase queryCase,
                                      @Named("connectionConfigPanel") ConnectionPanel connectionPanel,
                                      @Named("connectionButtonPanel") ButtonPanel connectionButtonPanel,
                                      @Named("jTabbedPaneConfig") ConfigTab configTab,
                                      @Named("checkboxConfig") JCheckBox checkboxConfig) {

    this.profileManager = profileManager;
    this.encryptDecrypt = encryptDecrypt;

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.connectionTemplateCase = connectionTemplateCase;
    this.queryCase = queryCase;
    this.connectionPanel = connectionPanel;
    this.connectionButtonPanel = connectionButtonPanel;
    this.configTab = configTab;
    this.checkboxConfig = checkboxConfig;
    this.jarFC = new JFileChooser();

    this.openedTab = ConnectionTypeTabPane.JDBC;
    this.connectionPanel.getConnTypeTab().addChangeListener(this);

    this.connectionButtonPanel.getBtnNew().addActionListener(this);
    this.connectionButtonPanel.getBtnCopy().addActionListener(this);
    this.connectionButtonPanel.getBtnDel().addActionListener(this);
    this.connectionButtonPanel.getBtnEdit().addActionListener(this);
    this.connectionButtonPanel.getBtnSave().addActionListener(this);
    this.connectionButtonPanel.getBtnCancel().addActionListener(this);

    this.connectionPanel.getJarButton().addActionListener(e -> executor.submit(() -> {
      connectionPanel.getJarButton().setEnabled(false);
      connectionPanel.getJTextFieldConnectionJar().requestFocus();

      int returnVal = jarFC.showOpenDialog(jFrame);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        File file = jarFC.getSelectedFile();
        connectionPanel.getJTextFieldConnectionJar().setText(file.getAbsolutePath());
      }

      this.connectionPanel.getJarButton().setVisible(true);
      connectionPanel.getJarButton().setEnabled(true);
    }));

    this.connectionButtonPanel.getBtnDel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    this.connectionButtonPanel.getBtnDel().getActionMap().put("delete", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        connectionButtonPanel.getBtnDel().doClick();
      }
    });

    this.connectionButtonPanel.getBtnCancel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    this.connectionButtonPanel.getBtnCancel().getActionMap().put("cancel", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        connectionButtonPanel.getBtnCancel().doClick();
      }
    });

    this.status = LifeCycleStatus.NONE;

    this.connectionPanel.getJTextFieldConnectionPassword()
        .getDocument().addDocumentListener((PasswordDocumentListener) e -> {
          if (this.connectionPanel.getJTextFieldConnectionPassword().isEditable()) {
            isPasswordChanged = true;
          }
        });

    this.eventListener = eventListener;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String oldGuiPass = "";
    if (isPasswordChanged) {
      oldGuiPass = String.valueOf(connectionPanel.getJTextFieldConnectionPassword().getPassword());
    } else {

      try {
        oldGuiPass = encryptDecrypt
            .decrypt(String.valueOf(connectionPanel.getJTextFieldConnectionPassword().getPassword()));
      } catch (Exception exception) {
        isPasswordChanged = true;
        connectionPanel.getJTextFieldConnectionPassword().setText("");

        int connectionId = getSelectedConnectionId();
        oldFileConnection = profileManager.getConnectionInfoById(connectionId);
        oldFileConnection.setPassword("");
        profileManager.updateConnection(oldFileConnection);
        profileManager.updateCache();

        JOptionPane.showMessageDialog(connectionPanel,
                                      "Need to set new password. Perhaps the configuration was copied from another computer",
                                      "Password decryption issue", JOptionPane.WARNING_MESSAGE);
      }

    }

    if (e.getSource() == connectionButtonPanel.getBtnNew()) {
      status = LifeCycleStatus.NEW;

      newEmptyPanel(openedTab);
      setPanelView(false, ConnectionType.JDBC.getName());
      setPanelView(false, ConnectionType.HTTP.getName());

      connectionPanel.getConnTypeTab().setEnabledTab(ConnectionTypeTabPane.JDBC, true);
      connectionPanel.getConnTypeTab().setEnabledTab(ConnectionTypeTabPane.HTTP, true);

    } else if (e.getSource() == connectionButtonPanel.getBtnCopy()) {
      status = LifeCycleStatus.COPY;

      if (connectionCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("The connection to copy is not selected. Please select and try again!");
      } else {
        int connectionId = getSelectedConnectionId();
        ConnectionInfo connection = profileManager.getConnectionInfoById(connectionId);
        if (Objects.isNull(connection)) {
          throw new NotFoundException("Not found connection: " + connectionId);
        }
        String connectionType = connection.getType().getName();

        int input = 0;
        if (!connectionType.equals(openedTab.getName())) {

          input = JOptionPane.showOptionDialog(null,// 0=yes, 1=no
                                               "The " + connectionType + " connection is selected in the table, "
                                                   + "and you are on the " + openedTab.getName() + " tab."
                                                   + " Go to the " + connectionType + " tab and make a copy?",
                                               "Information", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                               null, new String[]{"Yes", "No"}, "Yes");

        }
        if (input == 0) {
          setPanelView(false, connectionType);
          if (openedTab.equals(ConnectionTypeTabPane.JDBC)) {
            connectionPanel.getJTextFieldConnectionName().setText(connection.getName() + "_copy");
            connectionPanel.getJTextFieldConnectionURL().setText(connection.getUrl());
            connectionPanel.getJTextFieldConnectionUserName().setText(connection.getUserName());
            connectionPanel.getJTextFieldConnectionPassword().setText("");
            connectionPanel.getJTextFieldConnectionJar().setText(connection.getJar());
            connectionPanel.getJTextFieldConnectionDriver().setText(connection.getDriver());
            connectionPanel.getConnTypeTab().setEnabledAt(1, false);
          } else if (openedTab.equals(ConnectionTypeTabPane.HTTP)) {
            connectionPanel.getJTextFieldHttpName().setText(connection.getName() + "_copy");
            connectionPanel.getJTextFieldHttpName().setPrompt(bundleDefault.getString("cName"));
            connectionPanel.getJTextFieldHttpURL().setText(connection.getUrl());
            connectionPanel.getJTextFieldHttpURL().setPrompt(bundleDefault.getString("cURL"));
            connectionPanel.getConnTypeTab().setEnabledAt(0, false);
          }
        }
      }

    } else if (e.getSource() == connectionButtonPanel.getBtnDel()) {
      if (connectionCase.getJxTable().getSelectedRow() == -1) {
        JOptionPane.showMessageDialog(null, "Not selected connection. Please select and try again!",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      } else {
        int connectionId = getSelectedConnectionId();
        int input = JOptionPane.showConfirmDialog(new JDialog(),// 0=yes, 1=no, 2=cancel
                                                  "Do you want to delete configuration: "
                                                      + connectionCase.getDefaultTableModel()
                                                      .getValueAt(connectionCase.getJxTable().getSelectedRow(), 1)
                                                      + "?");
        if (input == 0) {
          if (isUsedOnTask(connectionId)) {
            ConnectionInfo connection = profileManager.getConnectionInfoById(connectionId);
            if (Objects.isNull(connection)) {
              throw new NotFoundException("Not found task: " + connectionId);
            }
            profileManager.deleteConnection(connection.getId(), connection.getName());

            clearConnectionCase();

            profileManager.getConnectionInfoList().forEach(connectionInfo -> {
                                                             if (connectionInfo.getType() != null) {
                                                               connectionCase.getDefaultTableModel()
                                                                   .addRow(new Object[]{connectionInfo.getId(), connectionInfo.getName(), connectionInfo.getType()});
                                                             } else {
                                                               connectionCase.getDefaultTableModel()
                                                                   .addRow(new Object[]{connectionInfo.getId(), connectionInfo.getName(), ConnectionType.JDBC});
                                                             }
                                                           }
            );

            if (connectionCase.getJxTable().getSelectedRow() > 0) {
              connectionCase.getJxTable().setRowSelectionInterval(0, 0);
            }

            if (openedTab.equals(ConnectionTypeTabPane.HTTP)) {
              for (int rowQuery = 0; rowQuery < queryCase.getJxTable().getRowCount(); rowQuery++) {
                String nameQuery = queryCase.getDefaultTableModel().getValueAt(rowQuery, 1).toString();
                if (nameQuery.equals(connection.getName())) {
                  int idQuery = (Integer) queryCase.getDefaultTableModel().getValueAt(rowQuery, 0);
                  deleteQueryById(idQuery);
                  profileManager.deleteTable(nameQuery);
                  updateQueryCase();
                }
              }
            }
          } else {
            JOptionPane.showMessageDialog(null, "Cannot delete this connection it is used in the task",
                                          "General Error", JOptionPane.ERROR_MESSAGE);
          }
        }
      }
    } else if (e.getSource() == connectionButtonPanel.getBtnEdit()) {
      if (connectionCase.getJxTable().getSelectedRow() == -1) {
        JOptionPane.showMessageDialog(null, "Not selected connection. Please select and try again!",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      } else {
        status = LifeCycleStatus.EDIT;
        int connectionId = getSelectedConnectionId();
        oldFileConnection = profileManager.getConnectionInfoById(connectionId);
        if (Objects.isNull(oldFileConnection)) {
          throw new NotFoundException("Not found task: " + connectionId);
        }

        String connectionType =
            oldFileConnection.getType() != null ? oldFileConnection.getType().getName() : ConnectionType.JDBC.getName();

        int input = 0;
        if (!connectionType.equals(openedTab.getName())) {

          input = JOptionPane.showOptionDialog(null,// 0=yes, 1=no
                                               "The " + connectionType + " connection is selected in the table, "
                                                   + "and you are on the " + openedTab.getName() + " tab."
                                                   + " Go to the " + connectionType + " tab and edit the connection?",
                                               "Information", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                               null, new String[]{"Yes", "No"}, "Yes");

        }
        if (input == 0) {
          setPanelView(false, connectionType);
        }
      }
    } else if (e.getSource() == connectionButtonPanel.getBtnSave()) {

      if (LifeCycleStatus.NEW.equals(status) || LifeCycleStatus.COPY.equals(status)) {

        AtomicInteger connectionIdNext = new AtomicInteger();

        profileManager.getConnectionInfoList().stream()
            .max(Comparator.comparing(ConnectionInfo::getId))
            .ifPresentOrElse(connection -> connectionIdNext.set(connection.getId()),
                             () -> {
                               log.info("Not found Connection");
                               connectionIdNext.set(0);
                             });

        if ((!connectionPanel.getJTextFieldConnectionName().getText().trim().isEmpty()
            && openedTab.equals(ConnectionTypeTabPane.JDBC))
            || (!connectionPanel.getJTextFieldHttpName().getText().trim().isEmpty()
            && openedTab.equals(ConnectionTypeTabPane.HTTP))) {

          int connectionId = connectionIdNext.incrementAndGet();
          String newConnectionName;
          if (openedTab.equals(ConnectionTypeTabPane.JDBC)) {
            newConnectionName = connectionPanel.getJTextFieldConnectionName().getText();
          } else {
            newConnectionName = connectionPanel.getJTextFieldHttpName().getText();
          }
          checkConnectionNameIsBusy(connectionId, newConnectionName);

          ConnectionInfo saveConnection = new ConnectionInfo();

          saveConnection.setId(connectionIdNext.incrementAndGet());
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

            JRadioButton selectedMethod = GUIHelper.getSelectedButton(connectionPanel.getMethodRadioButtonPanel()
                                                                          .getButtonGroup());
            saveConnection.setHttpMethod(Method.normalizedValueOf(selectedMethod.getText()));

            JRadioButton selectedParse = GUIHelper.getSelectedButton(connectionPanel.getParseRadioButtonPanel()
                                                                         .getButtonGroup());
            ParseType parseType = ParseType.valueOf(selectedParse.getText().toUpperCase());
            saveConnection.setParseType(parseType);

            QueryInfo saveQueryinfo = new QueryInfo();
            saveQueryinfo.setId(getIdForQuery());
            saveQueryinfo.setName(connectionPanel.getJTextFieldHttpName().getText());

            TableInfo tableInfo = new TableInfo();
            tableInfo.setTableName(saveQueryinfo.getName());

            profileManager.addTable(tableInfo);
            profileManager.addQuery(saveQueryinfo);

            updateQueryCase();

            configTab.setSelectedTab(ConfigEditTabPane.CONNECTION);
          }

          isPasswordChanged = false;

          profileManager.addConnection(saveConnection);

          clearConnectionCase();

          int selection = 0;
          int index = 0;
          for (ConnectionInfo connection : profileManager.getConnectionInfoList()) {
            if (connection.getType() != null) {
              connectionCase.getDefaultTableModel()
                  .addRow(new Object[]{connection.getId(), connection.getName(), connection.getType()});
            } else {
              connectionCase.getDefaultTableModel()
                  .addRow(new Object[]{connection.getId(), connection.getName(), ConnectionType.JDBC});
            }

            if (connection.getId() == saveConnection.getId()) {
              index++;
              selection = index;
            }
            index++;
          }

          setPanelView(true, openedTab.getName());
          connectionCase.getJxTable().setRowSelectionInterval(selection - 1, selection - 1);

        } else {
          throw new EmptyNameException("The name field is empty");
        }

      } else if (LifeCycleStatus.EDIT.equals(status)) {

        int selectedIndex = connectionCase.getJxTable().getSelectedRow();
        int connectionId = getSelectedConnectionId();

        if ((!connectionPanel.getJTextFieldConnectionName().getText().trim().isEmpty()
            && openedTab.equals(ConnectionTypeTabPane.JDBC))
            || (!connectionPanel.getJTextFieldHttpName().getText().trim().isEmpty()
            && openedTab.equals(ConnectionTypeTabPane.HTTP))) {
          String newConnectionName;
          if (openedTab.equals(ConnectionTypeTabPane.JDBC)) {
            newConnectionName = connectionPanel.getJTextFieldConnectionName().getText();
          } else {
            newConnectionName = connectionPanel.getJTextFieldHttpName().getText();
          }
          checkConnectionNameIsBusy(connectionId, newConnectionName);
          ConnectionInfo oldConnection = profileManager.getConnectionInfoById(connectionId);

          ConnectionInfo editConnection = new ConnectionInfo();
          editConnection.setId(connectionId);
          editConnection.setType(ConnectionType.valueOf(openedTab.getName()));

          if (openedTab.equals(ConnectionTypeTabPane.JDBC)) {
            editConnection.setName(connectionPanel.getJTextFieldConnectionName().getText());
            editConnection.setUserName(connectionPanel.getJTextFieldConnectionUserName().getText());
            editConnection.setUrl(connectionPanel.getJTextFieldConnectionURL().getText());

            setEditPassword(oldGuiPass, editConnection, encryptDecrypt.decrypt(oldFileConnection.getPassword()));

            editConnection.setJar(connectionPanel.getJTextFieldConnectionJar().getText());
            editConnection.setDriver(connectionPanel.getJTextFieldConnectionDriver().getText());
          } else {
            editConnection.setName(connectionPanel.getJTextFieldHttpName().getText());
            editConnection.setUrl(connectionPanel.getJTextFieldHttpURL().getText());

            JRadioButton selectedMethod = GUIHelper.getSelectedButton(connectionPanel.getMethodRadioButtonPanel()
                                                                          .getButtonGroup());
            editConnection.setHttpMethod(Method.normalizedValueOf(selectedMethod.getText()));

            JRadioButton selectedParse = GUIHelper.getSelectedButton(connectionPanel.getParseRadioButtonPanel()
                                                                         .getButtonGroup());
            ParseType parseType = ParseType.valueOf(selectedParse.getText().toUpperCase());
            editConnection.setParseType(parseType);
          }

          if (!oldConnection.getName().equals(newConnectionName)) {
            deleteConnectionById(connectionId);
            profileManager.addConnection(editConnection);
            if (openedTab.equals(ConnectionTypeTabPane.HTTP)) {
              for (int rowQuery = 0; rowQuery < queryCase.getJxTable().getRowCount(); rowQuery++) {
                String nameQuery = queryCase.getDefaultTableModel().getValueAt(rowQuery, 1).toString();
                if (nameQuery.equals(oldConnection.getName())) {
                  int idQuery = (Integer) queryCase.getDefaultTableModel().getValueAt(rowQuery, 0);
                  deleteQueryById(idQuery);
                  profileManager.deleteTable(nameQuery);

                  QueryInfo queryinfo = new QueryInfo();
                  queryinfo.setId(idQuery);
                  queryinfo.setName(connectionPanel.getJTextFieldHttpName().getText());

                  TableInfo tableInfo = new TableInfo();
                  tableInfo.setTableName(queryinfo.getName());

                  profileManager.addTable(tableInfo);
                  profileManager.addQuery(queryinfo);
                  updateQueryCase();
                }
              }
            }
          } else {
            profileManager.updateConnection(editConnection);
          }

          clearConnectionCase();

          profileManager.getConnectionInfoList().forEach(connectionInfo -> {
            if (connectionInfo.getType() != null) {
              connectionCase.getDefaultTableModel()
                  .addRow(new Object[]{connectionInfo.getId(), connectionInfo.getName(), connectionInfo.getType()});
            } else {
              connectionCase.getDefaultTableModel()
                  .addRow(new Object[]{connectionInfo.getId(), connectionInfo.getName(), ConnectionType.JDBC});
            }
          });

          setPanelView(true, openedTab.getName());
          connectionCase.getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);

        } else {
          throw new EmptyNameException("The name field is empty");
        }
      }
    } else if (e.getSource() == connectionButtonPanel.getBtnCancel()) {

      if (!connectionPanel.getJButtonTemplate().isEnabled()) {
        if (connectionCase.getJxTable().getSelectedRowCount() > 0) {
          int selectedIndex = connectionCase.getJxTable().getSelectedRow();
          String connectionType = connectionCase.getDefaultTableModel()
              .getValueAt(connectionCase.getJxTable().getSelectedRow(), 2).toString();
          ConnectionTypeTabPane selectedOpenTab = openedTab;
          connectionCase.getJxTable().setRowSelectionInterval(0, 0);
          setPanelView(true, selectedOpenTab.getName());
          connectionCase.getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);

          int connectionId = getSelectedConnectionId();
          ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(connectionId);
          if (Objects.isNull(connectionInfo)) {
            throw new NotFoundException("Not found task: " + connectionId);
          }
          if (connectionType.equals(ConnectionType.JDBC.getName())) {
            connectionPanel.getJTextFieldConnectionName().setText(connectionInfo.getName());
            connectionPanel.getJTextFieldConnectionUserName().setText(connectionInfo.getUserName());
            connectionPanel.getJTextFieldConnectionURL().setText(connectionInfo.getUrl());
            connectionPanel.getJTextFieldConnectionPassword().setText(connectionInfo.getPassword());
            connectionPanel.getJTextFieldConnectionJar().setText(connectionInfo.getJar());
            connectionPanel.getJTextFieldConnectionDriver().setText(connectionInfo.getDriver());
          } else {
            connectionPanel.getJTextFieldHttpName().setText(connectionInfo.getName());
            connectionPanel.getJTextFieldHttpURL().setText(connectionInfo.getUrl());
          }

          connectionPanel.setSelectedTabFull(selectedOpenTab);
        } else {
          setPanelView(true, openedTab.getName());
          newEmptyPanel(openedTab);

        }
      } else {
        connectionPanel.getJButtonTemplate().setEnabled(false);
        connectionPanel.getConnectionTemplateCase().getJxTable().clearSelection();
        int selectedIndex = connectionCase.getJxTable().getSelectedRow();

        if (connectionCase.getJxTable().getSelectedRowCount() > 0) {
          connectionCase.getJxTable().clearSelection();
          setPanelView(true, openedTab.getName());
          connectionCase.getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);
        } else {
          setPanelView(true, openedTab.getName());
          newEmptyPanel(openedTab);
        }
      }
    }
  }

  private void updateQueryCase() {
    queryCase.getDefaultTableModel().getDataVector().removeAllElements();
    queryCase.getDefaultTableModel().fireTableDataChanged();

    profileManager.getQueryInfoList()
        .forEach(queryInfo -> queryCase.getDefaultTableModel()
            .addRow(new Object[]{queryInfo.getId(), queryInfo.getName()}));

    int rowCount = queryCase.getDefaultTableModel().getRowCount();

    queryCase.getJxTable().setRowSelectionInterval(rowCount - 1, rowCount - 1);

    final JScrollBar verticalScrollBar = queryCase.getJScrollPane().getVerticalScrollBar();
    verticalScrollBar.setValue(verticalScrollBar.getMaximum());
  }

  private int getIdForQuery() {
    AtomicInteger connectionIdNext = new AtomicInteger();

    profileManager.getQueryInfoList().stream()
        .max(Comparator.comparing(QueryInfo::getId))
        .ifPresentOrElse(query -> connectionIdNext.set(query.getId()),
                         () -> {
                           log.info("Not found Query");
                           connectionIdNext.set(0);
                         });
    return connectionIdNext.incrementAndGet();
  }

  private void setEditPassword(String oldGuiPass,
                               ConnectionInfo editConnection,
                               String oldFilePass) {
    if (isPasswordChanged) {
      if (!oldGuiPass.equals(oldFilePass)) {
        editConnection.setPassword(encryptDecrypt.encrypt(
            String.valueOf(connectionPanel.getJTextFieldConnectionPassword().getPassword())));
      } else {
        editConnection.setPassword(encryptDecrypt.encrypt(oldFilePass));
      }
    } else {
      editConnection.setPassword(
          String.valueOf(connectionPanel.getJTextFieldConnectionPassword().getPassword()));
    }

    isPasswordChanged = false;
  }

  public void checkConnectionNameIsBusy(int id,
                                        String newConnectionName) {
    List<ConnectionInfo> connectionList = profileManager.getConnectionInfoList();
    for (ConnectionInfo connection : connectionList) {
      if (connection.getName().equals(newConnectionName) && connection.getId() != id) {
        throw new NotFoundException("Name " + newConnectionName
                                        + " already exists, please enter another one.");
      }
    }
  }

  public void deleteConnectionById(int id) {
    ConnectionInfo connectionDel = profileManager.getConnectionInfoById(id);
    if (Objects.isNull(connectionDel)) {
      throw new NotFoundException("Not found connection by id: " + id);
    }
    profileManager.deleteConnection(connectionDel.getId(), connectionDel.getName());
  }

  public void deleteQueryById(int id) {
    QueryInfo queryDel = profileManager.getQueryInfoById(id);
    if (Objects.isNull(queryDel)) {
      throw new NotFoundException("Not found query by id: " + id);
    }
    profileManager.deleteQuery(queryDel.getId(), queryDel.getName());
  }

  private int getSelectedConnectionId() {
    return (Integer) connectionCase.getDefaultTableModel()
        .getValueAt(connectionCase.getJxTable().getSelectedRow(), 0);
  }


  private void clearConnectionCase() {
    connectionCase.getDefaultTableModel().getDataVector().removeAllElements();
    connectionCase.getDefaultTableModel().fireTableDataChanged();
  }

  private void setPanelView(Boolean isSelected,
                            String connectionTabbedPane) {
    connectionButtonPanel.setButtonView(isSelected);
    if (connectionTabbedPane.equals(ConnectionTypeTabPane.JDBC.getName())) {
      connectionPanel.getJTextFieldConnectionName().setEditable(!isSelected);
      connectionPanel.getJTextFieldConnectionUserName().setEditable(!isSelected);
      connectionPanel.getJTextFieldConnectionPassword().setEditable(!isSelected);
      connectionPanel.getJTextFieldConnectionDriver().setEditable(!isSelected);
      connectionPanel.getJTextFieldConnectionJar().setEditable(!isSelected);
      connectionPanel.getJTextFieldConnectionURL().setEditable(!isSelected);
      connectionPanel.getConnTypeTab().setEnabledAt(1, isSelected);
      connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.JDBC);
    } else {
      connectionPanel.getJTextFieldHttpName().setEditable(!isSelected);
      connectionPanel.getJTextFieldHttpURL().setEditable(!isSelected);
      connectionPanel.getConnTypeTab().setEnabledAt(0, isSelected);
      connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.HTTP);
      connectionPanel.getBtnLoadHttp().setEnabled(!isSelected);
      if (isSelected) {
        connectionPanel.getMethodRadioButtonPanel().setButtonNotView();
        connectionPanel.getParseRadioButtonPanel().setButtonNotView();
      } else {
        connectionPanel.getMethodRadioButtonPanel().setButtonView();
        connectionPanel.getParseRadioButtonPanel().setButtonView();
      }
    }
    configTab.setEnabledAt(1, isSelected);
    configTab.setEnabledAt(0, isSelected);
    configTab.setEnabledAt(3, isSelected);
    profileCase.getJxTable().setEnabled(isSelected);
    taskCase.getJxTable().setEnabled(isSelected);
    connectionCase.getJxTable().setEnabled(isSelected);
    queryCase.getJxTable().setEnabled(isSelected);
    connectionTemplateCase.getJxTable().setEnabled(isSelected);
    checkboxConfig.setEnabled(isSelected);
    connectionPanel.getJarButton().setEnabled(!isSelected);
  }

  private void newEmptyPanel(ConnectionTypeTabPane connectionTypeTabPane) {
    if (connectionTypeTabPane.equals(ConnectionTypeTabPane.JDBC)) {
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
    } else if (connectionTypeTabPane.equals(ConnectionTypeTabPane.HTTP)) {
      connectionPanel.getJTextFieldHttpName().setText("");
      connectionPanel.getJTextFieldHttpName().setPrompt(bundleDefault.getString("cName"));
      connectionPanel.getJTextFieldHttpURL().setText("");
      connectionPanel.getJTextFieldHttpURL().setPrompt(bundleDefault.getString("cURL"));
    }
  }

  private boolean isUsedOnTask(int connectionId) {
    return !profileManager.getTaskInfoList()
        .stream()
        .anyMatch(task -> task.getConnectionId() == connectionId);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (connectionPanel.getConnTypeTab().getSelectedIndex() == 0) {
      openedTab = ConnectionTypeTabPane.JDBC;
    } else if (connectionPanel.getConnTypeTab().getSelectedIndex() == 1) {
      openedTab = ConnectionTypeTabPane.HTTP;
    }
  }
}
