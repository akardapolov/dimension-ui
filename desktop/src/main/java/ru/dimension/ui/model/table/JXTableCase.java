package ru.dimension.ui.model.table;

import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jdesktop.swingx.JXTable;

@Data
@RequiredArgsConstructor
public class JXTableCase {

  @NonNull
  private JXTable jxTable;
  @NonNull
  private DefaultTableModel defaultTableModel;
  @NonNull
  private JScrollPane jScrollPane;

  @Setter
  private boolean isBlockRunAction;

  public void removeAllElements() {
    defaultTableModel.getDataVector().removeAllElements();
    defaultTableModel.fireTableDataChanged();
  }
}
