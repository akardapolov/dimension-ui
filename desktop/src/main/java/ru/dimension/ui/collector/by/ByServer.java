package ru.dimension.ui.collector.by;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class ByServer implements ByTarget {

  private final ProfileTaskQueryKey profileTaskQueryKey;
  private final QueryInfo queryInfo;
  private final TableInfo tableInfo;
  private final Connection connection;

  private final SqlQueryState sqlQueryState;

  public ByServer(ProfileTaskQueryKey profileTaskQueryKey,
                  QueryInfo queryInfo,
                  TableInfo tableInfo,
                  Connection connection,
                  SqlQueryState sqlQueryState) {
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.queryInfo = queryInfo;
    this.tableInfo = tableInfo;
    this.connection = connection;
    this.sqlQueryState = sqlQueryState;
  }

  @Override
  public PreparedStatement getPreparedStatement(long begin, long end) throws SQLException {

    PreparedStatement ps;

    String columnTimestamp = getColumnTimestamp(tableInfo);

    String sqlText = queryInfo.getText()
        + " WHERE " + columnTimestamp + " BETWEEN ? AND ? "
        + " ORDER BY " + getColumnTimestamp(tableInfo);

    ps = connection.prepareStatement(sqlText);

    ps.setTimestamp(1, new Timestamp(begin));
    ps.setTimestamp(2, new Timestamp(end));

    log.info("By server: " + profileTaskQueryKey + " begin: " + toLocalDateTime(begin) + " end: " + toLocalDateTime(end));

    return ps;
  }

  private LocalDateTime toLocalDateTime(long ofEpochMilli) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(ofEpochMilli), TimeZone.getDefault().toZoneId());
  }
}
