package ru.dimension.ui.view.handler.connection;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.column.ConnectionColumnNames;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.view.panel.config.connection.ConnectionPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;

@Log4j2
@Singleton
public class ConnectionTemplateTableHandler implements ListSelectionListener, ActionListener {

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final ConfigTab configTab;
  private final JXTableCase connectionTemplateCase;
  private final ConnectionPanel connectionPanel;
  private final JCheckBox checkboxConfig;
  private final TemplateManager templateManager;
  private final ProfileManager profileManager;
  private int connectionID;

  @Inject
  public ConnectionTemplateTableHandler(@Named("connectionConfigPanel") ConnectionPanel connectionPanel,
                                        @Named("profileConfigCase") JXTableCase profileCase,
                                        @Named("taskConfigCase") JXTableCase taskCase,
                                        @Named("connectionConfigCase") JXTableCase connectionCase,
                                        @Named("queryConfigCase") JXTableCase queryCase,
                                        @Named("connectionTemplateCase") JXTableCase connectionTemplateCase,
                                        @Named("templateManager") TemplateManager templateManager,
                                        @Named("profileManager") ProfileManager profileManager,
                                        @Named("jTabbedPaneConfig") ConfigTab configTab,
                                        @Named("checkboxConfig") JCheckBox checkboxConfig) {

    this.connectionPanel = connectionPanel;

    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.connectionTemplateCase = connectionTemplateCase;
    this.configTab = configTab;
    this.connectionID = 0;
    this.checkboxConfig = checkboxConfig;

    this.templateManager = templateManager;
    this.profileManager = profileManager;

    this.connectionTemplateCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    this.connectionPanel.getJButtonTemplate().addActionListener(this);
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();
    if (!e.getValueIsAdjusting()) {

      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing profile fields");
        connectionPanel.getJButtonTemplate().setEnabled(false);
      } else {
        connectionPanel.getJTextFieldConnectionName().setEditable(false);
        connectionPanel.getJTextFieldConnectionUserName().setEditable(false);
        connectionPanel.getJTextFieldConnectionPassword().setEditable(false);
        connectionPanel.getJTextFieldConnectionDriver().setEditable(false);
        connectionPanel.getJTextFieldConnectionJar().setEditable(false);
        connectionPanel.getJTextFieldConnectionURL().setEditable(false);
        connectionPanel.getJButtonTemplate().setEnabled(true);
        connectionPanel.getConnectionButtonPanel().getBtnNew().setEnabled(false);
        connectionPanel.getConnectionButtonPanel().getBtnCopy().setEnabled(false);
        connectionPanel.getConnectionButtonPanel().getBtnDel().setEnabled(false);
        connectionPanel.getConnectionButtonPanel().getBtnEdit().setEnabled(false);
        connectionPanel.getConnectionButtonPanel().getBtnCancel().setEnabled(true);
        checkboxConfig.setEnabled(false);
        configTab.setEnabledAt(1, false);
        configTab.setEnabledAt(0, false);
        configTab.setEnabledAt(3, false);
        profileCase.getJxTable().setEnabled(false);
        taskCase.getJxTable().setEnabled(false);
        connectionCase.getJxTable().setEnabled(false);
        queryCase.getJxTable().setEnabled(false);
        this.connectionID = GUIHelper.getIdByColumnName(connectionTemplateCase.getJxTable(),
                                                        connectionTemplateCase.getDefaultTableModel(), listSelectionModel, ConnectionColumnNames.ID.getColName());
        Connection selectConnection = templateManager.getConfigList(Connection.class).stream()
            .filter(s -> s.getId() == connectionID).findFirst().orElse(null);

        if (ConnectionType.HTTP.equals(selectConnection.getType())) {
          connectionPanel.getJTextFieldHttpName().setText(selectConnection.getName());
          connectionPanel.getJTextFieldConnectionURL().setText(selectConnection.getUrl());
          connectionPanel.getMethodRadioButtonPanel().setSelectedRadioButton(selectConnection.getHttpMethod());
          connectionPanel.getParseRadioButtonPanel().setSelectedRadioButton(selectConnection.getParseType());
          connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.HTTP);
          return;
        }

        connectionPanel.getJTextFieldConnectionName().setText(selectConnection.getName());
        connectionPanel.getJTextFieldConnectionURL().setText(selectConnection.getUrl());
        connectionPanel.getJTextFieldConnectionUserName().setText(selectConnection.getUserName());
        connectionPanel.getJTextFieldConnectionPassword().setText(selectConnection.getPassword());
        connectionPanel.getJTextFieldConnectionJar().setText(selectConnection.getJar());
        connectionPanel.getJTextFieldConnectionDriver().setText(selectConnection.getDriver());
        connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.JDBC);
      }
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == connectionPanel.getJButtonTemplate()) {

      AtomicInteger connectionIdNext = new AtomicInteger();

      profileManager.getConnectionInfoList().stream()
          .max(Comparator.comparing(ConnectionInfo::getId))
          .ifPresentOrElse(connection -> connectionIdNext.set(connection.getId()),
                           () -> {
                             log.info("Not found Connection");
                             connectionIdNext.set(0);
                           });

      ConnectionInfo copyConnection = new ConnectionInfo();

      Connection selectConnection = templateManager.getConfigList(Connection.class).stream()
          .filter(s -> s.getId() == connectionID)
          .findFirst()
          .orElse(null);

      if (isBusyName(selectConnection.getName())) {

        copyConnection.setId(connectionIdNext.incrementAndGet());
        copyConnection.setName(selectConnection.getName());
        copyConnection.setUserName(selectConnection.getUserName());
        copyConnection.setUrl(selectConnection.getUrl());
        copyConnection.setJar(selectConnection.getJar());
        copyConnection.setDriver(selectConnection.getDriver());
        copyConnection.setPassword(selectConnection.getPassword());
        copyConnection.setType(selectConnection.getType());

        if (ConnectionType.HTTP.equals(selectConnection.getType())) {
          copyConnection.setHttpMethod(selectConnection.getHttpMethod());
          copyConnection.setParseType(selectConnection.getParseType());
        }

        profileManager.addConnection(copyConnection);

        connectionCase.getDefaultTableModel().getDataVector().removeAllElements();
        connectionCase.getDefaultTableModel().fireTableDataChanged();

        int selection = 0;
        int index = 0;
        for (ConnectionInfo connection : profileManager.getConnectionInfoList()) {
          connectionCase.getDefaultTableModel()
              .addRow(new Object[]{connection.getId(),
                  connection.getName(),
                  connection.getType() != null ? connection.getType() : ConnectionTypeTabPane.JDBC});

          if (connection.getId() == copyConnection.getId()) {
            index++;
            selection = index;
          }
          index++;
        }

        connectionCase.getJxTable().setRowSelectionInterval(selection - 1, selection - 1);
        connectionTemplateCase.getJxTable().clearSelection();
        connectionPanel.getJButtonTemplate().setEnabled(false);
        connectionPanel.getConnectionButtonPanel().setButtonView(true);
        checkboxConfig.setEnabled(true);
        configTab.setEnabledAt(1, true);
        configTab.setEnabledAt(0, true);
        configTab.setEnabledAt(3, true);
        profileCase.getJxTable().setEnabled(true);
        taskCase.getJxTable().setEnabled(true);
        connectionCase.getJxTable().setEnabled(true);
        queryCase.getJxTable().setEnabled(true);
      }
    }
  }


  public boolean isBusyName(String newName) {
    boolean isBusy = true;
    List<ConnectionInfo> connectionList = profileManager.getConnectionInfoList();
    for (ConnectionInfo connection : connectionList) {
      if ((connection.getName().equals(newName))) {
        isBusy = false;
        JOptionPane.showMessageDialog(null, "Name " + newName
                                          + " already exists, please enter another one.",
                                      "Information", JOptionPane.INFORMATION_MESSAGE);
        connectionPanel.getJButtonTemplate().setEnabled(false);
        connectionPanel.getConnectionTemplateCase().getJxTable().clearSelection();
        connectionPanel.getConnectionButtonPanel().setButtonView(true);
        checkboxConfig.setEnabled(true);
        configTab.setEnabledAt(1, true);
        configTab.setEnabledAt(0, true);
        configTab.setEnabledAt(3, true);
        profileCase.getJxTable().setEnabled(true);
        taskCase.getJxTable().setEnabled(true);
        connectionCase.getJxTable().setEnabled(true);
        queryCase.getJxTable().setEnabled(true);
        break;
      }
    }
    return isBusy;
  }
}
