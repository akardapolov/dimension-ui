package ru.dimension.ui.warehouse;

import com.sleepycat.je.DatabaseException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.DBase;
import ru.dimension.db.config.DBaseConfig;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.EnumByteExceedException;
import ru.dimension.db.exception.GanttColumnNotSupportedException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.exception.TableNameEmptyException;
import ru.dimension.db.model.GroupFunction;
import ru.dimension.db.model.OrderBy;
import ru.dimension.db.model.filter.CompositeFilter;
import ru.dimension.db.model.output.BlockKeyTail;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.output.GanttColumnSum;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.db.sql.BatchResultSet;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.warehouse.backend.BerkleyDB;

@Log4j2
@Singleton
public class LocalDB implements DStore {

  private final FilesHelper filesHelper;
  private final DBaseConfig DBaseConfig;
  private final DBase DBase;
  private final DStore DStore;
  private final BerkleyDB berkleyDB;

  @Inject
  public LocalDB(FilesHelper filesHelper) {
    this.filesHelper = filesHelper;
    DBaseConfig = new DBaseConfig().setConfigDirectory(filesHelper.getDatabaseDir());
    berkleyDB = new BerkleyDB(filesHelper.getDatabaseDir());
    DBase = new DBase(DBaseConfig, berkleyDB.getStore());
    DStore = DBase.getDStore();
  }

  @Override
  public TProfile getTProfile(String tableName) {
    try {
      return DStore.getTProfile(tableName);
    } catch (TableNameEmptyException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public TProfile loadDirectTableMetadata(SProfile sProfile) {
    try {
      return DStore.loadDirectTableMetadata(sProfile);
    } catch (TableNameEmptyException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public TProfile loadJdbcTableMetadata(Connection connection,
                                        String select,
                                        SProfile sProfile) {
    try {
      return DStore.loadJdbcTableMetadata(connection, select, sProfile);
    } catch (SQLException | TableNameEmptyException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public TProfile loadJdbcTableMetadata(Connection connection,
                                        String sqlSchemaName,
                                        String sqlTableName,
                                        SProfile sProfile) {
    try {
      return DStore.loadJdbcTableMetadata(connection, sqlSchemaName, sqlTableName, sProfile);
    } catch (SQLException | TableNameEmptyException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setTimestampColumn(String tableName,
                                 String timestampColumnName) throws TableNameEmptyException {
    DStore.setTimestampColumn(tableName, timestampColumnName);
  }

  @Override
  public void putDataDirect(String tableName,
                            List<List<Object>> list) {
    try {
      DStore.putDataDirect(tableName, list);
    } catch (Exception e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public long putDataJdbc(String tableName,
                          ResultSet resultSet)
      throws SqlColMetadataException, EnumByteExceedException {
    return DStore.putDataJdbc(tableName, resultSet);
  }

  @Override
  public void putDataJdbcBatch(String tableName,
                               ResultSet resultSet,
                               Integer DBaseBatchSize)
      throws SqlColMetadataException, EnumByteExceedException {
    DStore.putDataJdbcBatch(tableName, resultSet, DBaseBatchSize);
  }

  @Override
  public List<BlockKeyTail> getBlockKeyTailList(String tableName,
                                                long begin,
                                                long end) throws SqlColMetadataException, BeginEndWrongOrderException {
    return DStore.getBlockKeyTailList(tableName, begin, end);
  }

  @Override
  public List<StackedColumn> getStacked(String tableName,
                                        CProfile cProfile,
                                        GroupFunction groupFunction,
                                        CompositeFilter compositeFilter,
                                        long begin,
                                        long end) throws SqlColMetadataException, BeginEndWrongOrderException {
    return DStore.getStacked(tableName, cProfile, groupFunction, compositeFilter, begin, end);
  }

  @Override
  public List<GanttColumnCount> getGanttCount(String tableName,
                                              CProfile firstGrpBy,
                                              CProfile secondGrpBy,
                                              CompositeFilter compositeFilter,
                                              long begin,
                                              long end)
      throws SqlColMetadataException, BeginEndWrongOrderException, GanttColumnNotSupportedException {
    return DStore.getGanttCount(tableName, firstGrpBy, secondGrpBy, compositeFilter, begin, end);
  }

  @Override
  public List<GanttColumnCount> getGanttCount(String tableName,
                                              CProfile firstGrpBy,
                                              CProfile secondGrpBy,
                                              CompositeFilter compositeFilter,
                                              int batchSize,
                                              long begin,
                                              long end)
      throws SqlColMetadataException, BeginEndWrongOrderException, GanttColumnNotSupportedException {
    return DStore.getGanttCount(tableName, firstGrpBy, secondGrpBy, compositeFilter, batchSize, begin, end);
  }

  @Override
  public List<GanttColumnSum> getGanttSum(String tableName,
                                          CProfile firstGrpBy,
                                          CProfile secondGrpBy,
                                          CompositeFilter compositeFilter,
                                          long begin,
                                          long end)
      throws SqlColMetadataException, BeginEndWrongOrderException, GanttColumnNotSupportedException {
    return DStore.getGanttSum(tableName, firstGrpBy, secondGrpBy, compositeFilter, begin, end);
  }

  @Override
  public List<String> getDistinct(String tableName,
                                  CProfile cProfile,
                                  OrderBy orderBy,
                                  CompositeFilter compositeFilter,
                                  int limit,
                                  long begin,
                                  long end) throws BeginEndWrongOrderException {
    return DStore.getDistinct(tableName, cProfile, orderBy, compositeFilter, limit, begin, end);
  }

  @Override
  public List<List<Object>> getRawDataByColumn(String tableName,
                                               CProfile cProfile,
                                               long begin,
                                               long end) {
    return DStore.getRawDataByColumn(tableName, cProfile, begin, end);
  }

  @Override
  public List<List<Object>> getRawDataAll(String tableName,
                                          long begin,
                                          long end) {
    return DStore.getRawDataAll(tableName, begin, end);
  }

  @Override
  public List<List<Object>> getRawDataAll(String tableName,
                                          CProfile cProfileFilter,
                                          String filter,
                                          long begin,
                                          long end) {
    return DStore.getRawDataAll(tableName, cProfileFilter, filter, begin, end);
  }

  @Override
  public BatchResultSet getBatchResultSet(String tableName,
                                          int fetchSize) {
    return DStore.getBatchResultSet(tableName, fetchSize);
  }

  @Override
  public BatchResultSet getBatchResultSet(String tableName,
                                          long begin,
                                          long end,
                                          int fetchSize) {
    return DStore.getBatchResultSet(tableName, begin, end, fetchSize);
  }

  @Override
  public long getFirst(String tableName,
                       long begin,
                       long end) {
    return DStore.getFirst(tableName, begin, end);
  }

  @Override
  public long getLast(String tableName,
                      long begin,
                      long end) {
    return DStore.getLast(tableName, begin, end);
  }

  @Override
  public void syncBackendDb() {
    try {
      if (this.berkleyDB.getStore() != null) {
        this.berkleyDB.getStore().sync();
      }
    } catch (IllegalStateException e) {
      log.error("Database is already closed: ", e);
    }
  }

  @Override
  public void closeBackendDb() {
    if (this.berkleyDB.getStore() != null) {
      try {
        this.berkleyDB.getStore().close();
      } catch (DatabaseException dbe) {
        log.error("Error closing store: {}", String.valueOf(dbe));
      }
    }
    if (this.berkleyDB.getEnv() != null) {
      try {
        this.berkleyDB.getEnv().close();
      } catch (DatabaseException dbe) {
        log.error("Error closing env: {}", String.valueOf(dbe));
      }
    }
  }
}