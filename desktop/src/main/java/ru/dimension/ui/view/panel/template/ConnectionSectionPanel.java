package ru.dimension.ui.view.panel.template;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import org.apache.hc.core5.http.Method;
import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.FieldHighlighter;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.parse.ParseType;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.validation.TemplateDraft;
import ru.dimension.ui.model.validation.ValidationIssue;
import ru.dimension.ui.model.validation.ValidationSection;
import ru.dimension.ui.model.validation.ValidationSeverity;
import ru.dimension.ui.view.panel.config.connection.MethodRadioButtonPanel;
import ru.dimension.ui.view.panel.config.connection.ParseRadioButtonPanel;

/**
 * Единая редактируемая панель подключения на замену трёх дублирующихся
 * TemplateConnPanel/TemplateHTTPConnPanel/TemplateJmxConnPanel.
 * Общие поля (Name, URL) показаны всегда, под ними — специфичные для типа
 * поля через CardLayout. Тип соединения фиксирован шаблоном и не меняется.
 */
public class ConnectionSectionPanel extends JPanel {

  /**
   * Фиксированное количество колонок для текстовых полей подключения.
   * Без явного setColumns(...) preferred-ширина JTextField/JPasswordField
   * с columns == 0 вычисляется от ТЕКУЩЕГО ТЕКСТА поля. Для пустых полей
   * (пароль всегда пустой, username иногда тоже) это приводило к схлопыванию
   * компонента до нескольких пикселей. Явный setColumns(...) гарантирует
   * стабильную ширину независимо от содержимого.
   */
  private static final int TEXT_FIELD_COLUMNS = 20;

  private static final String PASSWORD_TOOLTIP =
      "Leave blank to keep the password from the template connection";

  private final JLabel typeBadge;
  private final ValidatedField connName;
  private final ValidatedField connUrl;

  private final CardLayout cardLayout;
  private final JPanel cardsPanel;

  private final JXTextField jdbcUser;
  private final JXTextField jdbcJar;
  private final JXTextField jdbcDriver;
  /** ВАЖНО: раньше этого поля не было в принципе — Password для JDBC
   *  нельзя было ни увидеть, ни изменить в форме редактирования шаблона. */
  private final JPasswordField jdbcPassword;

  private final MethodRadioButtonPanel methodRadioButtonPanel;
  private final ParseRadioButtonPanel parseRadioButtonPanel;

  private final JXTextField jmxUser;
  private final JPasswordField jmxPassword;

  private ConnectionType currentType = ConnectionType.JDBC;

  public ConnectionSectionPanel() {
    setLayout(new BorderLayout(4, 4));

    this.typeBadge = new JLabel();
    this.connName = new ValidatedField("Connection name...");
    this.connName.setFieldKey("conn.name");
    this.connUrl = new ValidatedField("URL...");
    this.connUrl.setFieldKey("conn.url");

    JPanel commonPanel = new JPanel();
    PainlessGridBag gblCommon = new PainlessGridBag(commonPanel, PGHelper.getPGConfig(0), false);
    gblCommon.row().cell(new JLabel("Type")).cellXRemainder(typeBadge).fillX();
    gblCommon.row().cell(new JLabel("Name")).cellXRemainder(connName).fillX();
    gblCommon.row().cell(new JLabel("URL")).cellXRemainder(connUrl).fillX();
    gblCommon.done();

    Border border = GUIHelper.getGrayBorder();

    this.jdbcUser = new JXTextField();
    this.jdbcUser.setPrompt("Username...");
    this.jdbcUser.setColumns(TEXT_FIELD_COLUMNS);
    this.jdbcUser.setBorder(border);

    this.jdbcJar = new JXTextField();
    this.jdbcJar.setPrompt("Path to driver jar...");
    this.jdbcJar.setColumns(TEXT_FIELD_COLUMNS);
    this.jdbcJar.setBorder(border);

    this.jdbcDriver = new JXTextField();
    this.jdbcDriver.setPrompt("Driver class...");
    this.jdbcDriver.setColumns(TEXT_FIELD_COLUMNS);
    this.jdbcDriver.setBorder(border);

    this.jdbcPassword = new JPasswordField();
    this.jdbcPassword.setColumns(TEXT_FIELD_COLUMNS);
    this.jdbcPassword.setBorder(border);
    this.jdbcPassword.setToolTipText(PASSWORD_TOOLTIP);

    JPanel jdbcPanel = new JPanel();
    PainlessGridBag gblJdbc = new PainlessGridBag(jdbcPanel, PGHelper.getPGConfig(0), false);
    gblJdbc.row().cell(new JLabel("Username")).cellXRemainder(jdbcUser).fillX();
    gblJdbc.row().cell(new JLabel("Password")).cellXRemainder(jdbcPassword).fillX();
    gblJdbc.row().cell(new JLabel("Jar")).cellXRemainder(jdbcJar).fillX();
    gblJdbc.row().cell(new JLabel("Driver")).cellXRemainder(jdbcDriver).fillX();
    gblJdbc.row().cellXYRemainder(new JLabel()).fillXY();
    gblJdbc.done();

    this.methodRadioButtonPanel = new MethodRadioButtonPanel();
    this.methodRadioButtonPanel.setButtonView();
    this.parseRadioButtonPanel = new ParseRadioButtonPanel();
    this.parseRadioButtonPanel.setButtonView();

    JPanel httpPanel = new JPanel();
    PainlessGridBag gblHttp = new PainlessGridBag(httpPanel, PGHelper.getPGConfig(0), false);
    gblHttp.row().cell(new JLabel("Method"))
        .cell(methodRadioButtonPanel.getGetMethod())
        .cellXRemainder(new JLabel()).fillX();
    gblHttp.row().cell(new JLabel("Parse"))
        .cell(parseRadioButtonPanel.getParsePrometheus())
        .cell(parseRadioButtonPanel.getParseJson())
        .cellXRemainder(new JLabel()).fillX();
    gblHttp.row().cellXYRemainder(new JLabel()).fillXY();
    gblHttp.done();

    this.jmxUser = new JXTextField();
    this.jmxUser.setPrompt("Username...");
    this.jmxUser.setColumns(TEXT_FIELD_COLUMNS);
    this.jmxUser.setBorder(border);

    this.jmxPassword = new JPasswordField();
    this.jmxPassword.setColumns(TEXT_FIELD_COLUMNS);
    this.jmxPassword.setBorder(border);
    this.jmxPassword.setToolTipText(PASSWORD_TOOLTIP);

    JPanel jmxPanel = new JPanel();
    PainlessGridBag gblJmx = new PainlessGridBag(jmxPanel, PGHelper.getPGConfig(0), false);
    gblJmx.row().cell(new JLabel("Username")).cellXRemainder(jmxUser).fillX();
    gblJmx.row().cell(new JLabel("Password")).cellXRemainder(jmxPassword).fillX();
    gblJmx.row().cellXYRemainder(new JLabel()).fillXY();
    gblJmx.done();

    this.cardLayout = new CardLayout();
    this.cardsPanel = new JPanel(cardLayout);
    cardsPanel.add(jdbcPanel, ConnectionType.JDBC.name());
    cardsPanel.add(httpPanel, ConnectionType.HTTP.name());
    cardsPanel.add(jmxPanel, ConnectionType.JMX.name());

    add(new JXTitledSeparator("Connection"), BorderLayout.NORTH);

    JPanel center = new JPanel(new BorderLayout(4, 8));
    center.add(commonPanel, BorderLayout.NORTH);
    center.add(cardsPanel, BorderLayout.CENTER);
    add(center, BorderLayout.CENTER);
  }

  public void setOnChange(Runnable onChange) {
    connName.setOnChange(onChange);
    connUrl.setOnChange(onChange);
    addDocListener(jdbcUser, onChange);
    addDocListener(jdbcJar, onChange);
    addDocListener(jdbcDriver, onChange);
    addPasswordDocListener(jdbcPassword, onChange);
    addDocListener(jmxUser, onChange);
    addPasswordDocListener(jmxPassword, onChange);
  }

  private void addDocListener(JXTextField field, Runnable onChange) {
    field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
      @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
      @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    });
  }

  private void addPasswordDocListener(JPasswordField field, Runnable onChange) {
    field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
      @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
      @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    });
  }

  /** Загрузка из драфта (валидируемые поля) + источник (non-validated: method/parse/password/driver). */
  public void loadFrom(TemplateDraft draft, Connection source) {
    this.currentType = draft.getConnectionType() == null ? ConnectionType.JDBC : draft.getConnectionType();
    typeBadge.setText("[ " + currentType.getName() + " ]");

    connName.setText(draft.getConnName());
    connUrl.setText(draft.getConnUrl());

    jdbcUser.setText(draft.getConnUserName());
    jdbcJar.setText(draft.getConnJar());
    jdbcDriver.setText(draft.getConnDriver());
    // Пароль никогда не подставляем в текст (не расшифровываем существующий) —
    // соображения безопасности. Пустое поле = "оставить текущий пароль как есть".
    jdbcPassword.setText("");

    if (source != null && currentType == ConnectionType.HTTP) {
      methodRadioButtonPanel.setSelectedRadioButton(source.getHttpMethod());
      parseRadioButtonPanel.setSelectedRadioButton(source.getParseType());
    }
    if (source != null && currentType == ConnectionType.JMX) {
      jmxUser.setText(source.getUserName());
      jmxPassword.setText("");
    }

    cardLayout.show(cardsPanel, currentType.name());
  }

  public void applyTo(TemplateDraft draft) {
    draft.setConnName(connName.getText());
    draft.setConnUrl(connUrl.getText());
    draft.setConnUserName(currentType == ConnectionType.JMX ? jmxUser.getText() : jdbcUser.getText());
    draft.setConnJar(jdbcJar.getText());
    draft.setConnDriver(jdbcDriver.getText());
  }

  /**
   * Подсветка полей по результатам валидации. Подписей под полями больше нет —
   * сообщение доступно через tooltip (FieldHighlighter) и сводится в общую
   * строку состояния диалога. Цвет подсветки всегда жёлтый (WARNING).
   */
  public void applyIssues(List<ValidationIssue> issues) {
    connName.setIssue(findIssue(issues, "conn.name"));
    connUrl.setIssue(findIssue(issues, "conn.url"));

    ValidationIssue jarIssue = findIssue(issues, "conn.jar");
    if (jarIssue != null && currentType == ConnectionType.JDBC) {
      FieldHighlighter.apply(jdbcJar, ValidationSeverity.WARNING, jarIssue.getMessage());
    } else {
      FieldHighlighter.clear(jdbcJar);
    }
  }

  private ValidationIssue findIssue(List<ValidationIssue> issues, String fieldKey) {
    return issues.stream()
        .filter(i -> i.getSection() == ValidationSection.CONNECTION && fieldKey.equals(i.getFieldKey()))
        .findFirst().orElse(null);
  }

  public Method getSelectedHttpMethod() {
    JRadioButton selected = GUIHelper.getSelectedButton(methodRadioButtonPanel.getButtonGroup());
    return selected == null ? Method.GET : Method.normalizedValueOf(selected.getText());
  }

  public ParseType getSelectedParseType() {
    JRadioButton selected = GUIHelper.getSelectedButton(parseRadioButtonPanel.getButtonGroup());
    return selected == null ? ParseType.JSON : ParseType.valueOf(selected.getText().toUpperCase());
  }

  public char[] getJmxPasswordChars() {
    return jmxPassword.getPassword();
  }

  /** Новый метод — пароль для JDBC-подключения, введённый пользователем (может быть пустым). */
  public char[] getJdbcPasswordChars() {
    return jdbcPassword.getPassword();
  }

  public ValidatedField getConnNameField() {
    return connName;
  }
}