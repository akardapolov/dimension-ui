package ru.dimension.ui.view.panel.config.connection;

import static ru.dimension.ui.model.view.tab.ConnectionTypeTabPane.HTTP;
import static ru.dimension.ui.model.view.tab.ConnectionTypeTabPane.JDBC;

import java.awt.Dimension;
import java.util.ResourceBundle;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.Border;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.tab.ConnTypeTab;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.panel.config.ButtonPanel;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class ConnectionPanel extends JPanel {

  private final ButtonPanel connectionButtonPanel;
  private final JLabel labelConnectionName;
  private final JLabel labelConnectionURL;
  private final JLabel labelConnectionUserName;
  private final JLabel labelConnectionPassword;
  private final JLabel labelConnectionJar;
  private final JLabel labelConnectionDriver;
  private final JXTextField jTextFieldConnectionName;
  private final JXTextField jTextFieldConnectionURL;
  private final JXTextField jTextFieldConnectionUserName;
  private final JPasswordField jTextFieldConnectionPassword;
  private final JXTextField jTextFieldConnectionJar;
  private final JXTextField jTextFieldConnectionDriver;
  private final JButton jButtonTemplate;
  private final JXTableCase connectionTemplateCase;
  private final JButton jarButton;
  private final ResourceBundle bundleDefault;
  private final ConnTypeTab connTypeTab;

  private final JXTextField jTextFieldHttpName;
  private final JXTextField jTextFieldHttpURL;
  private final MethodRadioButtonPanel methodRadioButtonPanel;
  private final ParseRadioButtonPanel parseRadioButtonPanel;
  private final JButton btnLoadHttp;

  @Inject
  public ConnectionPanel(@Named("connectionButtonPanel") ButtonPanel connectionButtonPanel,
                         @Named("connectionTemplateCase") JXTableCase connectionTemplateCase) {
    this.connectionButtonPanel = connectionButtonPanel;
    this.bundleDefault = Internationalization.getInternationalizationBundle();
    this.labelConnectionName = new JLabel("Name");
    this.labelConnectionURL = new JLabel("URL");
    this.labelConnectionUserName = new JLabel("User name");
    this.labelConnectionPassword = new JLabel("Password");
    this.labelConnectionJar = new JLabel("Jar");
    this.labelConnectionDriver = new JLabel("Driver");
    this.jTextFieldConnectionName = new JXTextField();
    this.jTextFieldConnectionName.setPrompt(bundleDefault.getString("cName"));
    this.jTextFieldConnectionName.setEditable(false);
    this.jTextFieldConnectionURL = new JXTextField();
    this.jTextFieldConnectionURL.setPrompt(bundleDefault.getString("cURL"));
    this.jTextFieldConnectionURL.setEditable(false);
    this.jTextFieldConnectionUserName = new JXTextField();
    this.jTextFieldConnectionUserName.setPrompt(bundleDefault.getString("cUserName"));
    this.jTextFieldConnectionUserName.setEditable(false);
    this.jTextFieldConnectionPassword = new JPasswordField();
    this.jTextFieldConnectionPassword.setEditable(false);
    this.jTextFieldConnectionJar = new JXTextField();
    this.jTextFieldConnectionJar.setPrompt(bundleDefault.getString("cJar"));
    this.jTextFieldConnectionJar.setEditable(false);
    this.jTextFieldConnectionDriver = new JXTextField();
    this.jTextFieldConnectionDriver.setPrompt(bundleDefault.getString("cDriver"));
    this.jTextFieldConnectionDriver.setEditable(false);
    this.jButtonTemplate = new JButton("Copy from template");
    this.jButtonTemplate.setEnabled(false);
    this.jarButton = new JButton("...");
    this.jarButton.setEnabled(false);
    this.jTextFieldHttpName = new JXTextField();
    this.jTextFieldHttpName.setPrompt(bundleDefault.getString("cName"));
    this.jTextFieldHttpName.setEditable(false);
    this.jTextFieldHttpURL = new JXTextField();
    this.jTextFieldHttpURL.setPrompt(bundleDefault.getString("cURL"));
    this.jTextFieldHttpURL.setEditable(false);
    this.btnLoadHttp = new JButton("Load");
    this.btnLoadHttp.setEnabled(false);
    this.methodRadioButtonPanel = new MethodRadioButtonPanel();
    this.methodRadioButtonPanel.setButtonNotView();
    this.parseRadioButtonPanel = new ParseRadioButtonPanel();
    this.parseRadioButtonPanel.setButtonNotView();

    this.connectionTemplateCase = connectionTemplateCase;

    Border finalBorder = GUIHelper.getGrayBorder();
    this.jTextFieldConnectionName.setBorder(finalBorder);
    this.jTextFieldConnectionURL.setBorder(finalBorder);
    this.jTextFieldConnectionUserName.setBorder(finalBorder);
    this.jTextFieldConnectionPassword.setBorder(finalBorder);
    this.jTextFieldConnectionJar.setBorder(finalBorder);
    this.jTextFieldConnectionDriver.setBorder(finalBorder);

    JPanel jarPanel = new TextFieldWithButtonPanel(jarButton, jTextFieldConnectionJar);

    JPanel jdbcPanel = new JPanel();
    PainlessGridBag gblJDBC = new PainlessGridBag(jdbcPanel, PGHelper.getPGConfig(), false);

    gblJDBC.row()
        .cell(labelConnectionName).cell(jTextFieldConnectionName).fillX();
    gblJDBC.row()
        .cell(labelConnectionURL).cell(jTextFieldConnectionURL).fillX();
    gblJDBC.row()
        .cell(labelConnectionUserName).cell(jTextFieldConnectionUserName).fillX();
    gblJDBC.row()
        .cell(labelConnectionPassword).cell(jTextFieldConnectionPassword).fillX();
    gblJDBC.row()
        .cell(labelConnectionJar).cellXRemainder(jarPanel).fillX();
    gblJDBC.row()
        .cell(labelConnectionDriver).cell(jTextFieldConnectionDriver).fillX();
    gblJDBC.row().cellXYRemainder(new JLabel()).fillXY();

    gblJDBC.done();

    JPanel loadPanel = new TextFieldWithButtonPanel(btnLoadHttp, jTextFieldHttpURL);

    jTextFieldHttpName.setPreferredSize(new Dimension(100, loadPanel.getHeight()));

    JPanel httpPanel = new JPanel();
    PainlessGridBag gblHTTP = new PainlessGridBag(httpPanel, PGHelper.getPGConfig(), false);
    gblHTTP.row()
        .cell(new JLabel("Name")).cellXRemainder(jTextFieldHttpName).fillX();
    gblHTTP.row()
        .cell(new JLabel("URL")).cellXRemainder(loadPanel).fillX();
    gblHTTP.row()
        .cell(new JLabel("Method"))
        .cell(methodRadioButtonPanel.getGetMethod())
        .cellXRemainder(new JLabel())
        .fillX();
    gblHTTP.row()
        .cell(new JLabel("Parse"))
        .cell(parseRadioButtonPanel.getParsePrometheus())
        .cell(parseRadioButtonPanel.getParseJson())
        .cellXRemainder(new JLabel())
        .fillX();
    gblHTTP.row()
        .cell(new JLabel()).cellXYRemainder(new JLabel()).fillXY();

    gblHTTP.done();

    JPanel templatePanel = new JPanel();
    PainlessGridBag gblTemplate = new PainlessGridBag(templatePanel, PGHelper.getPGConfig(0), false);
    gblTemplate.row()
        .cellXRemainder(new JXTitledSeparator("List of templates")).fillX();
    gblTemplate.row()
        .cell(jButtonTemplate)
        .cellXRemainder(new JLabel()).fillX();
    gblTemplate.done();

    this.connTypeTab = new ConnTypeTab();
    this.connTypeTab.add(jdbcPanel, JDBC.getName());
    this.connTypeTab.add(httpPanel, HTTP.getName());

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXRemainder(connectionButtonPanel).fillX();
    gbl.row().cellXRemainder(connTypeTab).fillX();
    gbl.row().cellXRemainder(templatePanel).fillX();
    gbl.row().cellXYRemainder(this.connectionTemplateCase.getJScrollPane()).fillXY();
    gbl.done();
  }

  public void setSelectedTabFull(ConnectionTypeTabPane tabbedPane) {
    connTypeTab.setSelectedTab(tabbedPane);

    if (tabbedPane.equals(ConnectionTypeTabPane.JDBC)) {
      connTypeTab.setSelectedTab(tabbedPane);
      connTypeTab.setEnabledTab(tabbedPane, true);
      connTypeTab.setEnabledTab(ConnectionTypeTabPane.HTTP, false);
    } else if (tabbedPane.equals(ConnectionTypeTabPane.HTTP)) {
      connTypeTab.setSelectedTab(tabbedPane);
      connTypeTab.setEnabledTab(tabbedPane, true);
      connTypeTab.setEnabledTab(ConnectionTypeTabPane.JDBC, false);
    }
  }
}
