package ru.dimension.ui.chart;

import jakarta.inject.Provider;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import ru.dimension.db.model.OrderBy;
import ru.dimension.db.model.output.BlockKeyTail;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.HandlerMock;
import ru.dimension.ui.chart.model.TestDataSet;
import ru.dimension.ui.chart.model.TestRecord;
import ru.dimension.ui.chart.util.TestDataFactory;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.realtime.ClientRealtimeSCP;
import ru.dimension.ui.helper.ColorHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.data.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.view.SeriesType;

public class ClientRealtimeSCPGUITest extends AbstractBackendDataTest {

  private static final Color PASS_COLOR = new Color(46, 125, 50);
  private static final Color FAIL_COLOR = new Color(198, 40, 40);
  private static final Color WAIT_COLOR = new Color(251, 192, 45);
  private static final Color PASS_LIGHT_COLOR = new Color(232, 245, 233);
  private static final Color FAIL_LIGHT_COLOR = new Color(255, 235, 238);
  private static final Color WAIT_LIGHT_COLOR = new Color(255, 248, 225);

  private int tableCounter = 0;
  private final ArrayList<TestView> testViews = new ArrayList<>();
  private ReportPanel reportPanel;
  private JSplitPane splitPane;

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      ClientRealtimeSCPGUITest app = new ClientRealtimeSCPGUITest();
      app.start();
    });
  }

  private void start() {
    try {
      initializeHandlerMockLifecycle();
      ensureColorHelperRegistered();

      File dir = Files.createTempDirectory("client-realtime-scp-gui-test").toFile();
      initialize(dir);

      JFrame frame = new JFrame("ClientRealtimeSCP GUI Test");
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setSize(1600, 1000);
      frame.setLocationRelativeTo(null);

      reportPanel = new ReportPanel();

      JPanel chartsContent = new JPanel();
      chartsContent.setLayout(new BoxLayout(chartsContent, BoxLayout.Y_AXIS));

      buildNormalPanel();
      buildEmptyPanel();
      buildSingleMeasurementPanel();
      buildSingleEnumPanel();
      buildDuplicateTimestampsPanel();
      buildGapPanel();
      buildMultipleGapsPanel();
      buildFilterNullPanel();
      buildFilterEmptyPanel();
      buildFilterSelectedPanel();
      buildReinitializeCustomPanel();
      buildIncrementalPanel();
      buildIncrementalGapPanel();

      for (TestView testView : testViews) {
        chartsContent.add(testView.panel);
      }

      JButton goToReportButton = new JButton("Перейти к отчету");
      goToReportButton.addActionListener(e -> showReport());

      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      topPanel.add(goToReportButton, BorderLayout.WEST);

      JScrollPane chartsScrollPane = new JScrollPane(chartsContent);
      splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartsScrollPane, reportPanel);
      splitPane.setResizeWeight(0.72);

      JPanel root = new JPanel(new BorderLayout());
      root.add(topPanel, BorderLayout.NORTH);
      root.add(splitPane, BorderLayout.CENTER);

      frame.setContentPane(root);
      frame.setVisible(true);

      SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.72));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void showReport() {
    reportPanel.selectMostRelevantTest();
    splitPane.setDividerLocation(0.55);
    reportPanel.focusSummaryTable();
  }

  private void initializeHandlerMockLifecycle() {
    invokeAnnotatedLifecycleMethods(HandlerMock.class, BeforeAll.class);
    invokeAnnotatedLifecycleMethods(HandlerMock.class, BeforeEach.class);
  }

  private void invokeAnnotatedLifecycleMethods(Class<?> type, Class<? extends Annotation> annotationType) {
    for (Method method : type.getDeclaredMethods()) {
      if (!method.isAnnotationPresent(annotationType)) {
        continue;
      }
      if (method.getParameterCount() != 0) {
        continue;
      }
      try {
        method.setAccessible(true);
        Object target = Modifier.isStatic(method.getModifiers()) ? null : this;
        method.invoke(target);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void ensureColorHelperRegistered() {
    try {
      ServiceLocator.get(ColorHelper.class);
      return;
    } catch (Exception ignored) {
    }

    ColorHelper colorHelper = Mockito.mock(ColorHelper.class);
    Mockito.when(colorHelper.getColor(Mockito.anyString(), Mockito.anyString()))
        .thenAnswer(invocation -> buildColor(invocation.getArgument(1)));
    Mockito.when(colorHelper.getColorMap(Mockito.anyString()))
        .thenReturn(new HashMap<>());

    registerInServiceLocator(ColorHelper.class, colorHelper);

    ServiceLocator.get(ColorHelper.class);
  }

  private Color buildColor(String seriesName) {
    int hash = Math.abs(seriesName == null ? 0 : seriesName.hashCode());
    int red = 30 + (hash % 180);
    int green = 30 + ((hash / 7) % 180);
    int blue = 30 + ((hash / 13) % 180);
    return new Color(red, green, blue);
  }

  private <T> void registerInServiceLocator(Class<T> type, T instance) {
    List<Method> methods = new ArrayList<>();
    for (Method method : ServiceLocator.class.getMethods()) {
      methods.add(method);
    }
    for (Method method : ServiceLocator.class.getDeclaredMethods()) {
      methods.add(method);
    }

    for (Method method : methods) {
      if (!Modifier.isStatic(method.getModifiers())) {
        continue;
      }

      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length != 2) {
        continue;
      }
      if (parameterTypes[0] != Class.class) {
        continue;
      }

      try {
        method.setAccessible(true);

        if (parameterTypes[1].isAssignableFrom(instance.getClass())) {
          method.invoke(null, type, instance);
          return;
        }

        if (Provider.class.isAssignableFrom(parameterTypes[1])) {
          Provider<T> provider = () -> instance;
          method.invoke(null, type, provider);
          return;
        }
      } catch (Exception ignored) {
      }
    }

    throw new IllegalStateException("No suitable ServiceLocator registration method for " + type.getName());
  }

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

  private ClientRealtimeSCP createSCPWithFilter(TProfile tProfile, Map<CProfile, LinkedHashSet<String>> filter) {
    ChartConfig config = buildChartConfig(tProfile, GatherDataMode.BY_CLIENT_JDBC);
    ProfileTaskQueryKey ptqKey = config.getChartKey().getProfileTaskQueryKey();
    return new ClientRealtimeSCP(sqlQueryState, dStore, config, ptqKey, filter);
  }

  private TestView buildNormalPanel() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    ValidationReport report = new ValidationReport();
    report.check(hasTenMinuteWindow(dataSet.getRecords()), "окно 10 минут и по 5 значений", "некорректное окно или размер измерений");
    report.check(!TestDataFactory.hasGaps(dataSet.getRecords(), TestDataFactory.INTERVAL_MS), "gap-ов нет", "найдены gap-ы");
    report.check(isStrictlyOrdered(dataSet.getRecords()), "timestamp упорядочены", "timestamp не упорядочены");
    report.check(scp.getChartDataset() != null, "dataset создан", "dataset не создан");
    report.check(scp.getChartDataset().getSeriesCount() > 0, "серии построены", "серии не построены");
    report.check(scp.getChartDataset().getItemCount() > 0, "данные загружены", "данные не загружены");
    report.check(scp.getSeriesType() == SeriesType.COMMON, "режим COMMON", "ожидался COMMON");
    report.check(rawRowsCount(tProfile.getTableName(), dataSet.getRecords()) == TestDataFactory.countDistinctTimestamps(dataSet.getRecords()), "rawData соответствует числу measurement", "rawData не соответствует числу measurement");
    report.check(distinctMatches(tProfile, dataSet), "distinct VALUE_ENUM совпадает", "distinct VALUE_ENUM не совпадает");
    report.check(blockTailsNonEmpty(tProfile.getTableName(), dataSet.getRecords()), "BlockKeyTail найден", "BlockKeyTail пуст");
    report.check(scp.getSeriesColorMap() != null && !scp.getSeriesColorMap().isEmpty(), "цвета серий заполнены", "цвета серий не заполнены");

    int itemsBefore = scp.getChartDataset().getItemCount();
    int seriesBefore = scp.getChartDataset().getSeriesCount();
    scp.loadData();
    report.check(scp.getChartDataset().getItemCount() == itemsBefore, "повторная загрузка без новых данных стабильна", "повторная загрузка изменила itemCount");
    report.check(scp.getChartDataset().getSeriesCount() == seriesBefore, "повторная загрузка не меняет серии", "повторная загрузка изменила seriesCount");

    return wrapPanel("normalData / rawData / distinct / colors / noNewData", scp, report);
  }

  private TestView buildEmptyPanel() {
    TestDataSet dataSet = TestDataFactory.emptyData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    ValidationReport report = new ValidationReport();
    report.check(scp.getChartDataset() != null, "dataset создан", "dataset не создан");
    report.check(scp.getChartDataset().getItemCount() == 0, "itemCount = 0", "itemCount должен быть 0");
    report.check(scp.getChartDataset().getSeriesCount() == 0, "seriesCount = 0", "seriesCount должен быть 0");
    report.check(blockTailsEmpty(tProfile.getTableName()), "BlockKeyTail пуст", "BlockKeyTail должен быть пуст");

    return wrapPanel("emptyData", scp, report);
  }

  private TestView buildSingleMeasurementPanel() {
    TestDataSet dataSet = TestDataFactory.singleMeasurement();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    ValidationReport report = new ValidationReport();
    report.check(TestDataFactory.countDistinctTimestamps(dataSet.getRecords()) == 1, "один timestamp", "ожидался один timestamp");
    report.check(hasFiveValuesPerMeasurement(dataSet.getRecords()), "пять значений в measurement", "в measurement не пять значений");
    report.check(scp.getChartDataset() != null, "dataset создан", "dataset не создан");
    report.check(scp.getChartDataset().getSeriesCount() >= 1, "график построен", "график не построен");

    return wrapPanel("singleMeasurement", scp, report);
  }

  private TestView buildSingleEnumPanel() {
    TestDataSet dataSet = TestDataFactory.singleEnum();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    ValidationReport report = new ValidationReport();
    report.check(hasTenMinuteWindow(dataSet.getRecords()), "окно корректно", "окно некорректно");
    report.check(TestDataFactory.countDistinctEnumValues(dataSet.getRecords()) == 1, "один enum", "enum должен быть один");
    report.check(scp.getChartDataset() != null, "dataset создан", "dataset не создан");
    report.check(scp.getChartDataset().getSeriesCount() == 1, "ровно одна серия", "ожидалась одна серия");

    return wrapPanel("singleEnum", scp, report);
  }

  private TestView buildDuplicateTimestampsPanel() {
    TestDataSet dataSet = TestDataFactory.duplicateTimestamps();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    ValidationReport report = new ValidationReport();
    report.check(TestDataFactory.countDistinctTimestamps(dataSet.getRecords()) == 1, "duplicate timestamp обработаны", "ожидался один distinct timestamp");
    report.check(scp.getChartDataset() != null, "dataset создан", "dataset не создан");
    report.check(scp.getChartDataset().getSeriesCount() > 0, "серии построены", "серии не построены");

    return wrapPanel("duplicateTimestamps", scp, report);
  }

  private TestView buildGapPanel() {
    TestDataSet dataSet = TestDataFactory.gapData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    ValidationReport report = new ValidationReport();
    report.check(TestDataFactory.hasGaps(dataSet.getRecords(), TestDataFactory.INTERVAL_MS), "gap обнаружен", "gap должен быть обнаружен");
    report.check(TestDataFactory.countGaps(dataSet.getRecords(), TestDataFactory.INTERVAL_MS) == 1, "ровно один gap", "ожидался один gap");
    report.check(isGapRegionEmptyInStore(tProfile.getTableName(), dataSet.getRecords()), "в gap-регионе нет BlockKeyTail", "в gap-регионе найден BlockKeyTail");
    report.check(scp.getChartDataset() != null, "dataset создан", "dataset не создан");
    report.check(scp.getChartDataset().getSeriesCount() > 0, "график построен", "график не построен");

    return wrapPanel("gapData", scp, report);
  }

  private TestView buildMultipleGapsPanel() {
    TestDataSet dataSet = TestDataFactory.multipleGaps();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    ValidationReport report = new ValidationReport();
    report.check(TestDataFactory.countGaps(dataSet.getRecords(), TestDataFactory.INTERVAL_MS) >= 2, "несколько gap-ов обнаружены", "ожидалось несколько gap-ов");
    report.check(scp.getChartDataset() != null, "dataset создан", "dataset не создан");
    report.check(scp.getChartDataset().getSeriesCount() > 0, "график построен", "график не построен");

    return wrapPanel("multipleGaps", scp, report);
  }

  private TestView buildFilterNullPanel() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCPWithFilter(tProfile, null);
    scp.initialize();

    ValidationReport report = new ValidationReport();
    report.check(scp.getFilter() == null, "null filter обработан", "filter должен быть null");
    report.check(scp.getChartDataset() != null, "dataset создан", "dataset не создан");

    return wrapPanel("filterNull", scp, report);
  }

  private TestView buildFilterEmptyPanel() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ChartConfig config = buildChartConfig(tProfile, GatherDataMode.BY_CLIENT_JDBC);
    CProfile yAxis = config.getMetric().getYAxis();
    Map<CProfile, LinkedHashSet<String>> emptyFilter = Map.of(yAxis, new LinkedHashSet<>());

    ClientRealtimeSCP scp = createSCPWithFilter(tProfile, emptyFilter);
    scp.initialize();

    ValidationReport report = new ValidationReport();
    report.check(scp.getFilter() == null, "empty filter обработан как null", "empty filter должен стать null");
    report.check(scp.getChartDataset() != null, "dataset создан", "dataset не создан");

    return wrapPanel("filterEmpty", scp, report);
  }

  private TestView buildFilterSelectedPanel() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ChartConfig config = buildChartConfig(tProfile, GatherDataMode.BY_CLIENT_JDBC);
    CProfile yAxis = config.getMetric().getYAxis();
    ArrayList<String> distinctEnumValues = new ArrayList<>(TestDataFactory.getDistinctEnumValues(dataSet.getRecords()));
    String selectedEnum = distinctEnumValues.getFirst();

    LinkedHashSet<String> selected = new LinkedHashSet<>();
    selected.add(selectedEnum);

    ClientRealtimeSCP scp = createSCPWithFilter(tProfile, Map.of(yAxis, selected));
    scp.initialize();

    ValidationReport report = new ValidationReport();
    report.check(scp.getFilter() != null, "filter сохранен", "filter не должен быть null");
    report.check(scp.getFilter().containsKey(yAxis), "filter содержит ось", "filter не содержит ось");
    report.check(scp.getFilter().get(yAxis).contains(selectedEnum), "selected enum сохранен", "selected enum не сохранен");

    return wrapPanel("filterSelected", scp, report);
  }

  private TestView buildReinitializeCustomPanel() {
    TestDataSet dataSet = TestDataFactory.normalData();
    TProfile tProfile = createProfileAndLoad(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();
    scp.reinitializeChartInCustomMode();

    ValidationReport report = new ValidationReport();
    report.check(scp.getSeriesType() == SeriesType.CUSTOM, "принудительный CUSTOM работает", "режим CUSTOM не включился");
    report.check(scp.getFilter() != null, "custom filter создан", "custom filter не создан");
    report.check(scp.getChartDataset() != null, "dataset жив после reinitialize", "dataset отсутствует после reinitialize");

    return wrapPanel("reinitializeChartInCustomMode", scp, report);
  }

  private TestView buildIncrementalPanel() {
    TestDataSet dataSet = TestDataFactory.incrementalLoad();
    TProfile tProfile = createProfileAndLoadInitial(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    ValidationReport initialReport = new ValidationReport();
    initialReport.check(hasTenMinuteWindow(dataSet.getInitialRecords()), "initial окно корректно", "initial окно некорректно");
    initialReport.check(hasFiveValuesPerMeasurement(dataSet.getIncrementalRecords()), "incremental набор корректен", "incremental набор некорректен");
    initialReport.waitStatus("ожидается загрузка incremental данных");

    TestView testView = wrapPanel("incrementalLoad", scp, initialReport);

    double endBefore = getDatasetEndX(scp.getChartDataset());
    long maxIncrementalTs = TestDataFactory.getMaxTimestamp(dataSet.getIncrementalRecords());
    int delayMs = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, TestDataFactory.millisUntil(maxIncrementalTs) + 250L));

    Timer timer = new Timer(delayMs, e -> {
      ValidationReport finalReport = initialReport.copyWithoutWait();
      try {
        loadIncremental(dataSet, tProfile);
        scp.loadData();
        double endAfter = getDatasetEndX(scp.getChartDataset());
        finalReport.check(endAfter > endBefore, "incremental сдвинул правую границу графика", "incremental не сдвинул график");
        updateTestView(testView, finalReport);
      } catch (Exception ex) {
        finalReport.fail(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        updateTestView(testView, finalReport);
      }
    });
    timer.setRepeats(false);
    timer.start();

    return testView;
  }

  private TestView buildIncrementalGapPanel() {
    TestDataSet dataSet = TestDataFactory.incrementalWithGap();
    TProfile tProfile = createProfileAndLoadInitial(dataSet);
    ClientRealtimeSCP scp = createSCP(tProfile);
    scp.initialize();

    ValidationReport initialReport = new ValidationReport();
    initialReport.check(hasTenMinuteWindow(dataSet.getInitialRecords()), "initial окно корректно", "initial окно некорректно");
    initialReport.check(hasFiveValuesPerMeasurement(dataSet.getIncrementalRecords()), "incremental набор корректен", "incremental набор некорректен");
    initialReport.waitStatus("ожидается загрузка incremental данных");

    TestView testView = wrapPanel("incrementalWithGap", scp, initialReport);

    long maxIncrementalTs = TestDataFactory.getMaxTimestamp(dataSet.getIncrementalRecords());
    int delayMs = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, TestDataFactory.millisUntil(maxIncrementalTs) + 250L));

    Timer timer = new Timer(delayMs, e -> {
      ValidationReport finalReport = initialReport.copyWithoutWait();
      try {
        loadIncremental(dataSet, tProfile);
        scp.loadData();
        finalReport.check(scp.getChartDataset() != null, "dataset жив после incremental gap", "dataset отсутствует после incremental gap");
        finalReport.check(scp.getChartDataset().getSeriesCount() > 0, "график продолжает отображаться", "график после incremental gap пуст");
        updateTestView(testView, finalReport);
      } catch (Exception ex) {
        finalReport.fail(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        updateTestView(testView, finalReport);
      }
    });
    timer.setRepeats(false);
    timer.start();

    return testView;
  }

  private TestView wrapPanel(String title, ClientRealtimeSCP scp, ValidationReport report) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    panel.setAlignmentX(JPanel.LEFT_ALIGNMENT);

    JLabel label = new JLabel();
    updateHeaderLabel(label, title, report.getOverallStatus());
    panel.add(label, BorderLayout.NORTH);

    scp.setPreferredSize(new Dimension(1450, 250));
    panel.add(scp, BorderLayout.CENTER);
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

    TestView testView = new TestView(title, panel, label, report);
    testViews.add(testView);
    reportPanel.addTest(testView);

    return testView;
  }

  private void updateTestView(TestView testView, ValidationReport report) {
    testView.report = report;
    updateHeaderLabel(testView.headerLabel, testView.title, report.getOverallStatus());
    reportPanel.updateTest(testView);
  }

  private void updateHeaderLabel(JLabel label, String title, ValidationStatus status) {
    label.setOpaque(true);
    label.setBackground(status.getBackground());
    label.setForeground(status.getForeground());
    label.setHorizontalAlignment(SwingConstants.LEFT);
    label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
    label.setPreferredSize(new Dimension(10, 36));
    label.setText(title);
  }

  private boolean hasFiveValuesPerMeasurement(List<TestRecord> records) {
    Map<Long, Long> counts = records.stream()
        .collect(Collectors.groupingBy(TestRecord::getDt, Collectors.counting()));
    return !counts.isEmpty() && counts.values().stream().allMatch(count -> count == TestDataFactory.VALUES_PER_MEASUREMENT);
  }

  private boolean hasTenMinuteWindow(List<TestRecord> records) {
    ArrayList<Long> timestamps = new ArrayList<>(TestDataFactory.getSortedDistinctTimestamps(records));
    return !timestamps.isEmpty()
        && timestamps.size() == TestDataFactory.MEASUREMENTS_PER_WINDOW
        && timestamps.getLast() - timestamps.getFirst() == TestDataFactory.WINDOW_MS
        && hasFiveValuesPerMeasurement(records);
  }

  private boolean isStrictlyOrdered(List<TestRecord> records) {
    List<Long> timestamps = TestDataFactory.getSortedDistinctTimestamps(records);
    for (int i = 1; i < timestamps.size(); i++) {
      if (timestamps.get(i) <= timestamps.get(i - 1)) {
        return false;
      }
    }
    return true;
  }

  private int rawRowsCount(String tableName, List<TestRecord> records) {
    long minTs = TestDataFactory.getMinTimestamp(records);
    long maxTs = TestDataFactory.getMaxTimestamp(records);
    return dStore.getRawDataAll(tableName, minTs - 1, maxTs + 1).size();
  }

  private boolean distinctMatches(TProfile tProfile, TestDataSet dataSet) {
    try {
      long minTs = TestDataFactory.getMinTimestamp(dataSet.getRecords());
      long maxTs = TestDataFactory.getMaxTimestamp(dataSet.getRecords());
      ChartConfig config = buildChartConfig(tProfile, GatherDataMode.BY_CLIENT_JDBC);

      List<String> distinct = dStore.getDistinct(
          tProfile.getTableName(),
          config.getMetric().getYAxis(),
          OrderBy.DESC,
          null,
          100,
          minTs - 1,
          maxTs + 1
      );

      return distinct.size() == TestDataFactory.getDistinctEnumValues(dataSet.getRecords()).size()
          && distinct.containsAll(TestDataFactory.getDistinctEnumValues(dataSet.getRecords()));
    } catch (Exception e) {
      return false;
    }
  }

  private boolean blockTailsNonEmpty(String tableName, List<TestRecord> records) {
    try {
      long minTs = TestDataFactory.getMinTimestamp(records);
      long maxTs = TestDataFactory.getMaxTimestamp(records);
      List<BlockKeyTail> tails = dStore.getBlockKeyTailList(tableName, minTs - 1, maxTs + 1);
      return tails != null && !tails.isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  private boolean blockTailsEmpty(String tableName) {
    try {
      List<BlockKeyTail> tails = dStore.getBlockKeyTailList(tableName, 0, Long.MAX_VALUE);
      return tails != null && tails.isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isGapRegionEmptyInStore(String tableName, List<TestRecord> records) {
    try {
      long[] gapBounds = findFirstGapBounds(records);
      if (gapBounds[0] == 0L || gapBounds[1] == 0L) {
        return false;
      }
      List<BlockKeyTail> tails = dStore.getBlockKeyTailList(tableName, gapBounds[0] + 1, gapBounds[1] - 1);
      return tails.isEmpty();
    } catch (Exception e) {
      return false;
    }
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

  private double getDatasetEndX(CategoryTableXYDatasetRealTime dataset) {
    if (dataset == null || dataset.getSeriesCount() == 0 || dataset.getItemCount() == 0) {
      return 0D;
    }
    return dataset.getEndXValue(0, dataset.getItemCount() - 1);
  }

  private enum ValidationStatus {
    PASS("Успех", PASS_COLOR, PASS_LIGHT_COLOR, Color.WHITE),
    WAIT("В процессе", WAIT_COLOR, WAIT_LIGHT_COLOR, Color.BLACK),
    FAIL("Ошибка", FAIL_COLOR, FAIL_LIGHT_COLOR, Color.WHITE);

    private final String displayName;
    private final Color background;
    private final Color lightBackground;
    private final Color foreground;

    ValidationStatus(String displayName, Color background, Color lightBackground, Color foreground) {
      this.displayName = displayName;
      this.background = background;
      this.lightBackground = lightBackground;
      this.foreground = foreground;
    }

    public String getDisplayName() {
      return displayName;
    }

    public Color getBackground() {
      return background;
    }

    public Color getLightBackground() {
      return lightBackground;
    }

    public Color getForeground() {
      return foreground;
    }
  }

  private static class ValidationItem {
    private final ValidationStatus status;
    private final String message;

    ValidationItem(ValidationStatus status, String message) {
      this.status = status;
      this.message = message;
    }
  }

  private static class ValidationReport {
    private final ArrayList<ValidationItem> items = new ArrayList<>();

    void check(boolean condition, String successMessage, String failureMessage) {
      items.add(new ValidationItem(condition ? ValidationStatus.PASS : ValidationStatus.FAIL, condition ? successMessage : failureMessage));
    }

    void waitStatus(String message) {
      items.add(new ValidationItem(ValidationStatus.WAIT, message));
    }

    void fail(String message) {
      items.add(new ValidationItem(ValidationStatus.FAIL, message));
    }

    ValidationReport copyWithoutWait() {
      ValidationReport copy = new ValidationReport();
      for (ValidationItem item : items) {
        if (item.status != ValidationStatus.WAIT) {
          copy.items.add(item);
        }
      }
      return copy;
    }

    ValidationStatus getOverallStatus() {
      boolean hasWait = false;
      for (ValidationItem item : items) {
        if (item.status == ValidationStatus.FAIL) {
          return ValidationStatus.FAIL;
        }
        if (item.status == ValidationStatus.WAIT) {
          hasWait = true;
        }
      }
      return hasWait ? ValidationStatus.WAIT : ValidationStatus.PASS;
    }

    int count(ValidationStatus status) {
      int count = 0;
      for (ValidationItem item : items) {
        if (item.status == status) {
          count++;
        }
      }
      return count;
    }

    ArrayList<ValidationItem> getItems() {
      return items;
    }
  }

  private static class TestView {
    private final String title;
    private final JPanel panel;
    private final JLabel headerLabel;
    private ValidationReport report;

    TestView(String title, JPanel panel, JLabel headerLabel, ValidationReport report) {
      this.title = title;
      this.panel = panel;
      this.headerLabel = headerLabel;
      this.report = report;
    }
  }

  private class ReportPanel extends JPanel {

    private final SummaryTableModel summaryTableModel = new SummaryTableModel();
    private final DetailsTableModel detailsTableModel = new DetailsTableModel();
    private final JTable summaryTable = new JTable(summaryTableModel);
    private final JTable detailsTable = new JTable(detailsTableModel);
    private final JLabel detailsTitle = new JLabel("Детали проверки");

    ReportPanel() {
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      setPreferredSize(new Dimension(1600, 340));

      summaryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      summaryTable.setRowHeight(28);
      summaryTable.getTableHeader().setReorderingAllowed(false);
      summaryTable.setDefaultRenderer(String.class, new SummaryRowRenderer());
      summaryTable.setDefaultRenderer(Integer.class, new SummaryRowRenderer());
      summaryTable.setDefaultRenderer(ValidationStatus.class, new StatusCellRenderer());
      summaryTable.getColumnModel().getColumn(0).setPreferredWidth(280);
      summaryTable.getColumnModel().getColumn(1).setPreferredWidth(110);
      summaryTable.getColumnModel().getColumn(2).setPreferredWidth(60);
      summaryTable.getColumnModel().getColumn(3).setPreferredWidth(90);
      summaryTable.getColumnModel().getColumn(4).setPreferredWidth(60);
      summaryTable.getSelectionModel().addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting()) {
          showSelectedDetails();
        }
      });

      detailsTable.setRowHeight(26);
      detailsTable.getTableHeader().setReorderingAllowed(false);
      detailsTable.setDefaultRenderer(String.class, new DetailRowRenderer());
      detailsTable.setDefaultRenderer(ValidationStatus.class, new StatusCellRenderer());
      detailsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
      detailsTable.getColumnModel().getColumn(1).setPreferredWidth(900);

      detailsTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

      JPanel detailsPanel = new JPanel(new BorderLayout());
      detailsPanel.add(detailsTitle, BorderLayout.NORTH);
      detailsPanel.add(new JScrollPane(detailsTable), BorderLayout.CENTER);

      JSplitPane reportSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(summaryTable), detailsPanel);
      reportSplit.setResizeWeight(0.35);

      add(reportSplit, BorderLayout.CENTER);

      SwingUtilities.invokeLater(() -> reportSplit.setDividerLocation(0.35));
    }

    void addTest(TestView testView) {
      int row = testViews.size() - 1;
      summaryTableModel.fireTableRowsInserted(row, row);
      if (row == 0) {
        summaryTable.setRowSelectionInterval(0, 0);
        showSelectedDetails();
      }
    }

    void updateTest(TestView testView) {
      int row = testViews.indexOf(testView);
      if (row >= 0) {
        summaryTableModel.fireTableRowsUpdated(row, row);
        if (summaryTable.getSelectedRow() == row || summaryTable.getSelectedRow() < 0) {
          showSelectedDetails();
        }
      }
    }

    void selectMostRelevantTest() {
      int target = findFirstByStatus(ValidationStatus.FAIL);
      if (target < 0) {
        target = findFirstByStatus(ValidationStatus.WAIT);
      }
      if (target < 0 && !testViews.isEmpty()) {
        target = 0;
      }
      if (target >= 0) {
        summaryTable.setRowSelectionInterval(target, target);
        summaryTable.scrollRectToVisible(summaryTable.getCellRect(target, 0, true));
        showSelectedDetails();
      }
    }

    void focusSummaryTable() {
      summaryTable.requestFocusInWindow();
    }

    private int findFirstByStatus(ValidationStatus status) {
      for (int i = 0; i < testViews.size(); i++) {
        if (testViews.get(i).report.getOverallStatus() == status) {
          return i;
        }
      }
      return -1;
    }

    private void showSelectedDetails() {
      int viewRow = summaryTable.getSelectedRow();
      if (viewRow < 0 || viewRow >= testViews.size()) {
        detailsTitle.setText("Детали проверки");
        detailsTableModel.setReport(null);
        return;
      }

      int row = summaryTable.convertRowIndexToModel(viewRow);
      TestView testView = testViews.get(row);
      detailsTitle.setText(testView.title + " — " + testView.report.getOverallStatus().getDisplayName());
      detailsTableModel.setReport(testView.report);
    }
  }

  private class SummaryTableModel extends AbstractTableModel {

    private final String[] columns = {"Тест", "Статус", "OK", "WAIT", "FAIL"};

    @Override
    public int getRowCount() {
      return testViews.size();
    }

    @Override
    public int getColumnCount() {
      return columns.length;
    }

    @Override
    public String getColumnName(int column) {
      return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return switch (columnIndex) {
        case 1 -> ValidationStatus.class;
        case 2, 3, 4 -> Integer.class;
        default -> String.class;
      };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      TestView testView = testViews.get(rowIndex);
      return switch (columnIndex) {
        case 0 -> testView.title;
        case 1 -> testView.report.getOverallStatus();
        case 2 -> testView.report.count(ValidationStatus.PASS);
        case 3 -> testView.report.count(ValidationStatus.WAIT);
        case 4 -> testView.report.count(ValidationStatus.FAIL);
        default -> "";
      };
    }
  }

  private static class DetailsTableModel extends AbstractTableModel {

    private final String[] columns = {"Статус", "Проверка"};
    private ValidationReport report;

    void setReport(ValidationReport report) {
      this.report = report;
      fireTableDataChanged();
    }

    ValidationStatus getRowStatus(int rowIndex) {
      if (report == null || rowIndex < 0 || rowIndex >= report.getItems().size()) {
        return null;
      }
      return report.getItems().get(rowIndex).status;
    }

    @Override
    public int getRowCount() {
      return report == null ? 0 : report.getItems().size();
    }

    @Override
    public int getColumnCount() {
      return columns.length;
    }

    @Override
    public String getColumnName(int column) {
      return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return columnIndex == 0 ? ValidationStatus.class : String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      ValidationItem item = report.getItems().get(rowIndex);
      return columnIndex == 0 ? item.status : item.message;
    }
  }

  private static class StatusCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      ValidationStatus status = value instanceof ValidationStatus ? (ValidationStatus) value : null;
      setHorizontalAlignment(SwingConstants.CENTER);

      if (status == null) {
        setText("");
        return this;
      }

      setText(status.getDisplayName());
      setBackground(isSelected ? status.getBackground().darker() : status.getBackground());
      setForeground(status.getForeground());

      return this;
    }
  }

  private class SummaryRowRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      int modelRow = table.convertRowIndexToModel(row);
      ValidationStatus status = testViews.get(modelRow).report.getOverallStatus();
      setHorizontalAlignment(value instanceof Integer ? SwingConstants.CENTER : SwingConstants.LEFT);

      if (isSelected) {
        setBackground(status.getBackground());
        setForeground(status.getForeground());
      } else {
        setBackground(status.getLightBackground());
        setForeground(Color.BLACK);
      }

      return this;
    }
  }

  private static class DetailRowRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      ValidationStatus status = null;
      if (table.getModel() instanceof DetailsTableModel model) {
        status = model.getRowStatus(table.convertRowIndexToModel(row));
      }

      setHorizontalAlignment(SwingConstants.LEFT);

      if (status == null) {
        return this;
      }

      if (isSelected) {
        setBackground(status.getBackground());
        setForeground(status.getForeground());
      } else {
        setBackground(status.getLightBackground());
        setForeground(Color.BLACK);
      }

      return this;
    }
  }
}