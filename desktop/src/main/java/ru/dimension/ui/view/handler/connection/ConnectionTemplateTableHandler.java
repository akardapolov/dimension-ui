package ru.dimension.ui.view.handler.connection;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.view.panel.config.connection.ConnectionPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.ConnectionTemplateRow;

@Log4j2
@Singleton
public final class ConnectionTemplateTableHandler implements ListSelectionListener {

  private final ConnectionPanel connectionPanel;
  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final JXTableCase connectionTemplateCase;
  private final TemplateManager templateManager;
  private final ProfileManager profileManager;
  private final ConfigTab configTab;
  private final JCheckBox checkboxConfig;

  private int templateConnectionId;

  @Inject
  public ConnectionTemplateTableHandler(@Named("connectionConfigPanel") ConnectionPanel connectionPanel,
                                        @Named("profileConfigCase") JXTableCase profileCase,
                                        @Named("taskConfigCase") JXTableCase taskCase,
                                        @Named("connectionConfigCase") JXTableCase connectionCase,
                                        @Named("queryConfigCase") JXTableCase queryCase,
                                        @Named("connectionTemplateCase") JXTableCase connectionTemplateCase,
                                        @Named("templateManager") TemplateManager templateManager,
                                        @Named("profileManager") ProfileManager profileManager,
                                        @Named("configTab") ConfigTab configTab,
                                        @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.connectionPanel = connectionPanel;
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.connectionTemplateCase = connectionTemplateCase;
    this.templateManager = templateManager;
    this.profileManager = profileManager;
    this.configTab = configTab;
    this.checkboxConfig = checkboxConfig;
    this.templateConnectionId = -1;

    this.connectionTemplateCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    this.connectionPanel.getJButtonTemplate().addActionListener(e -> copyFromTemplate());

    fillTemplateTable();
    this.connectionPanel.getJButtonTemplate().setEnabled(false);
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) {
      return;
    }

    ListSelectionModel sm = (ListSelectionModel) e.getSource();
    if (sm.isSelectionEmpty()) {
      connectionPanel.getJButtonTemplate().setEnabled(false);
      return;
    }

    templateConnectionId = getSelectedTemplateId();
    Connection select = templateManager.getConfigList(Connection.class).stream()
        .filter(c -> c.getId() == templateConnectionId)
        .findFirst()
        .orElse(null);

    if (select == null) {
      connectionPanel.getJButtonTemplate().setEnabled(false);
      return;
    }

    enterTemplatePreviewMode();

    if (ConnectionType.HTTP.equals(select.getType())) {
      connectionPanel.getJTextFieldHttpName().setText(select.getName());
      connectionPanel.getJTextFieldHttpURL().setText(select.getUrl());
      if (select.getHttpMethod() != null) {
        connectionPanel.getMethodRadioButtonPanel().setSelectedRadioButton(select.getHttpMethod());
      }
      if (select.getParseType() != null) {
        connectionPanel.getParseRadioButtonPanel().setSelectedRadioButton(select.getParseType());
      }
      connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.HTTP);
    } else {
      connectionPanel.getJTextFieldConnectionName().setText(select.getName());
      connectionPanel.getJTextFieldConnectionURL().setText(select.getUrl());
      connectionPanel.getJTextFieldConnectionUserName().setText(select.getUserName());
      connectionPanel.getJTextFieldConnectionPassword().setText(select.getPassword());
      connectionPanel.getJTextFieldConnectionJar().setText(select.getJar());
      connectionPanel.getJTextFieldConnectionDriver().setText(select.getDriver());
      connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.JDBC);
    }

    connectionPanel.getJButtonTemplate().setEnabled(true);
  }

  private void fillTemplateTable() {
    TTTable<ConnectionTemplateRow, JXTable> tt = connectionTemplateCase.getTypedTable();
    List<ConnectionTemplateRow> rows = Rows.mapList(
        templateManager.getConfigList(Connection.class)
    );
    tt.setItems(rows);
  }

  private int getSelectedTemplateId() {
    TTTable<ConnectionTemplateRow, JXTable> tt = connectionTemplateCase.getTypedTable();
    JXTable table = tt.table();
    int viewRow = table.getSelectedRow();
    if (viewRow < 0) {
      return -1;
    }
    int modelRow = table.convertRowIndexToModel(viewRow);
    ConnectionTemplateRow row = tt.model().itemAt(modelRow);
    return row != null ? row.getId() : -1;
  }

  private void enterTemplatePreviewMode() {
    connectionPanel.getJTextFieldConnectionName().setEditable(true);
    connectionPanel.getJTextFieldHttpName().setEditable(true);

    connectionPanel.getJTextFieldConnectionUserName().setEditable(false);
    connectionPanel.getJTextFieldConnectionPassword().setEditable(false);
    connectionPanel.getJTextFieldConnectionDriver().setEditable(false);
    connectionPanel.getJTextFieldConnectionJar().setEditable(false);
    connectionPanel.getJTextFieldConnectionURL().setEditable(false);
    connectionPanel.getJTextFieldHttpURL().setEditable(false);

    connectionPanel.getConnectionButtonPanel().getBtnNew().setEnabled(false);
    connectionPanel.getConnectionButtonPanel().getBtnCopy().setEnabled(false);
    connectionPanel.getConnectionButtonPanel().getBtnDel().setEnabled(false);
    connectionPanel.getConnectionButtonPanel().getBtnEdit().setEnabled(false);
    connectionPanel.getConnectionButtonPanel().getBtnSave().setEnabled(false);
    connectionPanel.getConnectionButtonPanel().getBtnCancel().setEnabled(true);

    checkboxConfig.setEnabled(false);

    configTab.setEnabledAt(0, false);
    configTab.setEnabledAt(1, false);
    configTab.setEnabledAt(3, false);

    profileCase.getJxTable().setEnabled(false);
    taskCase.getJxTable().setEnabled(false);
    connectionCase.getJxTable().setEnabled(false);
    queryCase.getJxTable().setEnabled(false);
  }

  private void exitTemplatePreviewMode() {
    connectionPanel.getJButtonTemplate().setEnabled(false);
    checkboxConfig.setEnabled(true);

    configTab.setEnabledAt(0, true);
    configTab.setEnabledAt(1, true);
    configTab.setEnabledAt(3, true);

    profileCase.getJxTable().setEnabled(true);
    taskCase.getJxTable().setEnabled(true);
    connectionCase.getJxTable().setEnabled(true);
    queryCase.getJxTable().setEnabled(true);
  }

  private void copyFromTemplate() {
    if (templateConnectionId < 0) {
      return;
    }

    Connection select = templateManager.getConfigList(Connection.class).stream()
        .filter(s -> s.getId() == templateConnectionId)
        .findFirst()
        .orElse(null);

    if (select == null) {
      return;
    }

    String newName;
    if (ConnectionType.HTTP.equals(select.getType())) {
      newName = connectionPanel.getJTextFieldHttpName().getText();
    } else {
      newName = connectionPanel.getJTextFieldConnectionName().getText();
    }

    if (newName == null || newName.trim().isEmpty()) {
      JOptionPane.showMessageDialog(null,
                                    "Name cannot be empty.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (!isNameFree(newName)) {
      JOptionPane.showMessageDialog(null,
                                    "Name " + newName + " already exists, please enter another one.",
                                    "Information", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    int newId = nextConnectionId();

    ConnectionInfo copy = new ConnectionInfo();
    copy.setId(newId);
    copy.setName(newName); // Use the validated name from UI
    copy.setUserName(select.getUserName());
    copy.setUrl(select.getUrl());
    copy.setJar(select.getJar());
    copy.setDriver(select.getDriver());
    copy.setPassword(select.getPassword());
    copy.setType(select.getType() != null ? select.getType() : ConnectionType.JDBC);

    if (ConnectionType.HTTP.equals(copy.getType())) {
      copy.setHttpMethod(select.getHttpMethod());
      copy.setParseType(select.getParseType());
    }

    profileManager.addConnection(copy);

    TTTable<ConnectionRow, JXTable> tt = connectionCase.getTypedTable();
    List<ConnectionRow> rows = profileManager.getConnectionInfoList().stream()
        .map(c -> {
          ConnectionInfo info = profileManager.getConnectionInfoById(c.getId());
          ConnectionType type = c.getType() != null ? c.getType() : ConnectionType.JDBC;
          return new ConnectionRow(c.getId(), c.getName(), type, info != null ? info.getDbType() : null);
        })
        .collect(Collectors.toList());
    tt.setItems(rows);

    int selection = 0;
    for (int i = 0; i < rows.size(); i++) {
      if (rows.get(i).getId() == copy.getId()) {
        selection = i;
        break;
      }
    }

    connectionCase.getJxTable().setRowSelectionInterval(selection, selection);
    connectionTemplateCase.getJxTable().clearSelection();

    exitTemplatePreviewMode();
  }

  private boolean isNameFree(String name) {
    return profileManager.getConnectionInfoList().stream().noneMatch(c -> Objects.equals(c.getName(), name));
  }

  private int nextConnectionId() {
    AtomicInteger next = new AtomicInteger(0);
    profileManager.getConnectionInfoList().stream()
        .max(Comparator.comparing(ConnectionInfo::getId))
        .ifPresent(c -> next.set(c.getId()));
    return next.incrementAndGet();
  }
}