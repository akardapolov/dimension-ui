package ru.dimension.ui.collector.by;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.QueryInfo;

@Log4j2
public class ByClient implements ByTarget {

  private final ProfileTaskQueryKey profileTaskQueryKey;
  private final QueryInfo queryInfo;
  private final Connection connection;

  public ByClient(ProfileTaskQueryKey profileTaskQueryKey,
                  QueryInfo queryInfo,
                  Connection connection) {
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.queryInfo = queryInfo;
    this.connection = connection;
  }

  @Override
  public PreparedStatement getPreparedStatement(long begin, long end) throws SQLException {
    return connection.prepareStatement(queryInfo.getText());
  }
}
