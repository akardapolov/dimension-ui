package ru.dimension.ui.component.module.analyze.handler;

import java.util.List;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.TimeSeriesAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.popup.PopupPanel;

public class TablePopupCellEditorHandler implements CellEditorListener {

  private final PopupPanel popupPanel;
  private final List<TimeSeriesAlgorithm<?>> listAlgorithm;

  public TablePopupCellEditorHandler(PopupPanel popupPanel,
                                     List<TimeSeriesAlgorithm<?>> listAlgorithm) {
    this.popupPanel = popupPanel;
    this.listAlgorithm = listAlgorithm;
  }

  @Override
  public void editingStopped(ChangeEvent e) {
    TableCellEditor editor = (TableCellEditor) e.getSource();
    int row = popupPanel.getTable().getJxTable().getSelectedRow();
    String value = (String) editor.getCellEditorValue();

    listAlgorithm.stream()
        .filter(algorithm -> algorithm.getName().equals(popupPanel.getTextField().getText()))
        .findFirst()
        .ifPresent(algorithm -> {
          String selectedKey = popupPanel.getTable().getJxTable().getStringAt(row, 0);
          if (algorithm.getParameters().containsKey(selectedKey)) {
            algorithm.getParameters().put(selectedKey, value);
          }
        });
  }

  @Override
  public void editingCanceled(ChangeEvent e) {
  }
}

