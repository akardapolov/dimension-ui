package ru.dimension.ui.view.panel.template;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import org.jdesktop.swingx.JXTitledSeparator;
import ru.dimension.ui.helper.FieldHighlighter;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.validation.TemplateDraft;
import ru.dimension.ui.model.validation.ValidationIssue;
import ru.dimension.ui.model.validation.ValidationSection;
import ru.dimension.ui.view.table.icon.VectorIcons;

/**
 * Таблица запросов шаблона. Вместо прежнего редактируемого столбца
 * "Description" — столбец "Edit" с кнопкой, открывающей отдельный JDialog
 * {@link QueryDetailDialog} с детализацией: имя (редактируемое, "вживую"
 * обновляет ячейку таблицы и подсвечивается при дубликате), режим сбора
 * данных (read-only), описание и текст запроса. Description/Text/GatherMode
 * не хранятся в видимых столбцах таблицы — они держатся во внутренних
 * списках этой панели и забираются наружу через {@link #applyTo(TemplateDraft)}
 * / {@link #getQueryText(int)}.
 */
public class QueriesSectionPanel extends javax.swing.JPanel {

  /** Тот же цвет, что используется для иконки Query в TTTable-колонках (ModelIconProviders). */
  private static final Color QUERY_ICON_COLOR = new Color(0xEF, 0x44, 0x44);

  private final JXTableCase tableCase;
  private final Map<Integer, ValidationIssue> rowIssues = new HashMap<>();

  private final List<String> descriptions = new ArrayList<>();
  private final List<String> texts = new ArrayList<>();
  private final List<String> gatherDataModes = new ArrayList<>();
  private final List<Integer> sourceQueryIds = new ArrayList<>();

  /** Создаётся лениво при первом клике на Edit — owner берём из реального окна-предка. */
  private QueryDetailDialog detailDialog;
  private Integer openRow = null;

  private boolean suppressEvents = false;

  public QueriesSectionPanel() {
    setLayout(new BorderLayout(4, 4));

    this.tableCase = GUIHelper.getEditJXTableCase(2, new String[]{"Name", "Edit"});
    this.tableCase.getJxTable().setSortable(false);
    this.tableCase.getJxTable().getColumnModel().getColumn(0).setCellRenderer(new StatusAwareRenderer());
    installEditButtonColumn();

    add(new JXTitledSeparator("Queries"), BorderLayout.NORTH);
    add(tableCase.getJScrollPane(), BorderLayout.CENTER);

    // Если в данный момент открыт детализирующий диалог для строки row и имя
    // поменяли ПРЯМО в таблице (не через диалог) — подтянуть новое имя в диалог.
    tableCase.getDefaultTableModel().addTableModelListener(e -> {
      if (suppressEvents || detailDialog == null) {
        return;
      }
      if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 0
          && e.getFirstRow() == e.getLastRow() && e.getFirstRow() >= 0) {
        int row = e.getFirstRow();
        if (openRow != null && openRow == row) {
          String newName = String.valueOf(tableCase.getDefaultTableModel().getValueAt(row, 0));
          detailDialog.setNameSilently(newName);
        }
      }
    });
  }

  private void installEditButtonColumn() {
    JTable table = tableCase.getJxTable();
    table.getColumnModel().getColumn(1).setCellRenderer((t, value, isSelected, hasFocus, row, column) -> {
      JButton button = new JButton("\u270E Edit");
      button.setMargin(new Insets(0, 6, 0, 6));
      return button;
    });
    table.getColumnModel().getColumn(1).setCellEditor(new EditButtonEditor());
    table.getColumnModel().getColumn(1).setPreferredWidth(80);
    table.getColumnModel().getColumn(1).setMaxWidth(100);
  }

  public void setOnChange(Runnable onChange) {
    tableCase.getDefaultTableModel().addTableModelListener(e -> onChange.run());
  }

  /**
   * @param queryTextBySourceId исходный текст запроса (из текущей конфигурации шаблона),
   *                            ключ — id исходного Query. Нужен, чтобы было что показать
   *                            в детализации запроса при первом открытии диалога.
   */
  public void loadFrom(TemplateDraft draft, Map<Integer, String> queryTextBySourceId) {
    DefaultTableModel model = tableCase.getDefaultTableModel();
    suppressEvents = true;
    try {
      model.getDataVector().removeAllElements();
      descriptions.clear();
      texts.clear();
      gatherDataModes.clear();
      sourceQueryIds.clear();
      for (TemplateDraft.QueryDraft q : draft.getQueries()) {
        model.addRow(new Object[]{q.getName(), "Edit"});
        descriptions.add(q.getDescription() == null ? "" : q.getDescription());
        texts.add(queryTextBySourceId == null
                      ? "" : queryTextBySourceId.getOrDefault(q.getSourceQueryId(), ""));
        gatherDataModes.add(q.getGatherDataMode() == null ? "" : q.getGatherDataMode());
        sourceQueryIds.add(q.getSourceQueryId());
      }
      model.fireTableDataChanged();
    } finally {
      suppressEvents = false;
    }
    openRow = null;
    if (detailDialog != null) {
      detailDialog.setVisible(false);
    }
  }

  public void applyTo(TemplateDraft draft) {
    DefaultTableModel model = tableCase.getDefaultTableModel();
    int rows = Math.min(model.getRowCount(), draft.getQueries().size());
    for (int i = 0; i < rows; i++) {
      draft.getQueries().get(i).setName(String.valueOf(model.getValueAt(i, 0)));
      draft.getQueries().get(i).setDescription(i < descriptions.size() ? descriptions.get(i) : "");
    }
  }

  /** Текущий (возможно отредактированный в детализации) текст запроса для указанной строки. */
  public String getQueryText(int row) {
    return row >= 0 && row < texts.size() ? texts.get(row) : "";
  }

  public void applyIssues(List<ValidationIssue> issues) {
    rowIssues.clear();
    for (ValidationIssue issue : issues) {
      if (issue.getSection() != ValidationSection.QUERIES) {
        continue;
      }
      String[] parts = issue.getFieldKey().split("\\.");
      if (parts.length == 3) {
        rowIssues.put(Integer.parseInt(parts[1]), issue);
      }
    }
    tableCase.getJxTable().repaint();
    if (openRow != null && detailDialog != null) {
      detailDialog.setNameIssue(rowIssues.get(openRow));
    }
  }

  private QueryDetailDialog getOrCreateDetailDialog() {
    if (detailDialog == null) {
      Window owner = SwingUtilities.getWindowAncestor(this);
      JDialog ownerDialog = owner instanceof JDialog ? (JDialog) owner : null;
      detailDialog = new QueryDetailDialog(ownerDialog);
    }
    return detailDialog;
  }

  private void openDetailDialog(int row) {
    openRow = row;
    String name = String.valueOf(tableCase.getDefaultTableModel().getValueAt(row, 0));
    String description = row < descriptions.size() ? descriptions.get(row) : "";
    String text = row < texts.size() ? texts.get(row) : "";
    String gatherDataMode = row < gatherDataModes.size() ? gatherDataModes.get(row) : "";

    QueryDetailDialog dialog = getOrCreateDetailDialog();
    dialog.setNameIssue(rowIssues.get(row));
    dialog.openFor(name, gatherDataMode, description, text,
                   newName -> setQueryNameFromDialog(row, newName),
                   newDescription -> {
                     if (row < descriptions.size()) {
                       descriptions.set(row, newDescription);
                     }
                     fireTableRowTouched(row);
                   },
                   newText -> {
                     if (row < texts.size()) {
                       texts.set(row, newText);
                     }
                     fireTableRowTouched(row);
                   });
  }

  /** Имя меняется из диалога детализации — применяем как обычное пользовательское редактирование ячейки. */
  private void setQueryNameFromDialog(int row, String newName) {
    DefaultTableModel model = tableCase.getDefaultTableModel();
    if (!newName.equals(String.valueOf(model.getValueAt(row, 0)))) {
      model.setValueAt(newName, row, 0);
    }
  }

  /**
   * Description/Text не хранятся в самой модели таблицы (нет видимых столбцов под них),
   * поэтому для срабатывания общего onChange (пересчёт валидации/preview) дёргаем
   * безобидное fireTableCellUpdated по служебному столбцу "Edit".
   */
  private void fireTableRowTouched(int row) {
    tableCase.getDefaultTableModel().fireTableCellUpdated(row, 1);
  }

  private class StatusAwareRenderer extends DefaultTableCellRenderer {

    StatusAwareRenderer() {
      // Визуальная метка строки — та же иконка, что и у запросов в превью шаблона.
      setIcon(new VectorIcons.Query(QUERY_ICON_COLOR));
      setIconTextGap(6);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      int modelRow = table.convertRowIndexToModel(row);
      ValidationIssue issue = rowIssues.get(modelRow);
      if (issue != null) {
        // Красный цвет не используется нигде — всегда жёлтая подсветка.
        setForeground(FieldHighlighter.WARNING_COLOR);
        setToolTipText(issue.getMessage());
      } else {
        // Цвет текста берём из текущей темы — раньше здесь был захардкожен
        // Color.BLACK, из-за чего на тёмной теме текст сливался с фоном.
        setForeground(GUIHelper.getTableTextColor());
        setToolTipText(null);
      }
      return c;
    }
  }

  private class EditButtonEditor extends AbstractCellEditor implements TableCellEditor {
    private final JButton button = new JButton("\u270E Edit");
    private int editingModelRow;

    EditButtonEditor() {
      button.setMargin(new Insets(0, 6, 0, 6));
      button.addActionListener(e -> {
        fireEditingStopped();
        openDetailDialog(editingModelRow);
      });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                 int row, int column) {
      editingModelRow = table.convertRowIndexToModel(row);
      return button;
    }

    @Override
    public Object getCellEditorValue() {
      return "Edit";
    }
  }

  /** Вызывается, когда пользователь вручную отредактировал ячейку Name прямо в таблице. */
  public void setOnQueryNameEdited(java.util.function.BiConsumer<Integer, String> handler) {
    tableCase.getDefaultTableModel().addTableModelListener(e -> {
      if (suppressEvents) {
        return;
      }
      // Программная загрузка (loadFrom) добавляет строки через addRow -> INSERT,
      // ручное редактирование ячейки -> UPDATE на конкретной строке/колонке.
      if (e.getType() == TableModelEvent.UPDATE
          && e.getColumn() == 0
          && e.getFirstRow() == e.getLastRow()
          && e.getFirstRow() >= 0) {
        int row = e.getFirstRow();
        String newName = String.valueOf(tableCase.getDefaultTableModel().getValueAt(row, 0));
        handler.accept(row, newName);
      }
    });
  }

  /** Программно подставить имя (авто-резолв дубликата) без повторного триггера. */
  public void setQueryNameSilently(int row, String name) {
    suppressEvents = true;
    try {
      tableCase.getDefaultTableModel().setValueAt(name, row, 0);
    } finally {
      suppressEvents = false;
    }
    tableCase.getJxTable().repaint();
    if (openRow != null && openRow == row && detailDialog != null) {
      detailDialog.setNameSilently(name);
    }
  }
}