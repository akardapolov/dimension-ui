package ru.dimension.ui.component.module.model;

import java.util.List;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import ru.dimension.db.model.profile.CProfile;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.config.Metric;

public class ModelCellEditorListener<T> implements CellEditorListener {

  private final List<T> itemList;
  private final JXTable jxTable;
  private final ModelHandler<T> modelHandler;

  public ModelCellEditorListener(List<T> itemList,
                                 JXTable jxTable,
                                 ModelHandler<T> modelHandler) {
    this.itemList = itemList;
    this.jxTable = jxTable;
    this.modelHandler = modelHandler;
  }

  @Override
  public void editingStopped(ChangeEvent e) {
    TableCellEditor editor = (TableCellEditor) e.getSource();
    Boolean cellValue = (Boolean) editor.getCellEditorValue();
    int selectedRow = jxTable.getSelectedRow();
    String selectedValue = jxTable.getModel().getValueAt(selectedRow, ColumnNames.NAME.ordinal()).toString();

    itemList.stream()
        .filter(item -> getItemColumnName(item).equals(selectedValue))
        .forEach(item -> modelHandler.handle(item, cellValue));
  }

  private String getItemColumnName(T item) {
    return item instanceof CProfile ? ((CProfile) item).getColName() : ((Metric) item).getName();
  }

  @Override
  public void editingCanceled(ChangeEvent changeEvent) {
  }
}
