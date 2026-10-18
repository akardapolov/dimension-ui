package ru.dimension.ui.view.panel.template;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jdesktop.swingx.JXTextArea;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.FieldHighlighter;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.validation.ValidationIssue;
import ru.dimension.ui.view.table.icon.VectorIcons;

/**
 * Отдельное окно детализации запроса шаблона.
 * <p>
 * Сверху — таблица из ОДНОЙ строки с колонками "Name" (редактируемая,
 * "вживую" обновляет ячейку в основной таблице Queries и подсвечивается при
 * дубликате) и "Gather data mode" (read-only, рисуется цветным бейджем —
 * как для наглядности/визуальной метки, так и для гарантированной
 * читаемости текста независимо от текущей темы). Снизу — описание и текст
 * запроса, как и раньше.
 * <p>
 * Подсветка дубликата имени приходит снаружи — после каждого пересчёта
 * общей валидации — через {@link #setNameIssue(ValidationIssue)}.
 */
public class QueryDetailDialog extends JDialog {

  /** Тот же цвет, что используется для иконки Query в TTTable-колонках (ModelIconProviders). */
  private static final Color QUERY_ICON_COLOR = new Color(0xEF, 0x44, 0x44);

  private final DefaultTableModel nameModeModel;
  private final JTable nameModeTable;

  private final JXTextArea descriptionArea;
  private final RSyntaxTextArea textArea;
  private final JButton closeButton;

  /** true — идёт программная подстановка значений (open/sync), колбэки наружу не дёргаем. */
  private boolean loading = false;
  private ValidationIssue nameIssue;

  private Consumer<String> onNameChange;
  private Consumer<String> onDescriptionChange;
  private Consumer<String> onTextChange;

  public QueryDetailDialog(JDialog owner) {
    super(owner, "Edit query", owner == null ? ModalityType.MODELESS : ModalityType.APPLICATION_MODAL);
    setLayout(new BorderLayout(6, 6));

    this.nameModeModel = new DefaultTableModel(new Object[]{"Name", "Gather data mode"}, 1) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return column == 0;
      }
    };
    this.nameModeTable = new JTable(nameModeModel);
    GUIHelper.applyStandardTableStyle(nameModeTable);
    this.nameModeTable.setRowHeight(26);
    this.nameModeTable.setCellSelectionEnabled(false);
    this.nameModeTable.setRowSelectionAllowed(false);

    JTextField nameEditorField = new JTextField();
    nameModeTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(nameEditorField));
    nameModeTable.getColumnModel().getColumn(0).setCellRenderer(new NameCellRenderer());
    nameModeTable.getColumnModel().getColumn(1).setCellRenderer(new GatherModeBadgeRenderer());

    // Имя коммитится в модель по стандартному для JTable правилу (Enter / потеря
    // фокуса ячейки) — так же, как в основной таблице Queries. Это намеренно:
    // живой per-keystroke коммит мог бы триггерить авто-резолв дубликата имени
    // прямо во время набора текста и перетирать то, что печатает пользователь.
    nameModeModel.addTableModelListener(e -> {
      if (loading) {
        return;
      }
      if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 0) {
        String newName = String.valueOf(nameModeModel.getValueAt(0, 0));
        updateTitle(newName);
        if (onNameChange != null) {
          onNameChange.accept(newName);
        }
      }
    });

    JPanel tablePanel = new JPanel(new BorderLayout());
    tablePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
    tablePanel.add(nameModeTable.getTableHeader(), BorderLayout.NORTH);
    tablePanel.add(nameModeTable, BorderLayout.CENTER);

    this.descriptionArea = new JXTextArea();
    this.descriptionArea.setLineWrap(true);
    this.descriptionArea.setWrapStyleWord(true);
    this.descriptionArea.setRows(3);

    this.textArea = new RSyntaxTextArea();
    this.textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
    this.textArea.setCodeFoldingEnabled(true);

    JPanel detailContent = new JPanel();
    detailContent.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
    PainlessGridBag gbl = new PainlessGridBag(detailContent, PGHelper.getPGConfig(0), false);
    gbl.row().cell(new JLabel("Description")).fillX();
    gbl.row().cellXRemainder(new JScrollPane(descriptionArea)).fillX();
    gbl.row().cell(new JLabel("Text")).fillX();
    gbl.row().cellXYRemainder(new JScrollPane(textArea)).fillXY();
    gbl.done();

    JPanel center = new JPanel(new BorderLayout(0, 0));
    center.add(tablePanel, BorderLayout.NORTH);
    center.add(detailContent, BorderLayout.CENTER);

    this.closeButton = new JButton("Close");
    closeButton.addActionListener(e -> setVisible(false));
    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
    bottom.add(closeButton);

    add(center, BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);

    descriptionArea.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { fireDescriptionChange(); }
      @Override public void removeUpdate(DocumentEvent e) { fireDescriptionChange(); }
      @Override public void changedUpdate(DocumentEvent e) { fireDescriptionChange(); }
    });
    textArea.getDocument().addDocumentListener(new DocumentListener() {
      @Override public void insertUpdate(DocumentEvent e) { fireTextChange(); }
      @Override public void removeUpdate(DocumentEvent e) { fireTextChange(); }
      @Override public void changedUpdate(DocumentEvent e) { fireTextChange(); }
    });

    setSize(new Dimension(640, 560));
    setLocationRelativeTo(owner);
  }

  private void fireDescriptionChange() {
    if (!loading && onDescriptionChange != null) {
      onDescriptionChange.accept(descriptionArea.getText());
    }
  }

  private void fireTextChange() {
    if (!loading && onTextChange != null) {
      onTextChange.accept(textArea.getText());
    }
  }

  /** Открыть диалог с данными конкретного запроса. */
  public void openFor(String name, String gatherDataMode, String description, String text,
                      Consumer<String> onNameChange,
                      Consumer<String> onDescriptionChange,
                      Consumer<String> onTextChange) {
    this.onNameChange = onNameChange;
    this.onDescriptionChange = onDescriptionChange;
    this.onTextChange = onTextChange;

    loading = true;
    try {
      nameModeModel.setValueAt(name == null ? "" : name, 0, 0);
      nameModeModel.setValueAt(gatherDataMode == null ? "" : gatherDataMode, 0, 1);
      descriptionArea.setText(description == null ? "" : description);
      textArea.setText(text == null ? "" : text);
      textArea.setCaretPosition(0);
    } finally {
      loading = false;
    }

    updateTitle(name);
    setVisible(true);
    SwingUtilities.invokeLater(this::toFront);
  }

  /** Программно обновить имя (если его поменяли прямо в таблице, минуя этот диалог). */
  public void setNameSilently(String name) {
    loading = true;
    try {
      nameModeModel.setValueAt(name == null ? "" : name, 0, 0);
    } finally {
      loading = false;
    }
    updateTitle(name);
  }

  private void updateTitle(String name) {
    setTitle("Edit query — " + (name == null || name.isBlank() ? "(unnamed)" : name));
  }

  /** Подсветка дубликата имени — подсказка приходит снаружи, из результата общей валидации. */
  public void setNameIssue(ValidationIssue issue) {
    this.nameIssue = issue;
    nameModeTable.repaint();
  }

  private class NameCellRenderer extends DefaultTableCellRenderer {

    NameCellRenderer() {
      // Визуальная метка строки — та же иконка, что и у запросов в превью шаблона.
      setIcon(new VectorIcons.Query(QUERY_ICON_COLOR));
      setIconTextGap(6);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (nameIssue != null) {
        // Красный цвет не используется нигде — всегда жёлтая подсветка.
        setForeground(FieldHighlighter.WARNING_COLOR);
        setToolTipText(nameIssue.getMessage());
      } else {
        // Цвет текста из текущей темы — раньше здесь был захардкожен Color.BLACK,
        // из-за чего на тёмной теме текст сливался с фоном.
        setForeground(GUIHelper.getTableTextColor());
        setToolTipText(null);
      }
      return c;
    }
  }

  /**
   * Read-only колонка "Gather data mode" рисуется цветным "бейджем" — это
   * решает сразу две задачи: гарантированная видимость текста независимо от
   * темы (белый текст на цветном фоне — всегда контрастно) и приятная
   * визуальная метка строки, а не просто серый текст.
   */
  private static class GatherModeBadgeRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      JLabel label = (JLabel) super.getTableCellRendererComponent(
          table, value, isSelected, hasFocus, row, column);
      String text = value == null ? "" : value.toString();
      label.setText(text.isBlank() ? "—" : text);
      label.setHorizontalAlignment(SwingConstants.CENTER);
      label.setOpaque(true);
      label.setIcon(null);
      label.setForeground(Color.WHITE);
      label.setBackground(badgeColor(text));
      label.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 10));
      label.setToolTipText("Gather data mode — set from the source query, read-only");
      return label;
    }

    private Color badgeColor(String text) {
      if (text == null || text.isBlank()) {
        return Color.GRAY;
      }
      int hash = Math.abs(text.hashCode());
      float hue = (hash % 360) / 360f;
      return Color.getHSBColor(hue, 0.5f, 0.6f);
    }
  }
}