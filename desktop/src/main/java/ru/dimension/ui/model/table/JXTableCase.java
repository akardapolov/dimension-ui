package ru.dimension.ui.model.table;

import java.util.Collections;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;

@Data
@NoArgsConstructor
public class JXTableCase {

  private JXTable jxTable;
  private DefaultTableModel defaultTableModel;
  private JScrollPane jScrollPane;

  private TTTable<?, JXTable> ttTable;

  @Setter
  private boolean isBlockRunAction;

  public JXTableCase(JXTable jxTable, DefaultTableModel defaultTableModel, JScrollPane jScrollPane) {
    this.jxTable = jxTable;
    this.defaultTableModel = defaultTableModel;
    this.jScrollPane = jScrollPane;
  }

  public JXTableCase(TTTable<?, JXTable> ttTable) {
    this.ttTable = ttTable;
    this.jxTable = ttTable.table();
    this.jScrollPane = ttTable.scrollPane();
  }

  public void clearTable() {
    if (ttTable != null) {
      ttTable.setItems(Collections.emptyList());
    } else if (defaultTableModel != null) {
      defaultTableModel.getDataVector().removeAllElements();
      defaultTableModel.fireTableDataChanged();
    }
  }

  public void clearSelection() {
    if (!isBlockRunAction && jxTable != null) {
      jxTable.clearSelection();
    }
  }

  public void addRow(Object[] objects) {
    if (defaultTableModel != null) {
      defaultTableModel.addRow(objects);
    } else {
      throw new UnsupportedOperationException("addRow(Object[]) not supported for Typed Table. Use getTtTable().add().");
    }
  }

  public <T> TTTable<T, JXTable> getTypedTable() {
    return (TTTable<T, JXTable>) ttTable;
  }
}