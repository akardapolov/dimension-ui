package ru.dimension.ui.view.handler;

import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;

public interface CommonViewHandler {

  default void fillConfigMetadata(TableInfo tableInfo,
                                  JXTableCase configMetadataCase) {
    Object[][] rowData = tableInfo.getCProfiles().stream()
        .filter(f -> !f.getCsType().isTimeStamp())
        .map(cProfile -> {
          boolean dimensionVal = tableInfo.getDimensionColumnList() != null &&
              tableInfo.getDimensionColumnList().stream().anyMatch(f -> f.equalsIgnoreCase(cProfile.getColName()));

          return new Object[]{
              cProfile.getColId(),
              cProfile.getColIdSql(),
              cProfile.getColName(),
              cProfile.getColDbTypeName(),
              cProfile.getCsType().getSType(),
              cProfile.getCsType().getCType(),
              dimensionVal
          };
        })
        .toArray(Object[][]::new);

    configMetadataCase.getDefaultTableModel().setRowCount(0);

    for (Object[] row : rowData) {
      configMetadataCase.getDefaultTableModel().addRow(row);
    }
  }
}
