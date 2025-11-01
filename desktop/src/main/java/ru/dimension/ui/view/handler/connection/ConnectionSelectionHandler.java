package ru.dimension.ui.view.handler.connection;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ResourceBundle;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.column.TaskColumnNames;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.tab.ConfigEditTabPane;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.view.handler.MouseListenerImpl;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.connection.ConnectionPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.prompt.Internationalization;

@Log4j2
@Singleton
public class ConnectionSelectionHandler extends MouseListenerImpl implements ListSelectionListener, ItemListener {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;

  private final JXTableCase connectionCase;
  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase queryCase;

  private final ConfigTab configTab;
  private final ConnectionPanel connectionPanel;
  private final ButtonPanel connectionButtonPanel;
  private final JXTableCase connectionTemplateCase;
  private final JCheckBox checkboxConfig;
  private Boolean isSelected;
  private final ResourceBundle bundleDefault;

  @Inject
  public ConnectionSelectionHandler(@Named("profileManager") ProfileManager profileManager,
                                    @Named("templateManager") TemplateManager templateManager,
                                    @Named("connectionConfigCase") JXTableCase connectionCase,
                                    @Named("taskConfigCase") JXTableCase taskCase,
                                    @Named("queryConfigCase") JXTableCase queryCase,
                                    @Named("profileConfigCase") JXTableCase profileCase,
                                    @Named("connectionConfigPanel") ConnectionPanel connectionPanel,
                                    @Named("connectionButtonPanel") ButtonPanel connectionButtonPanel,
                                    @Named("connectionTemplateCase") JXTableCase connectionTemplateCase,
                                    @Named("jTabbedPaneConfig") ConfigTab configTab,
                                    @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.profileManager = profileManager;
    this.templateManager = templateManager;
    this.connectionCase = connectionCase;
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.queryCase = queryCase;
    this.connectionCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    this.connectionCase.getJxTable().addMouseListener(this);
    this.connectionPanel = connectionPanel;
    this.connectionButtonPanel = connectionButtonPanel;
    this.connectionTemplateCase = connectionTemplateCase;
    this.configTab = configTab;
    this.checkboxConfig = checkboxConfig;
    this.checkboxConfig.addItemListener(this);
    this.isSelected = false;

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    List<Connection> connectionListTemplate = templateManager.getConfigList(Connection.class);
    connectionListTemplate.stream()
        .filter(f -> !connectionListTemplate.contains(f.getId()))
        .forEach(connectionIn -> {
          if (connectionIn.getType() != null) {
            connectionTemplateCase.getDefaultTableModel()
                .addRow(new Object[]{connectionIn.getId(), connectionIn.getName(),
                    connectionIn.getUserName(), connectionIn.getPassword(), connectionIn.getUrl(),
                    connectionIn.getJar(), connectionIn.getDriver(), connectionIn.getType(),
                    connectionIn.getHttpMethod(), connectionIn.getParseType()});
          } else {
            connectionTemplateCase.getDefaultTableModel()
                .addRow(new Object[]{connectionIn.getId(), connectionIn.getName(),
                    connectionIn.getUserName(), connectionIn.getPassword(), connectionIn.getUrl(),
                    connectionIn.getJar(), connectionIn.getDriver(), ConnectionType.JDBC, null, null});
          }
        });
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

    if (configTab.isEnabledAt(2)) {
      configTab.setSelectedTab(ConfigEditTabPane.CONNECTION);
    }

    if (!e.getValueIsAdjusting()) {
      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing connection fields");

        emptyTabJDBC();
        connectionPanel.getJButtonTemplate().setEnabled(false);
        emptyTabHTTP();

      } else {
        int connectionID = GUIHelper.getIdByColumnName(connectionCase.getJxTable(),
                                                       connectionCase.getDefaultTableModel(),
                                                       listSelectionModel, TaskColumnNames.ID.getColName());

        ConnectionInfo selectConnection = profileManager.getConnectionInfoById(connectionID);

        ConnectionType connectionType = selectConnection.getType();
        if (connectionType == null) {
          connectionType = ConnectionType.JDBC;
        }

        if (ConnectionType.JDBC.equals(connectionType)) {
          connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.JDBC);
          connectionPanel.getJTextFieldConnectionName().setText(selectConnection.getName());
          connectionPanel.getJTextFieldConnectionURL().setText(selectConnection.getUrl());
          connectionPanel.getJTextFieldConnectionUserName().setText(selectConnection.getUserName());
          connectionPanel.getJTextFieldConnectionPassword().setText(selectConnection.getPassword());
          connectionPanel.getJTextFieldConnectionJar().setText(selectConnection.getJar());
          connectionPanel.getJTextFieldConnectionDriver().setText(selectConnection.getDriver());

          emptyTabHTTP();
        } else if (ConnectionType.HTTP.equals(connectionType)) {
          connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.HTTP);
          connectionPanel.getJTextFieldHttpName().setText(selectConnection.getName());
          connectionPanel.getJTextFieldHttpURL().setText(selectConnection.getUrl());
          connectionPanel.getMethodRadioButtonPanel().setSelectedRadioButton(selectConnection.getHttpMethod());
          connectionPanel.getParseRadioButtonPanel().setSelectedRadioButton(selectConnection.getParseType());

          emptyTabJDBC();
        }

        GUIHelper.disableButton(connectionButtonPanel, !isSelected);
        connectionPanel.getConnectionTemplateCase().getJxTable().setEnabled(!isSelected);
      }
    }
  }

  public void emptyTabJDBC() {
    connectionPanel.getJTextFieldConnectionName().setEditable(false);
    connectionPanel.getJTextFieldConnectionUserName().setEditable(false);
    connectionPanel.getJTextFieldConnectionPassword().setEditable(false);
    connectionPanel.getJTextFieldConnectionDriver().setEditable(false);
    connectionPanel.getJTextFieldConnectionJar().setEditable(false);
    connectionPanel.getJTextFieldConnectionURL().setEditable(false);
    connectionPanel.getJTextFieldConnectionName().setText("");
    connectionPanel.getJTextFieldConnectionUserName().setText("");
    connectionPanel.getJTextFieldConnectionPassword().setText("");
    connectionPanel.getJTextFieldConnectionDriver().setText("");
    connectionPanel.getJTextFieldConnectionJar().setText("");
    connectionPanel.getJTextFieldConnectionURL().setText("");
    connectionPanel.getJTextFieldConnectionName().setPrompt(bundleDefault.getString("cName"));
    connectionPanel.getJTextFieldConnectionUserName().setPrompt(bundleDefault.getString("cUserName"));
    connectionPanel.getJTextFieldConnectionURL().setPrompt(bundleDefault.getString("cURL"));
    connectionPanel.getJTextFieldConnectionJar().setPrompt(bundleDefault.getString("cJar"));
    connectionPanel.getJTextFieldConnectionDriver().setPrompt(bundleDefault.getString("cDriver"));
  }

  public void emptyTabHTTP() {
    connectionPanel.getJTextFieldHttpName().setEditable(false);
    connectionPanel.getJTextFieldHttpURL().setEditable(false);
    connectionPanel.getJTextFieldHttpName().setText("");
    connectionPanel.getJTextFieldHttpURL().setText("");
    connectionPanel.getJTextFieldHttpName().setPrompt(bundleDefault.getString("cName"));
    connectionPanel.getJTextFieldHttpURL().setPrompt(bundleDefault.getString("cURL"));
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (configTab.isEnabledAt(2)) {
      configTab.setSelectedTab(ConfigEditTabPane.CONNECTION);
    }
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    if (e.getSource() == this.checkboxConfig) {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        isSelected = true;
        GUIHelper.disableButton(connectionButtonPanel, false);
        connectionPanel.getConnectionTemplateCase().getJxTable().setEnabled(false);
      } else {
        isSelected = false;
        GUIHelper.disableButton(connectionButtonPanel, true);
        connectionPanel.getConnectionTemplateCase().getJxTable().setEnabled(true);
      }
    }
  }
}


