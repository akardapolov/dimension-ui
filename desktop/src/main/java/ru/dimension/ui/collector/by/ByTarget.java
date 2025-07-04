package ru.dimension.ui.collector.by;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.model.info.TableInfo;

public interface ByTarget {

  PreparedStatement getPreparedStatement(long begin, long end) throws SQLException;

  default String getColumnTimestamp(TableInfo tableInfo) {
    return tableInfo.getCProfiles()
        .stream()
        .filter(f -> f.getCsType().isTimeStamp())
        .findAny()
        .orElseThrow(() -> new NotFoundException("Not found column timestamp: " + tableInfo.getTableName()))
        .getColName();
  }
}
