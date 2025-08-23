package ru.dimension.ui.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.DBase;
import ru.dimension.db.config.DBaseConfig;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.GanttColumnNotSupportedException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.exception.TableNameEmptyException;
import ru.dimension.db.model.GroupFunction;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.TProfile;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;
import ru.dimension.ui.collector.collect.prometheus.ExporterParser;
import ru.dimension.ui.warehouse.backend.BerkleyDB;
import ru.dimension.ui.http.HttpServerLocal;

@Log4j2
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractDirectTest {

  @TempDir
  static File databaseDir;
  protected BerkleyDB berkleyDB;

  protected DBaseConfig fBaseConfig;
  protected DBase fBase;
  protected DStore dStore;

  protected String tableNamePrometheus = "direct_table_prometheus_test";
  private TProfile tProfile;
  protected List<CProfile> cProfiles;

  protected HttpServerLocal httpServerLocal;
  protected ExporterParser parser;
  protected String response;

  @BeforeAll
  public void initBackendAndLoad() throws IOException {
    berkleyDB = new BerkleyDB(databaseDir.getAbsolutePath());

    fBaseConfig = new DBaseConfig().setConfigDirectory(databaseDir.getAbsolutePath());
    fBase = new DBase(fBaseConfig, berkleyDB.getStore());
    dStore = fBase.getDStore();

    response = getTestData("response", "spring_boot_prometheus.json");
    parser = new ExporterParser();
  }

  protected void startHttpServerLocal() {
    httpServerLocal = new HttpServerLocal(7777, "/actuator/prometheus", response);
  }

  protected void loadMetadataForTable(SProfile sProfile) {
    dStore = fBase.getDStore();

    try {
      tProfile = loadTableMetadata(sProfile);
      cProfiles = tProfile.getCProfiles();
    } catch (TableNameEmptyException e) {
      throw new RuntimeException(e);
    }
  }

  private TProfile loadTableMetadata(SProfile sProfile) throws TableNameEmptyException {
    return dStore.loadDirectTableMetadata(sProfile);
  }

  protected List<GanttColumnCount> getDataGanttColumn(String firstColName,
                                                 String secondColName,
                                                 int begin,
                                                 int end)
      throws BeginEndWrongOrderException, GanttColumnNotSupportedException, SqlColMetadataException {
    CProfile firstLevelGroupBy = cProfiles.stream()
        .filter(k -> k.getColName().equalsIgnoreCase(firstColName))
        .findAny().get();
    CProfile secondLevelGroupBy = cProfiles.stream()
        .filter(k -> k.getColName().equalsIgnoreCase(secondColName))
        .findAny().get();

    return getListGanttColumnTwoLevelGrouping(dStore, tProfile, firstLevelGroupBy, secondLevelGroupBy, begin, end);
  }

  public List<GanttColumnCount> getListGanttColumnTwoLevelGrouping(DStore dStore,
                                                              TProfile tProfile,
                                                              CProfile firstLevelGroupBy,
                                                              CProfile secondLevelGroupBy,
                                                              long begin,
                                                              long end)
      throws BeginEndWrongOrderException, GanttColumnNotSupportedException, SqlColMetadataException {
    return dStore.getGanttCount(tProfile.getTableName(), firstLevelGroupBy, secondLevelGroupBy, null, begin, end);
  }

  protected void compareKeySetForMapDataType(Map<String, Integer> expectedMap,
                                             List<StackedColumn> listMapActual) {
    assertEquals(expectedMap.keySet(),
                 listMapActual.stream().filter(f -> f.getKeyCount()
                         .keySet().equals(expectedMap.keySet()))
                     .findAny()
                     .orElseThrow()
                     .getKeyCount()
                     .keySet());
  }

  public List<StackedColumn> getListStackedDataBySqlCol(DStore dStore,
                                                        TProfile tProfile,
                                                        List<CProfile> cProfiles,
                                                        String colName,
                                                        int begin,
                                                        int end)
      throws BeginEndWrongOrderException, SqlColMetadataException {
    return dStore.getStacked(tProfile.getTableName(), cProfiles.stream()
        .filter(k -> k.getColName().equalsIgnoreCase(colName)).findAny().orElseThrow(), GroupFunction.COUNT, null, begin, end);
  }

  public Object findListStackedKey(List<StackedColumn> list,
                                   String filter) {
    for (StackedColumn stackedColumn : list) {
      if (stackedColumn.getKeyCount().containsKey(filter)) {
        return stackedColumn.getKeyCount().entrySet()
            .stream()
            .filter((k) -> k.getKey().equals(filter)).findAny().orElseThrow().getKey();
      }
    }
    return null;
  }

  public Object findListStackedValue(List<StackedColumn> list,
                                     String filter) {
    for (StackedColumn stackedColumn : list) {
      if (stackedColumn.getKeyCount().containsKey(filter)) {
        return stackedColumn.getKeyCount().entrySet()
            .stream()
            .filter((k) -> k.getKey().equals(filter)).findAny().orElseThrow().getValue();
      }
    }
    return null;
  }

  public List<StackedColumn> getDataStackedColumn(String colName,
                                                  int begin,
                                                  int end)
      throws BeginEndWrongOrderException, SqlColMetadataException {
    return getListStackedDataBySqlCol(dStore, tProfile, cProfiles, colName, begin, end);
  }

  public List<List<Object>> getRawDataAll(long begin,
                                          long end) {
    return dStore.getRawDataAll(tProfile.getTableName(), begin, end);
  }

  protected String getTestData(String dirName,
                               String fileName) throws IOException {
    return Files.readString(Paths.get("src", "test", "resources", dirName, fileName));
  }

  @AfterAll
  public void closeDb() {
    berkleyDB.closeDatabase();
  }
}
