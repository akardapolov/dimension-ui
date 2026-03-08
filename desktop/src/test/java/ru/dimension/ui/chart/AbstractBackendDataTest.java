package ru.dimension.ui.chart;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.dimension.db.DBase;
import ru.dimension.db.config.DBaseConfig;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.TableNameEmptyException;
import ru.dimension.db.metadata.DataType;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.db.model.profile.cstype.CSType;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.db.model.profile.cstype.SType;
import ru.dimension.db.model.profile.table.BType;
import ru.dimension.db.model.profile.table.IType;
import ru.dimension.db.model.profile.table.TType;
import ru.dimension.ui.HandlerMock;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.db.DBType;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.view.RangeRealTime;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.impl.SqlQueryStateImpl;
import ru.dimension.ui.warehouse.backend.BerkleyDB;

@Log4j2
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class AbstractBackendDataTest extends HandlerMock {

  public static long PULL_TIMEOUT_CLIENT = 1;

  @TempDir
  static File databaseDir;

  protected BerkleyDB berkleyDB;
  protected DBaseConfig dBaseConfig;
  protected DBase dBase;
  protected DStore dStore;
  protected SqlQueryState sqlQueryState;

  @BeforeAll
  public void initialize() {
    initialize(databaseDir);
  }

  public void initialize(File directory) {
    initializeBackend(directory);
    initializeState();
  }

  private void initializeBackend(File directory) {
    String path = directory.getAbsolutePath();
    berkleyDB = new BerkleyDB(path);
    dBaseConfig = new DBaseConfig().setConfigDirectory(path);
    dBase = new DBase(dBaseConfig, berkleyDB.getStore());
    dStore = dBase.getDStore();
  }

  protected void initializeState() {
    sqlQueryState = new SqlQueryStateImpl();
  }

  protected ChartConfig buildChartConfig(TProfile tProfile, GatherDataMode gatherDataMode) {
    ChartConfig config = new ChartConfig();

    ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(1, 1, 1);
    CProfile cProfile = tProfile.getCProfiles().stream()
        .filter(f -> f.getColName().equals(Column.VALUE_ENUM.name()))
        .findAny()
        .orElseThrow();
    ChartKey chartKey = new ChartKey(profileTaskQueryKey, cProfile);
    TableInfo tableInfo = new TableInfo(tProfile);

    ChartInfo chartInfo = new ChartInfo();
    chartInfo.setRangeRealtime(RangeRealTime.TEN_MIN);
    chartInfo.setPullTimeoutClient((int) PULL_TIMEOUT_CLIENT);

    QueryInfo queryInfo = new QueryInfo();
    queryInfo.setId(tProfile.getTableName().hashCode());
    queryInfo.setName(tProfile.getTableName());
    queryInfo.setGatherDataMode(gatherDataMode);
    queryInfo.setDbType(DBType.POSTGRES);

    Metric metric = new Metric(tableInfo, cProfile);

    config.setChartKey(chartKey);
    config.setTitle("");
    config.setXAxisLabel(metric.getYAxis().getColName());
    config.setYAxisLabel("Value");
    config.setMetric(metric);
    config.setChartInfo(chartInfo);
    config.setQueryInfo(queryInfo);

    return config;
  }

  public TProfile getTProfile(String tableName) {
    SProfile sProfile = new SProfile();
    sProfile.setTableName(tableName);
    sProfile.setTableType(TType.TIME_SERIES);
    sProfile.setIndexType(IType.LOCAL);
    sProfile.setBackendType(BType.BERKLEYDB);
    sProfile.setCompression(true);

    Map<String, CSType> csTypeMap = new LinkedHashMap<>();

    csTypeMap.put(Column.DT.name(),
                  CSType.builder().isTimeStamp(true).sType(SType.RAW).cType(CType.LONG).dType(DataType.LONG).build());
    csTypeMap.put(Column.VALUE_RAW.name(),
                  CSType.builder().isTimeStamp(false).sType(SType.RAW).cType(CType.LONG).dType(DataType.LONG).build());
    csTypeMap.put(Column.VALUE_ENUM.name(),
                  CSType.builder().isTimeStamp(false).sType(SType.RAW).cType(CType.STRING).dType(DataType.STRING).build());
    csTypeMap.put(Column.VALUE_HISTOGRAM.name(),
                  CSType.builder().isTimeStamp(false).sType(SType.RAW).cType(CType.LONG).dType(DataType.LONG).build());

    sProfile.setCsTypeMap(csTypeMap);

    try {
      return dStore.loadDirectTableMetadata(sProfile);
    } catch (TableNameEmptyException e) {
      throw new RuntimeException(e);
    }
  }

  public enum Column {
    DT,
    VALUE_RAW,
    VALUE_ENUM,
    VALUE_HISTOGRAM
  }

  @AfterAll
  public void closeDb() {
    if (berkleyDB != null) {
      berkleyDB.closeDatabase();
    }
  }
}