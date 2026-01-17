package ru.dimension.ui.view.handler.connection;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.ItemEvent;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.swing.JCheckBox;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.handler.core.AbstractTableSelectionHandler;
import ru.dimension.ui.view.handler.core.ButtonPanelBindings;
import ru.dimension.ui.view.handler.core.ConfigSelectionContext;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.connection.ConnectionPanel;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;

@Log4j2
@Singleton
public final class ConnectionSelectionHandler extends AbstractTableSelectionHandler<ConnectionRow> {

  private final ProfileManager profileManager;
  private final ConfigSelectionContext context;

  private final ConnectionPanel connectionPanel;
  private final ButtonPanel connectionButtonPanel;
  private final JXTableCase connectionTemplateCase;
  private final JCheckBox checkboxConfig;

  private final ResourceBundle bundleDefault;

  @Inject
  public ConnectionSelectionHandler(@Named("connectionConfigCase") JXTableCase connectionCase,
                                    @Named("profileManager") ProfileManager profileManager,
                                    @Named("configSelectionContext") ConfigSelectionContext context,
                                    @Named("connectionConfigPanel") ConnectionPanel connectionPanel,
                                    @Named("connectionButtonPanel") ButtonPanel connectionButtonPanel,
                                    @Named("connectionTemplateCase") JXTableCase connectionTemplateCase,
                                    @Named("checkboxConfig") JCheckBox checkboxConfig) {
    super(connectionCase);
    this.profileManager = profileManager;
    this.context = context;
    this.connectionPanel = connectionPanel;
    this.connectionButtonPanel = connectionButtonPanel;
    this.connectionTemplateCase = connectionTemplateCase;
    this.checkboxConfig = checkboxConfig;
    this.bundleDefault = Internationalization.getInternationalizationBundle();

    bind();

    this.checkboxConfig.addItemListener(e -> applyCheckboxState(e.getStateChange() == ItemEvent.SELECTED));
    applyCheckboxState(this.checkboxConfig.isSelected());
  }

  @Override
  protected void onSelection(Optional<ConnectionRow> item) {
    Integer id = item.map(ConnectionRow::getId).orElse(null);
    context.setSelectedConnectionId(id);

    if (id == null) {
      clearJdbc();
      clearHttp();
      connectionPanel.getJButtonTemplate().setEnabled(false);
      ButtonPanelBindings.setViewMode(connectionButtonPanel, false);
      return;
    }

    ConnectionInfo info = profileManager.getConnectionInfoById(id);
    if (info == null) {
      clearJdbc();
      clearHttp();
      connectionPanel.getJButtonTemplate().setEnabled(false);
      ButtonPanelBindings.setViewMode(connectionButtonPanel, false);
      return;
    }

    ConnectionType type = info.getType() != null ? info.getType() : ConnectionType.JDBC;

    if (ConnectionType.HTTP.equals(type)) {
      fillHttp(info);
      clearJdbc();
      connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.HTTP);
    } else {
      fillJdbc(info);
      clearHttp();
      connectionPanel.setSelectedTabFull(ConnectionTypeTabPane.JDBC);
    }

    ButtonPanelBindings.setViewMode(connectionButtonPanel, true);
    applyCheckboxState(checkboxConfig.isSelected());
  }

  private void applyCheckboxState(boolean enabled) {
    GUIHelper.disableButton(connectionButtonPanel, !enabled);
    JXTable tt = connectionTemplateCase.getJxTable();
    if (tt != null) {
      tt.setEnabled(!enabled);
    }
  }

  private void fillJdbc(ConnectionInfo info) {
    connectionPanel.getJTextFieldConnectionName().setText(info.getName());
    connectionPanel.getJTextFieldConnectionURL().setText(info.getUrl());
    connectionPanel.getJTextFieldConnectionUserName().setText(info.getUserName());
    connectionPanel.getJTextFieldConnectionPassword().setText(info.getPassword());
    connectionPanel.getJTextFieldConnectionJar().setText(info.getJar());
    connectionPanel.getJTextFieldConnectionDriver().setText(info.getDriver());
  }

  private void fillHttp(ConnectionInfo info) {
    connectionPanel.getJTextFieldHttpName().setText(info.getName());
    connectionPanel.getJTextFieldHttpURL().setText(info.getUrl());
    if (info.getHttpMethod() != null) {
      connectionPanel.getMethodRadioButtonPanel().setSelectedRadioButton(info.getHttpMethod());
    }
    if (info.getParseType() != null) {
      connectionPanel.getParseRadioButtonPanel().setSelectedRadioButton(info.getParseType());
    }
  }

  private void clearJdbc() {
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

  private void clearHttp() {
    connectionPanel.getJTextFieldHttpName().setEditable(false);
    connectionPanel.getJTextFieldHttpURL().setEditable(false);

    connectionPanel.getJTextFieldHttpName().setText("");
    connectionPanel.getJTextFieldHttpURL().setText("");

    connectionPanel.getJTextFieldHttpName().setPrompt(bundleDefault.getString("cName"));
    connectionPanel.getJTextFieldHttpURL().setPrompt(bundleDefault.getString("cURL"));

    connectionPanel.getBtnLoadHttp().setEnabled(false);
    connectionPanel.getMethodRadioButtonPanel().setButtonNotView();
    connectionPanel.getParseRadioButtonPanel().setButtonNotView();
  }
}