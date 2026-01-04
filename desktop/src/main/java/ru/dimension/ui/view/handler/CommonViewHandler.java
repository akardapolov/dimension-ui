package ru.dimension.ui.view.handler;

import java.util.List;
import java.util.stream.Collectors;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.table.row.Rows.MetadataRow;

public interface CommonViewHandler {

  default void fillConfigMetadata(TableInfo tableInfo, JXTableCase configMetadataCase) {
    if (tableInfo != null && tableInfo.getCProfiles() != null) {
      TTTable<MetadataRow, JXTable> tt = configMetadataCase.getTypedTable();

      List<MetadataRow> rows = tableInfo.getCProfiles().stream()
          .filter(f -> !f.getCsType().isTimeStamp())
          .map(cProfile -> {
            boolean isDimension = tableInfo.getDimensionColumnList() != null &&
                tableInfo.getDimensionColumnList().contains(cProfile.getColName());
            return new MetadataRow(cProfile, isDimension);
          })
          .collect(Collectors.toList());

      tt.setItems(rows);
    }
  }
}
