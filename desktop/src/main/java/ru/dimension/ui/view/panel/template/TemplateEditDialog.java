package ru.dimension.ui.view.panel.template;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.util.List;
import ru.dimension.ui.helper.FieldHighlighter;
import ru.dimension.ui.model.validation.TemplateDraft;
import ru.dimension.ui.model.validation.ValidationIssue;
import ru.dimension.ui.model.validation.ValidationSection;
import ru.dimension.ui.model.validation.ValidationSeverity;

/**
 * Отдельная немодальная форма редактирования шаблона перед сохранением в
 * Configuration. Открывается по кнопке "Load to Configuration" и
 * ЗАКРЫВАЕТСЯ (а не остаётся встроенной частью главного экрана), чтобы не
 * путать пользователя двумя одновременно видимыми формами.
 * <p>
 * Раскладка: сверху в один ряд — карточки сущностей (Profile, Task,
 * Connection, Queries), снизу — большая панель с предпросмотром того, что
 * будет добавлено в Configuration. Верхний и нижний блок разделены
 * перетаскиваемым {@link JSplitPane} (VERTICAL_SPLIT) — пользователь может
 * сам менять пропорции между формой редактирования и предпросмотром.
 * <p>
 * Строка состояния внизу содержит краткую сводку по валидации, а при
 * наведении на неё всплывает подсказка (tooltip) со списком ВСЕХ причин
 * ошибок/warning-ов по всем секциям — чтобы не пришлось открывать каждую
 * секцию по очереди, чтобы понять, что именно не так.
 */
public class TemplateEditDialog extends JDialog {

  private static final Color PROFILE_COLOR = new Color(0x42, 0x85, 0xF4);
  private static final Color TASK_COLOR = new Color(0x43, 0xA0, 0x47);
  private static final Color CONN_COLOR = new Color(0x8E, 0x24, 0xAA);
  private static final Color QUERIES_COLOR = new Color(0x00, 0x89, 0x7B);
  private static final Color PREVIEW_COLOR = new Color(0xF9, 0xA8, 0x25);
  private static final Color OK_COLOR = new Color(0x2E, 0x7D, 0x32);

  private final JLabel titleLabel;
  private final ProfileSectionPanel profilePanel;
  private final TaskSectionPanel taskPanel;
  private final ConnectionSectionPanel connectionPanel;
  private final QueriesSectionPanel queriesPanel;
  private final TemplateConfigTreePreviewPanel previewPanel;

  private final JLabel statusBarLabel;
  private final JButton saveButton;
  private final JButton cancelButton;

  public TemplateEditDialog(ProfileSectionPanel profilePanel,
                            TaskSectionPanel taskPanel,
                            ConnectionSectionPanel connectionPanel,
                            QueriesSectionPanel queriesPanel) {
    super();
    setTitle("Edit template values");
    setModalityType(ModalityType.APPLICATION_MODAL);
    setLayout(new BorderLayout(6, 6));

    this.profilePanel = profilePanel;
    this.taskPanel = taskPanel;
    this.connectionPanel = connectionPanel;
    this.queriesPanel = queriesPanel;
    this.previewPanel = new TemplateConfigTreePreviewPanel();

    this.titleLabel = new JLabel("Edit template values");
    this.titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
    this.titleLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));

    JPanel cardsRow = new JPanel(new GridLayout(1, 4, 6, 6));
    cardsRow.add(new SectionCard("Profile", profilePanel, PROFILE_COLOR));
    cardsRow.add(new SectionCard("Task", taskPanel, TASK_COLOR));
    cardsRow.add(new SectionCard("Connection", connectionPanel, CONN_COLOR));
    cardsRow.add(new SectionCard("Queries", queriesPanel, QUERIES_COLOR));

    JPanel topWrap = new JPanel(new BorderLayout());
    topWrap.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
    topWrap.add(cardsRow, BorderLayout.CENTER);

    JPanel previewCard = new SectionCard("Will be added to Configuration", previewPanel, PREVIEW_COLOR);
    JPanel bottomWrap = new JPanel(new BorderLayout());
    bottomWrap.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
    bottomWrap.add(previewCard, BorderLayout.CENTER);

    // Верхняя форма и нижний предпросмотр разделены перетаскиваемым
    // сплиттером вместо фиксированного BorderLayout(NORTH/CENTER) —
    // пользователь может вручную растянуть любую из частей под себя.
    JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topWrap, bottomWrap);
    verticalSplit.setResizeWeight(0.45);
    verticalSplit.setBorder(null);
    verticalSplit.setContinuousLayout(true);
    verticalSplit.setOneTouchExpandable(true);

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(verticalSplit, BorderLayout.CENTER);

    this.statusBarLabel = new JLabel(" ");
    this.saveButton = new JButton("Save to Configuration");
    this.cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(e -> setVisible(false));

    JPanel bottom = new JPanel(new BorderLayout());
    bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
    bottom.add(statusBarLabel, BorderLayout.WEST);
    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    buttonsPanel.add(cancelButton);
    buttonsPanel.add(saveButton);
    bottom.add(buttonsPanel, BorderLayout.EAST);

    add(titleLabel, BorderLayout.NORTH);
    add(contentPanel, BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);

    setSize(new Dimension(
        Math.min(1200, Toolkit.getDefaultToolkit().getScreenSize().width - 200),
        Math.min(680, Toolkit.getDefaultToolkit().getScreenSize().height - 150)));
    setLocationRelativeTo(null);
  }

  public void open(String templateName) {
    titleLabel.setText("Edit template values — " + templateName);
    setVisible(true);
    SwingUtilities.invokeLater(this::toFront);
  }

  public void close() {
    setVisible(false);
  }

  public void updatePreview(TemplateDraft draft) {
    previewPanel.update(draft);
  }

  public void showAutoRenameHint(ValidationSection section, String newValue) {
    statusBarLabel.setForeground(new Color(0x01, 0x57, 0x9E));
    statusBarLabel.setText("ℹ auto-renamed " + section.getTitle().toLowerCase()
                               + " to avoid duplicate: \"" + newValue + "\"");
  }

  /**
   * Сводка по валидации в строке состояния. Красный цвет нигде не
   * используется — ошибки и предупреждения показываются жёлтым,
   * различается только символ и текст сообщения. Полный разбор причин —
   * во всплывающей подсказке (tooltip) при наведении на строку состояния.
   */
  public void refreshValidation(List<ValidationIssue> issues) {
    profilePanel.applyIssues(issues);
    taskPanel.applyIssues(issues);
    connectionPanel.applyIssues(issues);
    queriesPanel.applyIssues(issues);

    long errors = issues.stream().filter(i -> i.getSeverity() == ValidationSeverity.ERROR).count();
    long warnings = issues.stream().filter(i -> i.getSeverity() == ValidationSeverity.WARNING).count();

    if (errors > 0) {
      statusBarLabel.setForeground(FieldHighlighter.WARNING_COLOR);
      statusBarLabel.setText(FieldHighlighter.symbol(ValidationSeverity.ERROR) + " " + errors
                                 + " error(s), " + warnings + " warning(s) — fix before saving");
    } else if (warnings > 0) {
      statusBarLabel.setForeground(FieldHighlighter.WARNING_COLOR);
      statusBarLabel.setText(FieldHighlighter.symbol(ValidationSeverity.WARNING) + " " + warnings
                                 + " warning(s) — can be saved");
    } else {
      statusBarLabel.setForeground(OK_COLOR);
      statusBarLabel.setText(FieldHighlighter.symbol(ValidationSeverity.OK) + " All good, ready to save");
    }

    statusBarLabel.setToolTipText(buildIssuesTooltip(issues));
    saveButton.setEnabled(errors == 0);
  }

  /**
   * Собирает ВСЕ причины ошибок/warning-ов во всплывающую подсказку статус-бара
   * (сначала ошибки, затем предупреждения), чтобы при наведении мышью было
   * сразу видно, что именно не так и в какой секции.
   */
  private String buildIssuesTooltip(List<ValidationIssue> issues) {
    List<ValidationIssue> errors = issues.stream()
        .filter(i -> i.getSeverity() == ValidationSeverity.ERROR).toList();
    List<ValidationIssue> warnings = issues.stream()
        .filter(i -> i.getSeverity() == ValidationSeverity.WARNING).toList();

    if (errors.isEmpty() && warnings.isEmpty()) {
      return null;
    }

    StringBuilder sb = new StringBuilder("<html>");
    for (ValidationIssue issue : errors) {
      appendIssueLine(sb, FieldHighlighter.symbol(ValidationSeverity.ERROR), issue);
    }
    for (ValidationIssue issue : warnings) {
      appendIssueLine(sb, FieldHighlighter.symbol(ValidationSeverity.WARNING), issue);
    }
    sb.append("</html>");
    return sb.toString();
  }

  private void appendIssueLine(StringBuilder sb, String symbol, ValidationIssue issue) {
    sb.append(symbol).append(" <b>").append(escapeHtml(issue.getSection().getTitle())).append("</b>: ")
        .append(escapeHtml(issue.getMessage())).append("<br>");
  }

  private String escapeHtml(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  public JButton getSaveButton() {
    return saveButton;
  }

  public JButton getCancelButton() {
    return cancelButton;
  }

  public ProfileSectionPanel getProfilePanel() {
    return profilePanel;
  }

  public TaskSectionPanel getTaskPanel() {
    return taskPanel;
  }

  public ConnectionSectionPanel getConnectionPanel() {
    return connectionPanel;
  }

  public QueriesSectionPanel getQueriesPanel() {
    return queriesPanel;
  }
}