package ru.dimension.ui.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import ru.dimension.db.model.output.BlockKeyTail;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.ui.chart.model.TestDataSet;
import ru.dimension.ui.chart.model.TestRecord;
import ru.dimension.ui.chart.util.TestDataFactory;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.realtime.ClientRealtimeSCP;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.data.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.view.SeriesType;

@Log4j2
public class ClientRealtimeSCPDataTest extends AbstractBackendDataTest {

  private int tableCounter = 0;

  private TProfile createProfileAndLoad(TestDataSet dataSet) {
    String tableName = dataSet.getTableName() + "_" + (tableCounter++);
    TProfile tProfile = getTProfile(tableName);
    List<TestRecord> records = dataSet.getRecords();
    if (records != null && !records.isEmpty()) {
      TestDataFactory.loadRecordsIntoDStore(dStore, tProfile, records);
    }
    return tProfile;
  }

  private TProfile createProfileAndLoadInitial(TestDataSet dataSet) {
    String tableName = dataSet.getTableName() + "_" + (tableCounter++);
    TProfile tProfile = getTProfile(tableName);
    List<TestRecord> records = dataSet.getInitialRecords();
    if (records != null && !records.isEmpty()) {
      TestDataFactory.loadRecordsIntoDStore(dStore, tProfile, records);
    }
    return tProfile;
  }

  private void loadIncremental(TestDataSet dataSet, TProfile tProfile) {
    List<TestRecord> records = dataSet.getIncrementalRecords();
    if (records != null && !records.isEmpty()) {
      TestDataFactory.loadRecordsIntoDStore(dStore, tProfile, records);
    }
  }

  private ClientRealtimeSCP createSCP(TProfile tProfile) {
    ChartConfig config = buildChartConfig(tProfile, GatherDataMode.BY_CLIENT_JDBC);
    ProfileTaskQueryKey ptqKey = config.getChartKey().getProfileTaskQueryKey();
    return new ClientRealtimeSCP(sqlQueryState, dStore, config, ptqKey);
  }

  private ClientRealtimeSCP createSCPWithFilter(TProfile tProfile,
                                                Map<CProfile, LinkedHashSet<String>> filter) {
    ChartConfig config = buildChartConfig(tProfile, GatherDataMode.BY_CLIENT_JDBC);
    ProfileTaskQueryKey ptqKey = config.getChartKey().getProfileTaskQueryKey();
    return new ClientRealtimeSCP(sqlQueryState, dStore, config, ptqKey, filter);
  }

  private void assertEveryMeasurementHasFiveValues(List<TestRecord> records) {
    Map<Long, Long> counts = records.stream()
        .collect(Collectors.groupingBy(TestRecord::getDt, Collectors.counting()));

    assertFalse(counts.isEmpty());
    assertTrue(counts.values().stream().allMatch(count -> count == TestDataFactory.VALUES_PER_MEASUREMENT));
  }

  private void assertTenMinuteWindow(List<TestRecord> records) {
    List<Long> timestamps = TestDataFactory.getSortedDistinctTimestamps(records);
    assertEquals(TestDataFactory.MEASUREMENTS_PER_WINDOW, timestamps.size());
    assertEquals(TestDataFactory.WINDOW_MS, timestamps.getLast() - timestamps.getFirst());
    assertEveryMeasurementHasFiveValues(records);
  }

  private double getDatasetEndX(CategoryTableXYDatasetRealTime dataset) {
    if (dataset == null || dataset.getSeriesCount() == 0 || dataset.getItemCount() == 0) {
      return 0D;
    }
    return dataset.getEndXValue(0, dataset.getItemCount() - 1);
  }

  private void sleepMs(long ms) {
    if (ms <= 0) {
      return;
    }
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private String getFirstEnumValue(TestDataSet dataSet) {
    return TestDataFactory.getDistinctEnumValues(dataSet.getRecords()).stream()
        .findFirst()
        .orElseThrow();
  }

  private long[] findFirstGapBounds(List<TestRecord> records) {
    List<Long> timestamps = TestDataFactory.getSortedDistinctTimestamps(records);
    for (int i = 1; i < timestamps.size(); i++) {
      if (timestamps.get(i) - timestamps.get(i - 1) > TestDataFactory.INTERVAL_MS) {
        return new long[]{timestamps.get(i - 1), timestamps.get(i)};
      }
    }
    return new long[]{0L, 0L};
  }

  @Test
  public void normalDataInitializeTest() {
    TestDataSet dataSet = TestDataFactory.normalData();

    assertTenMinuteWindow(dataSet.getRecords());
    assertFalse(TestDataFactory.hasGaps(dataSet.getRecords(), TestDataFactory.INTERVAL_MS));

    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    assertNotNull(dataset);
    assertEquals(SeriesType.COMMON, scp.getSeriesType());
    assertTrue(dataset.getSeriesCount() > 0);
    assertTrue(dataset.getItemCount() > 0);
  }

  @Test
  public void emptyDataInitializeTest() {
    TestDataSet dataSet = TestDataFactory.emptyData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    assertNotNull(dataset);
    assertEquals(0, dataset.getItemCount());
    assertEquals(0, dataset.getSeriesCount());
  }

  @Test
  public void singleMeasurementInitializeTest() {
    TestDataSet dataSet = TestDataFactory.singleMeasurement();

    assertEquals(1, TestDataFactory.countDistinctTimestamps(dataSet.getRecords()));
    assertEveryMeasurementHasFiveValues(dataSet.getRecords());

    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    assertNotNull(dataset);
    assertTrue(dataset.getSeriesCount() >= 1);
  }

  @Test
  public void singleEnumInitializeTest() {
    TestDataSet dataSet = TestDataFactory.singleEnum();

    assertTenMinuteWindow(dataSet.getRecords());
    assertEquals(1, TestDataFactory.countDistinctEnumValues(dataSet.getRecords()));

    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    assertNotNull(dataset);
    assertEquals(SeriesType.COMMON, scp.getSeriesType());
    assertEquals(1, dataset.getSeriesCount());
    assertTrue(dataset.getItemCount() > 0);
  }

  @Test
  public void gapDataInitializeTest() {
    TestDataSet dataSet = TestDataFactory.gapData();

    assertEveryMeasurementHasFiveValues(dataSet.getRecords());
    assertTrue(TestDataFactory.hasGaps(dataSet.getRecords(), TestDataFactory.INTERVAL_MS));
    assertEquals(1, TestDataFactory.countGaps(dataSet.getRecords(), TestDataFactory.INTERVAL_MS));

    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    assertNotNull(dataset);
    assertTrue(dataset.getSeriesCount() > 0);
    assertTrue(dataset.getItemCount() > 0);
  }

  @Test
  public void multipleGapsInitializeTest() {
    TestDataSet dataSet = TestDataFactory.multipleGaps();

    assertEveryMeasurementHasFiveValues(dataSet.getRecords());
    assertEquals(2, TestDataFactory.countGaps(dataSet.getRecords(), TestDataFactory.INTERVAL_MS));

    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    assertNotNull(dataset);
    assertTrue(dataset.getSeriesCount() > 0);
    assertTrue(dataset.getItemCount() > 0);
  }

  @Test
  public void duplicateTimestampsInitializeTest() {
    TestDataSet dataSet = TestDataFactory.duplicateTimestamps();

    assertEquals(1, TestDataFactory.countDistinctTimestamps(dataSet.getRecords()));

    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    assertNotNull(dataset);
    assertTrue(dataset.getSeriesCount() > 0);
  }

  @Test
  public void incrementalLoadTest() {
    TestDataSet dataSet = TestDataFactory.incrementalLoad();

    assertTenMinuteWindow(dataSet.getInitialRecords());
    assertEveryMeasurementHasFiveValues(dataSet.getIncrementalRecords());

    TProfile tProfile = createProfileAndLoadInitial(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    int seriesBefore = dataset.getSeriesCount();
    log.info("Before incremental: series={}, items={}", seriesBefore, dataset.getItemCount());

    long maxIncrTs = TestDataFactory.getMaxTimestamp(dataSet.getIncrementalRecords());
    sleepMs(TestDataFactory.millisUntil(maxIncrTs) + 500L);

    loadIncremental(dataSet, tProfile);
    scp.loadData();

    int seriesAfter = dataset.getSeriesCount();
    log.info("After incremental: series={}, items={}", seriesAfter, dataset.getItemCount());
    assertTrue(seriesAfter >= seriesBefore);
    assertTrue(dataset.getItemCount() > 0);
  }

  @Test
  public void incrementalLoadWithGapTest() {
    TestDataSet dataSet = TestDataFactory.incrementalWithGap();

    assertTenMinuteWindow(dataSet.getInitialRecords());
    assertEveryMeasurementHasFiveValues(dataSet.getIncrementalRecords());

    TProfile tProfile = createProfileAndLoadInitial(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    long maxIncrTs = TestDataFactory.getMaxTimestamp(dataSet.getIncrementalRecords());
    sleepMs(TestDataFactory.millisUntil(maxIncrTs) + 500L);

    loadIncremental(dataSet, tProfile);
    scp.loadData();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    assertNotNull(dataset);
    assertTrue(dataset.getSeriesCount() > 0);
    assertTrue(dataset.getItemCount() > 0);
  }

  @Test
  public void loadDataWithNoNewDataTest() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    int itemsBefore = dataset.getItemCount();
    int seriesBefore = dataset.getSeriesCount();

    scp.loadData();

    assertEquals(itemsBefore, dataset.getItemCount());
    assertEquals(seriesBefore, dataset.getSeriesCount());
  }

  @Test
  public void filterNullInitializeTest() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCPWithFilter(tProfile, null);
    scp.initialize();

    assertNull(scp.getFilter());
    assertNotNull(scp.getChartDataset());
  }

  @Test
  public void filterEmptyValuesInitializeTest() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ChartConfig config = buildChartConfig(tProfile, GatherDataMode.BY_CLIENT_JDBC);
    CProfile yAxis = config.getMetric().getYAxis();
    Map<CProfile, LinkedHashSet<String>> emptyFilter = Map.of(yAxis, new LinkedHashSet<>());

    ClientRealtimeSCP scp = createSCPWithFilter(tProfile, emptyFilter);
    scp.initialize();

    assertNull(scp.getFilter());
    assertNotNull(scp.getChartDataset());
  }

  @Test
  public void filterWithSelectedEnumTest() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ChartConfig config = buildChartConfig(tProfile, GatherDataMode.BY_CLIENT_JDBC);
    CProfile yAxis = config.getMetric().getYAxis();

    String selectedEnum = getFirstEnumValue(dataSet);
    LinkedHashSet<String> selected = new LinkedHashSet<>();
    selected.add(selectedEnum);
    Map<CProfile, LinkedHashSet<String>> filter = Map.of(yAxis, selected);

    ClientRealtimeSCP scp = createSCPWithFilter(tProfile, filter);
    scp.initialize();

    Map<CProfile, LinkedHashSet<String>> resultFilter = scp.getFilter();
    assertNotNull(resultFilter);
    assertTrue(resultFilter.containsKey(yAxis));
    assertEquals(1, resultFilter.get(yAxis).size());
    assertTrue(resultFilter.get(yAxis).contains(selectedEnum));
  }

  @Test
  public void reinitializeChartInCustomModeTest() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    scp.reinitializeChartInCustomMode();

    assertEquals(SeriesType.CUSTOM, scp.getSeriesType());
    assertNotNull(scp.getFilter());
  }

  @Test
  public void gapDetectionUtilityTest() {
    TestDataSet normalData = TestDataFactory.normalData();
    assertFalse(TestDataFactory.hasGaps(normalData.getRecords(), TestDataFactory.INTERVAL_MS));

    TestDataSet gapData = TestDataFactory.gapData();
    assertTrue(TestDataFactory.hasGaps(gapData.getRecords(), TestDataFactory.INTERVAL_MS));

    TestDataSet multiGapData = TestDataFactory.multipleGaps();
    assertTrue(TestDataFactory.hasGaps(multiGapData.getRecords(), TestDataFactory.INTERVAL_MS));
  }

  @Test
  public void consecutiveLoadDataCallsTest() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    CategoryTableXYDatasetRealTime dataset = scp.getChartDataset();
    int itemsBefore = dataset.getItemCount();
    int seriesBefore = dataset.getSeriesCount();

    for (int i = 0; i < 3; i++) {
      scp.loadData();
    }

    assertEquals(itemsBefore, dataset.getItemCount());
    assertEquals(seriesBefore, dataset.getSeriesCount());
  }

  @Test
  public void dataTimestampOrderingTest() {
    TestDataSet dataSet = TestDataFactory.normalData();
    List<Long> timestamps = TestDataFactory.getSortedDistinctTimestamps(dataSet.getRecords());

    for (int i = 1; i < timestamps.size(); i++) {
      assertTrue(timestamps.get(i) > timestamps.get(i - 1));
    }

    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    assertNotNull(scp.getChartDataset());
  }

  @Test
  public void rawDataVerificationAfterLoadTest() {
    TestDataSet dataSet = TestDataFactory.normalData();
    String tableName = "raw_verify_" + (tableCounter++);
    TProfile tProfile = getTProfile(tableName);
    TestDataFactory.loadRecordsIntoDStore(dStore, tProfile, dataSet.getRecords());

    long minTs = TestDataFactory.getMinTimestamp(dataSet.getRecords());
    long maxTs = TestDataFactory.getMaxTimestamp(dataSet.getRecords());

    List<List<Object>> rawData = dStore.getRawDataAll(tableName, minTs - 1, maxTs + 1);
    assertNotNull(rawData);
    assertFalse(rawData.isEmpty());

    long expectedRowCount = TestDataFactory.countDistinctTimestamps(dataSet.getRecords());
    int rawColumnCount = rawData.getFirst().size();

    log.info("Raw data rows: {}, columns: {}, expected rows: {}", rawData.size(), rawColumnCount, expectedRowCount);

    assertEquals(expectedRowCount, rawData.size());
    assertEquals(4, rawColumnCount);
  }

  @Test
  public void blockKeyTailListNonEmptyForNormalDataTest() throws Exception {
    TestDataSet dataSet = TestDataFactory.normalData();
    String tableName = "bkt_verify_" + (tableCounter++);
    TProfile tProfile = getTProfile(tableName);
    TestDataFactory.loadRecordsIntoDStore(dStore, tProfile, dataSet.getRecords());

    long minTs = TestDataFactory.getMinTimestamp(dataSet.getRecords());
    long maxTs = TestDataFactory.getMaxTimestamp(dataSet.getRecords());

    List<BlockKeyTail> tails = dStore.getBlockKeyTailList(tableName, minTs - 1, maxTs + 1);
    assertNotNull(tails);
    assertFalse(tails.isEmpty());
  }

  @Test
  public void blockKeyTailListEmptyForEmptyDataTest() throws Exception {
    String tableName = "bkt_empty_" + (tableCounter++);
    getTProfile(tableName);

    List<BlockKeyTail> tails = dStore.getBlockKeyTailList(tableName, 0, Long.MAX_VALUE);
    assertNotNull(tails);
    assertTrue(tails.isEmpty());
  }

  @Test
  public void gapDataBlockKeyTailVerificationTest() throws Exception {
    TestDataSet dataSet = TestDataFactory.gapData();
    String tableName = "bkt_gap_" + (tableCounter++);
    TProfile tProfile = getTProfile(tableName);
    TestDataFactory.loadRecordsIntoDStore(dStore, tProfile, dataSet.getRecords());

    long[] gapBounds = findFirstGapBounds(dataSet.getRecords());
    assertTrue(gapBounds[0] > 0L);
    assertTrue(gapBounds[1] > gapBounds[0]);

    List<BlockKeyTail> gapTails = dStore.getBlockKeyTailList(tableName, gapBounds[0] + 1, gapBounds[1] - 1);
    assertTrue(gapTails.isEmpty());
  }

  @Test
  public void seriesColorMapPopulatedTest() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    assertNotNull(scp.getSeriesColorMap());
    assertFalse(scp.getSeriesColorMap().isEmpty());
  }

  @Test
  public void chartDatasetNotNullAfterInitTest() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    assertNotNull(scp.getChartDataset());
    assertNotNull(scp.getjFreeChart());
    assertNotNull(scp.getConfig());
  }

  @Test
  public void distinctEnumValuesMatchLoadedDataTest() throws Exception {
    TestDataSet dataSet = TestDataFactory.normalData();
    String tableName = "distinct_verify_" + (tableCounter++);
    TProfile tProfile = getTProfile(tableName);
    TestDataFactory.loadRecordsIntoDStore(dStore, tProfile, dataSet.getRecords());

    long minTs = TestDataFactory.getMinTimestamp(dataSet.getRecords());
    long maxTs = TestDataFactory.getMaxTimestamp(dataSet.getRecords());
    ChartConfig config = buildChartConfig(tProfile, GatherDataMode.BY_CLIENT_JDBC);

    List<String> distinct = dStore.getDistinct(
        tableName,
        config.getMetric().getYAxis(),
        ru.dimension.db.model.OrderBy.DESC,
        null,
        100,
        minTs - 1,
        maxTs + 1
    );

    assertEquals(TestDataFactory.getDistinctEnumValues(dataSet.getRecords()).size(), distinct.size());
    assertTrue(distinct.containsAll(TestDataFactory.getDistinctEnumValues(dataSet.getRecords())));
  }
}