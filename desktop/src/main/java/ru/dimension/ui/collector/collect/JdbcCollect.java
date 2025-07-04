package ru.dimension.ui.collector.collect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.collector.JdbcLoader;
import ru.dimension.ui.collector.by.ByClient;
import ru.dimension.ui.collector.by.ByServer;
import ru.dimension.ui.collector.by.ByTarget;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class JdbcCollect extends AbstractCollect implements JdbcLoader {

  private final ByTarget byTarget;

  private final Connection connection;

  private final ProfileTaskQueryKey profileTaskQueryKey;
  private final TaskInfo taskInfo;
  private final QueryInfo queryInfo;
  private final TableInfo tableInfo;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  public JdbcCollect(ByTarget byTarget,
                     Connection connection,
                     ProfileTaskQueryKey profileTaskQueryKey,
                     TaskInfo taskInfo,
                     QueryInfo queryInfo,
                     TableInfo tableInfo,
                     SqlQueryState sqlQueryState,
                     DStore dStore) {
    this.byTarget = byTarget;
    this.connection = connection;
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.taskInfo = taskInfo;
    this.queryInfo = queryInfo;
    this.tableInfo = tableInfo;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;
  }

  @Override
  public void collect() {
    long begin = sqlQueryState.getLastTimestamp(profileTaskQueryKey);
    long end = getSysdate(queryInfo.getDbType().getQuery(), connection, log);

    try {

      PreparedStatement ps = byTarget.getPreparedStatement(begin, end);
      ps.setFetchSize(resultSetFetchSize);

      ResultSet r = ps.executeQuery();

      long lastTimeStampDB = dStore.putDataJdbc(tableInfo.getTableName(), r);

      if (byTarget instanceof ByServer) {
        sqlQueryState.setLastTimestamp(profileTaskQueryKey, end);
      } else if (byTarget instanceof ByClient) {
        if (lastTimeStampDB != -1) {
          sqlQueryState.setLastTimestamp(profileTaskQueryKey, lastTimeStampDB);
        } else {
          sqlQueryState.setLastTimestamp(profileTaskQueryKey, end);
        }
      }

      r.close();
      ps.close();
    } catch (Exception e) {
      sqlQueryState.setLastTimestamp(profileTaskQueryKey, end);

      log.catching(e);
    }
  }

  @Override
  public String getProtocol() {
    return CollectorConstants.PROTOCOL_JDBC;
  }
}
