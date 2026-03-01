package ru.dimension.ui.component.module.analyze.handler;

import java.util.List;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import ru.dimension.ui.component.module.analyze.timeseries.algorithm.TimeSeriesAlgorithm;
import ru.dimension.ui.component.module.analyze.timeseries.AlgorithmSettingsPanel;

public class TablePopupCellEditorHandler implements CellEditorListener {

  private final AlgorithmSettingsPanel algorithmSettingsPanel;
  private final List<TimeSeriesAlgorithm<?>> listAlgorithm;

  public TablePopupCellEditorHandler(AlgorithmSettingsPanel algorithmSettingsPanel,
                                     List<TimeSeriesAlgorithm<?>> listAlgorithm) {
    this.algorithmSettingsPanel = algorithmSettingsPanel;
    this.listAlgorithm = listAlgorithm;
  }

  @Override
  public void editingStopped(ChangeEvent e) {
    TableCellEditor editor = (TableCellEditor) e.getSource();
    int row = algorithmSettingsPanel.getTable().getJxTable().getSelectedRow();
    String value = (String) editor.getCellEditorValue();

    listAlgorithm.stream()
        .filter(algorithm -> algorithm.getName().equals(algorithmSettingsPanel.getTextField().getText()))
        .findFirst()
        .ifPresent(algorithm -> {
          String selectedKey = algorithmSettingsPanel.getTable().getJxTable().getStringAt(row, 0);
          if (algorithm.getParameters().containsKey(selectedKey)) {
            algorithm.getParameters().put(selectedKey, value);
          }
        });
  }

  @Override
  public void editingCanceled(ChangeEvent e) {
  }
}

