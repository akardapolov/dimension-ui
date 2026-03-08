package ru.dimension.ui.chart.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.EnumByteExceedException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.ui.chart.model.TestDataSet;
import ru.dimension.ui.chart.model.TestRecord;

public class TestDataFactory {

  public static final int WINDOW_MINUTES = 10;
  public static final long WINDOW_MS = Duration.ofMinutes(WINDOW_MINUTES).toMillis();
  public static final long INTERVAL_MS = 3000L;
  public static final int VALUES_PER_MEASUREMENT = 5;
  public static final int MEASUREMENTS_PER_WINDOW = (int) (WINDOW_MS / INTERVAL_MS) + 1;

  private static final int MIN_RANDOM_VALUE = 1;
  private static final int MAX_RANDOM_VALUE = 10;

  public static TestDataSet normalData() {
    long endTimestamp = defaultWindowEnd();
    List<TestRecord> records = generateWindowRecords(defaultWindowStart(endTimestamp),
                                                     MEASUREMENTS_PER_WINDOW,
                                                     new Random(11L),
                                                     null,
                                                     new int[0][0]);

    return TestDataSet.builder()
        .description("Normal continuous data for a 10-minute realtime window")
        .tableName("normal_data_test")
        .records(records)
        .build();
  }

  public static TestDataSet emptyData() {
    return TestDataSet.builder()
        .description("Empty dataset")
        .tableName("empty_data_test")
        .records(List.of())
        .build();
  }

  public static TestDataSet singleRecord() {
    return singleMeasurement();
  }

  public static TestDataSet singleMeasurement() {
    long timestamp = defaultWindowEnd();
    List<TestRecord> records = generateMeasurement(timestamp, new Random(21L), null);

    return TestDataSet.builder()
        .description("Single measurement with five values")
        .tableName("single_measurement_test")
        .records(records)
        .build();
  }

  public static TestDataSet singleEnum() {
    long endTimestamp = defaultWindowEnd();
    Random random = new Random(31L);
    double enumValue = nextRandomValue(random);
    List<TestRecord> records = generateWindowRecords(defaultWindowStart(endTimestamp),
                                                     MEASUREMENTS_PER_WINDOW,
                                                     random,
                                                     enumValue,
                                                     new int[0][0]);

    return TestDataSet.builder()
        .description("Continuous data where VALUE_ENUM is identical for the whole window")
        .tableName("single_enum_test")
        .records(records)
        .build();
  }

  public static TestDataSet duplicateTimestamps() {
    long timestamp = defaultWindowEnd();
    Random random = new Random(41L);
    List<TestRecord> records = new ArrayList<>();

    for (int i = 0; i < VALUES_PER_MEASUREMENT * 4; i++) {
      records.add(generateRecord(timestamp, random, null));
    }

    return TestDataSet.builder()
        .description("Many records at the same timestamp")
        .tableName("duplicate_ts_test")
        .records(records)
        .build();
  }

  public static TestDataSet gapData() {
    long endTimestamp = defaultWindowEnd();
    int middle = MEASUREMENTS_PER_WINDOW / 2;
    int[][] skippedRanges = {
        {middle, middle + 3}
    };

    List<TestRecord> records = generateWindowRecords(defaultWindowStart(endTimestamp),
                                                     MEASUREMENTS_PER_WINDOW,
                                                     new Random(51L),
                                                     null,
                                                     skippedRanges);

    return TestDataSet.builder()
        .description("10-minute data with one gap")
        .tableName("gap_data_test")
        .records(records)
        .build();
  }

  public static TestDataSet multipleGaps() {
    long endTimestamp = defaultWindowEnd();
    int firstGapStart = MEASUREMENTS_PER_WINDOW / 4;
    int secondGapStart = MEASUREMENTS_PER_WINDOW - 60;
    int[][] skippedRanges = {
        {firstGapStart, firstGapStart + 2},
        {secondGapStart, secondGapStart + 3}
    };

    List<TestRecord> records = generateWindowRecords(defaultWindowStart(endTimestamp),
                                                     MEASUREMENTS_PER_WINDOW,
                                                     new Random(61L),
                                                     null,
                                                     skippedRanges);

    return TestDataSet.builder()
        .description("10-minute data with multiple gaps")
        .tableName("multiple_gaps_test")
        .records(records)
        .build();
  }

  public static TestDataSet incrementalLoad() {
    long now = currentLocalAlignedMillis();
    long futureTimestamp = now + INTERVAL_MS * 3;
    long startTimestamp = futureTimestamp - WINDOW_MS;

    List<TestRecord> initial = generateWindowRecords(startTimestamp,
                                                     MEASUREMENTS_PER_WINDOW,
                                                     new Random(71L),
                                                     null,
                                                     new int[0][0]);

    List<TestRecord> incremental = generateMeasurement(futureTimestamp + INTERVAL_MS, new Random(72L), null);

    return TestDataSet.builder()
        .description("Continuous initial window and one future incremental measurement")
        .tableName("incremental_load_test")
        .initialRecords(initial)
        .incrementalRecords(incremental)
        .build();
  }

  public static TestDataSet incrementalWithGap() {
    long now = currentLocalAlignedMillis();
    long futureTimestamp = now + INTERVAL_MS * 3;
    long startTimestamp = futureTimestamp - WINDOW_MS;

    List<TestRecord> initial = generateWindowRecords(startTimestamp,
                                                     MEASUREMENTS_PER_WINDOW,
                                                     new Random(81L),
                                                     null,
                                                     new int[0][0]);

    List<TestRecord> incremental = generateMeasurement(futureTimestamp + INTERVAL_MS * 4, new Random(82L), null);

    return TestDataSet.builder()
        .description("Continuous initial window and future incremental measurement with gap")
        .tableName("incremental_gap_test")
        .initialRecords(initial)
        .incrementalRecords(incremental)
        .build();
  }

  public static void loadRecordsIntoDStore(DStore dStore, TProfile tProfile, List<TestRecord> records) {
    for (TestRecord record : records) {
      List<List<Object>> data = toColumnData(tProfile, record);
      try {
        dStore.putDataDirect(tProfile.getTableName(), data);
      } catch (SqlColMetadataException | EnumByteExceedException e) {
        throw new RuntimeException("Failed to put data for dt=" + record.getDt(), e);
      }
    }
  }

  public static long getMinTimestamp(List<TestRecord> records) {
    return records.stream().mapToLong(TestRecord::getDt).min().orElse(0L);
  }

  public static long getMaxTimestamp(List<TestRecord> records) {
    return records.stream().mapToLong(TestRecord::getDt).max().orElse(0L);
  }

  public static long countDistinctTimestamps(List<TestRecord> records) {
    return records.stream().mapToLong(TestRecord::getDt).distinct().count();
  }

  public static Set<String> getDistinctEnumValues(List<TestRecord> records) {
    return records.stream()
        .map(record -> String.valueOf((long) record.getValueEnum()))
        .collect(Collectors.toCollection(TreeSet::new));
  }

  public static long countDistinctEnumValues(List<TestRecord> records) {
    return getDistinctEnumValues(records).size();
  }

  public static List<Long> getSortedDistinctTimestamps(List<TestRecord> records) {
    return records.stream()
        .mapToLong(TestRecord::getDt)
        .distinct()
        .sorted()
        .boxed()
        .toList();
  }

  public static boolean hasGaps(List<TestRecord> records, long expectedIntervalMs) {
    List<Long> timestamps = getSortedDistinctTimestamps(records);
    for (int i = 1; i < timestamps.size(); i++) {
      if (timestamps.get(i) - timestamps.get(i - 1) > expectedIntervalMs) {
        return true;
      }
    }
    return false;
  }

  public static int countGaps(List<TestRecord> records, long expectedIntervalMs) {
    List<Long> timestamps = getSortedDistinctTimestamps(records);
    int count = 0;
    for (int i = 1; i < timestamps.size(); i++) {
      if (timestamps.get(i) - timestamps.get(i - 1) > expectedIntervalMs) {
        count++;
      }
    }
    return count;
  }

  public static long millisUntil(long timestamp) {
    return Math.max(0L, timestamp - System.currentTimeMillis());
  }

  public static int expectedRawRowCount(List<TestRecord> records) {
    return records.size();
  }

  private static List<TestRecord> generateWindowRecords(long startTimestamp,
                                                        int measurementCount,
                                                        Random random,
                                                        Double fixedEnumValue,
                                                        int[][] skippedRanges) {
    List<TestRecord> records = new ArrayList<>();

    for (int i = 0; i < measurementCount; i++) {
      if (shouldSkipMeasurement(i, skippedRanges)) {
        continue;
      }
      long timestamp = startTimestamp + i * INTERVAL_MS;
      records.addAll(generateMeasurement(timestamp, random, fixedEnumValue));
    }

    return records;
  }

  private static List<TestRecord> generateMeasurement(long timestamp,
                                                      Random random,
                                                      Double fixedEnumValue) {
    List<TestRecord> records = new ArrayList<>(VALUES_PER_MEASUREMENT);
    for (int i = 0; i < VALUES_PER_MEASUREMENT; i++) {
      records.add(generateRecord(timestamp, random, fixedEnumValue));
    }
    return records;
  }

  private static TestRecord generateRecord(long timestamp,
                                           Random random,
                                           Double fixedEnumValue) {
    double valueRaw = nextRandomValue(random);
    double valueEnum = fixedEnumValue == null ? nextRandomValue(random) : fixedEnumValue;
    double valueHistogram = nextRandomValue(random);
    return new TestRecord(timestamp, valueRaw, valueEnum, valueHistogram);
  }

  private static boolean shouldSkipMeasurement(int index, int[][] skippedRanges) {
    for (int[] skippedRange : skippedRanges) {
      if (index >= skippedRange[0] && index <= skippedRange[1]) {
        return true;
      }
    }
    return false;
  }

  private static double nextRandomValue(Random random) {
    return MIN_RANDOM_VALUE + random.nextInt(MAX_RANDOM_VALUE - MIN_RANDOM_VALUE + 1);
  }

  static long currentLocalAlignedMillis() {
    long now = LocalDateTime.now()
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli();
    return alignToInterval(now);
  }

  private static long defaultWindowEnd() {
    return currentLocalAlignedMillis();
  }

  private static long defaultWindowStart(long endTimestamp) {
    return endTimestamp - WINDOW_MS;
  }

  private static long alignToInterval(long timestamp) {
    return timestamp - Math.floorMod(timestamp, INTERVAL_MS);
  }

  private static List<List<Object>> toColumnData(TProfile tProfile, TestRecord record) {
    List<List<Object>> data = new ArrayList<>();
    tProfile.getCProfiles().forEach(cProfile -> {
      List<Object> colData = new ArrayList<>(1);
      switch (cProfile.getColName()) {
        case "DT" -> colData.add(record.getDt());
        case "VALUE_RAW" -> colData.add((long) record.getValueRaw());
        case "VALUE_ENUM" -> colData.add(String.valueOf((long) record.getValueEnum()));
        case "VALUE_HISTOGRAM" -> colData.add((long) record.getValueHistogram());
      }
      data.add(cProfile.getColId(), colData);
    });
    return data;
  }
}