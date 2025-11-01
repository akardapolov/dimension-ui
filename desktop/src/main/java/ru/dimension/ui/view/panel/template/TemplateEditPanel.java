package ru.dimension.ui.view.panel.template;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.util.List;
import java.util.ResourceBundle;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXTextArea;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.column.QueryColumnNames;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;
import ru.dimension.ui.view.panel.config.connection.MethodRadioButtonPanel;
import ru.dimension.ui.view.panel.config.connection.ParseRadioButtonPanel;
import ru.dimension.ui.view.tab.ConnTypeTab;
import ru.dimension.ui.prompt.Internationalization;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class TemplateEditPanel extends JDialog {

  private final JButton templateSaveJButton;

  private final JLabel labelProfileName;
  private final JLabel labelProfileDesc;

  private final JLabel labelTaskName;
  private final JLabel labelTaskDesc;

  private final JLabel labelQueryName;
  private final JLabel labelQueryDesc;

  private final JLabel labelConnName;
  private final JLabel labelConnUserName;
  private final JLabel labelConnPassword;
  private final JLabel labelConnUrl;
  private final JLabel labelConnPort;
  private final JLabel labelConnJar;

  private final JXTextField profileName;
  private final JXTextArea profileDesc;
  private final JLabel statusProfile;

  private final JXTextField taskName;
  private final JXTextArea taskDesc;
  private final JLabel statusTask;

  private final ConnTypeTab connTypeTab;
  private final JPanel jdbcPanel;
  private final JPanel httpPanel;
  private final JXTextField connName;
  private final JXTextField connUserName;
  private final JPasswordField connPassword;
  private final JXTextField connUrl;
  private final JXTextField connJar;
  private final JLabel statusConn;

  private final JXTextField jTextFieldHttpName;
  private final JXTextField jTextFieldHttpURL;
  private final MethodRadioButtonPanel methodRadioButtonPanel;
  private final ParseRadioButtonPanel parseRadioButtonPanel;

  private final JXTextField queryName;
  private final JXTextField queryDesc;
  private final JXLabel statusQuery;

  private final JXTableCase templateQueryCase;
  private final ResourceBundle bundleDefault;

  @Inject
  public TemplateEditPanel(@Named("templateSaveJButton") JButton templateSaveJButton,
                           @Named("templateProfileDescSave") JXTextArea profileDesc,
                           @Named("templateTaskDescSave") JXTextArea taskDesc) {
    this.bundleDefault = Internationalization.getInternationalizationBundle();
    this.templateSaveJButton = templateSaveJButton;

    this.labelProfileName = new JLabel("Name");
    this.labelProfileDesc = new JLabel("Description");

    this.labelTaskName = new JLabel("Name");
    this.labelTaskDesc = new JLabel("Description");

    this.labelQueryName = new JLabel("Name");
    this.labelQueryDesc = new JLabel("Description");

    this.labelConnName = new JLabel("Name");
    this.labelConnUserName = new JLabel("Username");
    this.labelConnPassword = new JLabel("Password");
    this.labelConnUrl = new JLabel("Url");
    this.labelConnPort = new JLabel("Port");
    this.labelConnJar = new JLabel("Jar");

    this.profileName = new JXTextField();
    this.profileName.setPrompt("Profile name");
    this.profileDesc = profileDesc;

    this.statusProfile = new JLabel("Profile already exist");
    this.statusProfile.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
    this.statusProfile.setOpaque(true);
    this.statusProfile.setVisible(false);

    this.taskName = new JXTextField();
    this.taskName.setPrompt("Task name");
    this.taskDesc = taskDesc;

    this.statusTask = new JLabel("Task already exist");
    this.statusTask.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
    this.statusTask.setOpaque(true);
    this.statusTask.setVisible(false);

    this.connTypeTab = new ConnTypeTab();
    this.jdbcPanel = new JPanel();
    this.httpPanel = new JPanel();

    this.connName = new JXTextField();
    this.connName.setPrompt("Connection name");
    this.connUserName = new JXTextField();
    this.connUserName.setPrompt("Database user name");
    this.connPassword = new JPasswordField();
    this.connJar = new JXTextField();
    this.connJar.setPrompt("Path to JDBC jar file");
    this.connUrl = new JXTextField();
    this.connUrl.setPrompt("Hostname for data source");

    this.statusConn = new JLabel("Connection already exist");
    this.statusConn.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
    this.statusConn.setOpaque(true);
    this.statusConn.setVisible(false);

    this.jTextFieldHttpName = new JXTextField();
    this.jTextFieldHttpName.setPrompt(bundleDefault.getString("cName"));
    this.jTextFieldHttpURL = new JXTextField();
    this.jTextFieldHttpURL.setPrompt(bundleDefault.getString("cURL"));
    this.methodRadioButtonPanel = new MethodRadioButtonPanel();
    this.methodRadioButtonPanel.setButtonView();
    this.parseRadioButtonPanel = new ParseRadioButtonPanel();
    this.parseRadioButtonPanel.setButtonView();

    this.queryName = new JXTextField();
    this.queryName.setPrompt("Query name");
    this.queryDesc = new JXTextField();
    this.queryDesc.setPrompt("Query description");

    this.statusQuery = new JXLabel("Query already exist");
    this.statusQuery.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
    this.statusQuery.setOpaque(true);
    this.statusQuery.setVisible(false);
    this.statusQuery.setAutoscrolls(true);
    this.statusQuery.setLineWrap(true);

    this.statusQuery.setLineWrap(true);

    this.templateQueryCase = loadTemplateEditQueryCase();

    JPanel panelEntities = new JPanel();
    JPanel panelStatus = new JPanel();
    panelStatus.setPreferredSize(new Dimension(100, 45));
    panelStatus.setMaximumSize(new Dimension(100, 45));
    panelStatus.setMinimumSize(new Dimension(100, 45));
    JPanel panelSave = new JPanel(new FlowLayout(FlowLayout.CENTER));

    PainlessGridBag gblEntity = new PainlessGridBag(panelEntities, PGHelper.getPGConfig(0), false);

    gblEntity.row()
        .cell(new JXTitledSeparator("Profile")).fillX()
        .cell(new JXTitledSeparator("Task")).fillX()
        .cell(new JXTitledSeparator("Connection")).fillX()
        .cell(new JXTitledSeparator("Query")).fillX();

    gblEntity.row()
        .cell(fillProfilePanel()).fillXY()
        .cell(fillTaskPanel()).fillXY()
        .cell(fillConnectionPanel()).fillXY()
        .cell(fillQueryPanel()).fillXY();

    gblEntity.done();

    PainlessGridBag gblStatus = new PainlessGridBag(panelStatus, PGHelper.getPGConfig(0), false);

    gblStatus.row().cell(new JLabel("Status")).fillX()
        .cell(GUIHelper.getJScrollPane(statusTask)).fillXY()
        .cell(GUIHelper.getJScrollPane(statusConn)).fillXY()
        .cell(GUIHelper.getJScrollPane(statusQuery)).fillXY();
    gblStatus.done();

    panelSave.add(templateSaveJButton);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXRemainder(panelEntities).fillXY();
    gbl.row().cellXRemainder(panelStatus).fillXY();
    gbl.row().cell(panelSave).fillX();

    gbl.done();

    this.setTitle("Edit template values");

    this.packTemplate(false);
  }

  private void packTemplate(boolean visible) {
    this.setVisible(visible);
    this.setResizable(false);
    this.setModal(true);
    this.setResizable(true);
    this.pack();

    this.setSize(new Dimension(1200, 500));
    this.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - getWidth() / 2,
                     (Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - getHeight() / 2);
  }

  public JXTableCase loadTemplateEditQueryCase() {
    JXTableCase jxTableCase =
        GUIHelper.getEditJXTableCase(3,
                                     new String[]{
                                         QueryColumnNames.ID.getColName(),
                                         QueryColumnNames.NAME.getColName(),
                                         QueryColumnNames.DESCRIPTION.getColName()
                                     });

    jxTableCase.getJxTable().getColumnExt(0).setVisible(false);
    jxTableCase.getJxTable().getColumnModel().getColumn(0).setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());

    jxTableCase.getJxTable().setSortable(false);
    return jxTableCase;
  }

  public void updateModelTemplateEditQueryCase(List<Query> queryList) {
    templateQueryCase.getDefaultTableModel().getDataVector().removeAllElements();
    templateQueryCase.getDefaultTableModel().fireTableDataChanged();

    queryList.forEach(e ->
                          templateQueryCase
                              .getDefaultTableModel()
                              .addRow(new Object[]{e.getId(), e.getName(), e.getDescription()}));
  }


  private JPanel fillProfilePanel() {
    JPanel panelProfile = new JPanel();

    PainlessGridBag gblProfile = new PainlessGridBag(panelProfile, PGHelper.getPGConfig(0), false);

    gblProfile.row()
        .cell(labelProfileName).cellXRemainder(profileName).fillX();
    gblProfile.row()
        .cell(labelProfileDesc).cellXRemainder(new JScrollPane(profileDesc)).fillXY();

    gblProfile.done();

    return panelProfile;
  }

  private JPanel fillTaskPanel() {
    JPanel panelTask = new JPanel();

    PainlessGridBag gblTask = new PainlessGridBag(panelTask, PGHelper.getPGConfig(0), false);

    gblTask.row()
        .cell(labelTaskName).cell(taskName).fillX();
    gblTask.row()
        .cell(labelTaskDesc).cell(new JScrollPane(taskDesc)).fillXY();

    gblTask.done();

    return panelTask;
  }

  private JTabbedPane fillConnectionPanel() {

    PainlessGridBag gblJDBC = new PainlessGridBag(jdbcPanel, PGHelper.getPGConfig(), false);

    gblJDBC.row()
        .cell(labelConnName).cell(connName).fillX();
    gblJDBC.row()
        .cell(labelConnUserName).cell(connUserName).fillX();
    gblJDBC.row()
        .cell(labelConnPassword).cell(connPassword).fillX();
    gblJDBC.row()
        .cell(labelConnUrl).cell(connUrl).fillX();
    gblJDBC.row()
        .cell(labelConnJar).cell(connJar).fillX();
    gblJDBC.row()
        .cellXYRemainder(new JLabel()).fillXY();
    gblJDBC.done();

    PainlessGridBag gblHTTP = new PainlessGridBag(httpPanel, PGHelper.getPGConfig(), false);
    gblHTTP.row()
        .cell(new JLabel("Name")).cellXRemainder(jTextFieldHttpName).fillX();
    gblHTTP.row()
        .cell(new JLabel("URL")).cellXRemainder(jTextFieldHttpURL).fillX();
    gblHTTP.row()
        .cell(new JLabel("Method"))
        .cell(methodRadioButtonPanel.getGetMethod())
        .cellXRemainder(new JLabel()).fillX();
    gblHTTP.row()
        .cell(new JLabel("Parse"))
        .cell(parseRadioButtonPanel.getParsePrometheus())
        .cell(parseRadioButtonPanel.getParseJson())
        .cellXRemainder(new JLabel())
        .fillX();
    gblHTTP.row()
        .cellXYRemainder(new JLabel()).fillXY();

    gblHTTP.done();

    this.connTypeTab.add(jdbcPanel, ConnectionTypeTabPane.JDBC.getName());
    this.connTypeTab.add(httpPanel, ConnectionTypeTabPane.HTTP.getName());

    return connTypeTab;
  }

  private JPanel fillQueryPanel() {
    JPanel panelQuery = new JPanel();

    PainlessGridBag gblQuery = new PainlessGridBag(panelQuery, PGHelper.getPGConfig(0), false);
    gblQuery.row()
        .cell(templateQueryCase.getJScrollPane()).fillXY();

    gblQuery.done();

    return panelQuery;
  }

  public void setEmptyJdbcPanel() {
    this.connName.setText("");
    this.connUserName.setText("");
    this.connPassword.setText("");
    this.connUrl.setText("");
    this.connJar.setText("");
  }

  public void setEmptyHttpPanel() {
    this.jTextFieldHttpName.setText("");
    this.jTextFieldHttpURL.setText("");
    this.methodRadioButtonPanel.setButtonView();
    this.parseRadioButtonPanel.setButtonView();
  }
}
