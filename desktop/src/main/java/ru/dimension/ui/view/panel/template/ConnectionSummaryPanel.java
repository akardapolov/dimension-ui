package ru.dimension.ui.view.panel.template;

import java.awt.CardLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.Border;
import org.jdesktop.swingx.JXTextField;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.view.panel.config.connection.MethodRadioButtonPanel;
import ru.dimension.ui.view.panel.config.connection.ParseRadioButtonPanel;

/**
 * Read-only сводка подключения для правой панели обзора шаблона (Вариант 2).
 * Заменяет собой TemplateConnPanel / TemplateHTTPConnPanel / TemplateJmxConnPanel.
 */
public class ConnectionSummaryPanel extends JPanel {

  /** См. комментарий в ConnectionSectionPanel.TEXT_FIELD_COLUMNS. */
  private static final int TEXT_FIELD_COLUMNS = 20;

  private final CardLayout cardLayout;

  private final JXTextField jdbcUrl;
  private final JXTextField jdbcUser;
  private final JXTextField jdbcJar;
  private final JXTextField jdbcDriver;

  private final JXTextField httpUrl;
  private final MethodRadioButtonPanel httpMethodPanel;
  private final ParseRadioButtonPanel httpParsePanel;

  private final JXTextField jmxUrl;
  private final JXTextField jmxUser;
  private final JPasswordField jmxPassword;

  public ConnectionSummaryPanel() {
    this.cardLayout = new CardLayout();
    setLayout(cardLayout);

    Border border = GUIHelper.getGrayBorder();

    this.jdbcUrl = readOnlyField(border);
    this.jdbcUser = readOnlyField(border);
    this.jdbcJar = readOnlyField(border);
    this.jdbcDriver = readOnlyField(border);

    JPanel jdbcPanel = new JPanel();
    PainlessGridBag gblJdbc = new PainlessGridBag(jdbcPanel, PGHelper.getPGConfig(0), false);
    gblJdbc.row().cell(new JLabel("URL")).cellXRemainder(jdbcUrl).fillX();
    gblJdbc.row().cell(new JLabel("Username")).cellXRemainder(jdbcUser).fillX();
    gblJdbc.row().cell(new JLabel("Jar")).cellXRemainder(jdbcJar).fillX();
    gblJdbc.row().cell(new JLabel("Driver")).cellXRemainder(jdbcDriver).fillX();
    gblJdbc.row().cellXYRemainder(new JLabel()).fillXY();
    gblJdbc.done();

    this.httpUrl = readOnlyField(border);
    this.httpMethodPanel = new MethodRadioButtonPanel();
    this.httpMethodPanel.setButtonNotView();
    this.httpParsePanel = new ParseRadioButtonPanel();
    this.httpParsePanel.setButtonNotView();

    JPanel httpPanel = new JPanel();
    PainlessGridBag gblHttp = new PainlessGridBag(httpPanel, PGHelper.getPGConfig(0), false);
    gblHttp.row().cell(new JLabel("URL")).cellXRemainder(httpUrl).fillX();
    gblHttp.row().cell(new JLabel("Method")).cell(httpMethodPanel.getGetMethod())
        .cellXRemainder(new JLabel()).fillX();
    gblHttp.row().cell(new JLabel("Parse"))
        .cell(httpParsePanel.getParsePrometheus())
        .cell(httpParsePanel.getParseJson())
        .cellXRemainder(new JLabel()).fillX();
    gblHttp.row().cellXYRemainder(new JLabel()).fillXY();
    gblHttp.done();

    this.jmxUrl = readOnlyField(border);
    this.jmxUser = readOnlyField(border);
    this.jmxPassword = new JPasswordField();
    this.jmxPassword.setEditable(false);
    this.jmxPassword.setColumns(TEXT_FIELD_COLUMNS);
    this.jmxPassword.setBorder(border);

    JPanel jmxPanel = new JPanel();
    PainlessGridBag gblJmx = new PainlessGridBag(jmxPanel, PGHelper.getPGConfig(0), false);
    gblJmx.row().cell(new JLabel("URL")).cellXRemainder(jmxUrl).fillX();
    gblJmx.row().cell(new JLabel("Username")).cellXRemainder(jmxUser).fillX();
    gblJmx.row().cell(new JLabel("Password")).cellXRemainder(jmxPassword).fillX();
    gblJmx.row().cellXYRemainder(new JLabel()).fillXY();
    gblJmx.done();

    add(jdbcPanel, ConnectionType.JDBC.name());
    add(httpPanel, ConnectionType.HTTP.name());
    add(jmxPanel, ConnectionType.JMX.name());
  }

  private JXTextField readOnlyField(Border border) {
    JXTextField field = new JXTextField();
    field.setEditable(false);
    field.setColumns(TEXT_FIELD_COLUMNS);
    field.setBorder(border);
    return field;
  }

  public void show(Connection connection) {
    if (connection == null) {
      showEmpty();
      return;
    }
    ConnectionType type = connection.getType() == null ? ConnectionType.JDBC : connection.getType();

    switch (type) {
      case JDBC -> {
        jdbcUrl.setText(connection.getUrl());
        jdbcUser.setText(connection.getUserName());
        jdbcJar.setText(connection.getJar());
        jdbcDriver.setText(connection.getDriver());
      }
      case HTTP -> {
        httpUrl.setText(connection.getUrl());
        httpMethodPanel.setSelectedRadioButton(connection.getHttpMethod());
        httpParsePanel.setSelectedRadioButton(connection.getParseType());
      }
      case JMX -> {
        jmxUrl.setText(connection.getUrl());
        jmxUser.setText(connection.getUserName());
        jmxPassword.setText(connection.getPassword() == null || connection.getPassword().isEmpty() ? "" : "••••••••");
      }
    }
    cardLayout.show(this, type.name());
  }

  public void showEmpty() {
    jdbcUrl.setText("");
    jdbcUser.setText("");
    jdbcJar.setText("");
    jdbcDriver.setText("");
    httpUrl.setText("");
    jmxUrl.setText("");
    jmxUser.setText("");
    jmxPassword.setText("");
  }
}