package ru.dimension.ui.view.handler.core;

import java.util.Optional;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.model.table.JXTableCase;

public final class TTTableSelection {

  private TTTableSelection() {
  }

  public static <R> Optional<R> selectedItem(JXTableCase tableCase) {
    if (tableCase == null || tableCase.getJxTable() == null) {
      return Optional.empty();
    }

    JXTable table = tableCase.getJxTable();
    int viewRow = table.getSelectedRow();
    if (viewRow < 0) {
      return Optional.empty();
    }

    int modelRow = table.convertRowIndexToModel(viewRow);

    @SuppressWarnings("unchecked")
    TTTable<R, JXTable> tt = (TTTable<R, JXTable>) tableCase.getTypedTable();

    if (tt == null || tt.model() == null) {
      return Optional.empty();
    }

    if (modelRow < 0 || modelRow >= tt.model().getRowCount()) {
      return Optional.empty();
    }

    return Optional.ofNullable(tt.model().itemAt(modelRow));
  }
}