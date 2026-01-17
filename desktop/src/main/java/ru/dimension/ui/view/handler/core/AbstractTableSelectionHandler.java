package ru.dimension.ui.view.handler.core;

import jakarta.inject.Inject;
import java.util.Optional;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import ru.dimension.ui.model.table.JXTableCase;

public abstract class AbstractTableSelectionHandler<R> {

  private final JXTableCase tableCase;

  @Inject
  protected AbstractTableSelectionHandler(JXTableCase tableCase) {
    this.tableCase = tableCase;
  }

  public void bind() {
    if (this.tableCase != null && this.tableCase.getJxTable() != null) {
      this.tableCase.getJxTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (e.getValueIsAdjusting()) {
            return;
          }
          onSelection(TTTableSelection.<R>selectedItem(AbstractTableSelectionHandler.this.tableCase));
        }
      });
      onSelection(TTTableSelection.<R>selectedItem(this.tableCase));
    } else {
      onSelection(Optional.empty());
    }
  }

  protected final JXTableCase tableCase() {
    return tableCase;
  }

  protected abstract void onSelection(Optional<R> item);
}