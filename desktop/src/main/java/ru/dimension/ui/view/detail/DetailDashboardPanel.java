package ru.dimension.ui.view.detail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities; // Required for updating UI from a background thread
import javax.swing.border.EtchedBorder;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.module.PreviewModule;
import ru.dimension.ui.component.module.analyze.DetailAction;
import ru.dimension.ui.component.module.api.UnitView;
import ru.dimension.ui.component.module.chart.main.unit.HistoryUnitView;
import ru.dimension.ui.component.module.chart.preview.DetailChartContext;
import ru.dimension.ui.component.module.chart.report.ReportChartView;
import ru.dimension.ui.component.module.preview.spi.RunMode;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.date.DateLocale;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.detail.pivot.MainPivotDashboardPanel;
import ru.dimension.ui.view.detail.raw.RawDataDashboardPanel;
import ru.dimension.ui.view.detail.top.MainTopDashboardPanel;

@Log4j2
public class DetailDashboardPanel extends JPanel implements IDetailPanel, DetailAction {

  private static final String TAB_TITLE_TOP = "Top";
  private static final String TAB_TITLE_PIVOT = "Pivot";
  private static final String TAB_TITLE_RAW = "Raw";
  private static final String TAB_TITLE_BREAKDOWN = "Breakdown";

  private JPanel mainPanel;

  private final UnitView unitView;
  private final ChartKey chartKey;
  private final QueryInfo queryInfo;
  private final ChartInfo chartInfo;
  private final TableInfo tableInfo;
  private final MessageBroker.Panel panel;
  private final Metric metric;
  private final CProfile cProfile;
  private final ExecutorService executorService;
  private final Map<String, Color> seriesColorMap;
  @Setter
  private SeriesType seriesType;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  private Map<CProfile, LinkedHashSet<String>> topMapSelected;

  private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

  public DetailDashboardPanel(UnitView unitView,
                              ChartKey chartKey,
                              QueryInfo queryInfo,
                              ChartInfo chartInfo,
                              TableInfo tableInfo,
                              Metric metric,
                              Map<String, Color> seriesColorMap,
                              MessageBroker.Panel panel,
                              SeriesType seriesType,
                              SqlQueryState sqlQueryState,
                              DStore dStore,
                              Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    this.unitView = unitView;
    this.chartKey = chartKey;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo;
    this.tableInfo = tableInfo;
    this.metric = metric;
    this.cProfile = metric.getYAxis();
    this.seriesColorMap = seriesColorMap;
    this.panel = panel;
    this.seriesType = seriesType;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;
    this.topMapSelected = topMapSelected;

    this.executorService = Executors.newSingleThreadExecutor();

    initializeUI();
  }

  private void initializeUI() {
    this.setLayout(new BorderLayout());
    this.setBorder(new EtchedBorder());

    this.mainPanel = new JPanel();
    this.mainPanel.setLayout(new GridLayout(1, 1, 3, 3));

    this.add(this.mainPanel, BorderLayout.CENTER);
  }

  @Override
  public void cleanMainPanel() {
    executorService.submit(this::clearPanel);
  }

  private void clearPanel() {
    mainPanel.removeAll();
    mainPanel.repaint();
    mainPanel.revalidate();
  }

  @Override
  public void loadDataToDetail(long begin, long end) {
    executorService.submit(() -> loadDetailData(begin, end));
  }

  private void loadDetailData(long begin, long end) {
    mainPanel.removeAll();
    mainPanel.add(ProgressBarHelper.createProgressBar("Loading, please wait..."));
    mainPanel.repaint();
    mainPanel.revalidate();

    try {
      JTabbedPane tabs = new JTabbedPane();

      Map<CProfile, LinkedHashSet<String>> actualTopMapSelected = resolveTopMapSelected();

      addTopTab(tabs, begin, end, actualTopMapSelected);
      addPivotTab(tabs, begin, end);
      addRawTab(tabs, begin, end);
      addRangeTab(tabs, begin, end);
      addBreakdownTab(tabs, begin, end, actualTopMapSelected);

      displayTabs(tabs);
    } catch (Exception exception) {
      log.catching(exception);
    }
  }

  private Map<CProfile, LinkedHashSet<String>> resolveTopMapSelected() {
    Map<CProfile, LinkedHashSet<String>> actualTopMapSelected = topMapSelected;
    if (SeriesType.CUSTOM.equals(seriesType)) {
      if (actualTopMapSelected == null) {
        actualTopMapSelected = new HashMap<>();
      }
      actualTopMapSelected.computeIfAbsent(cProfile, k -> new LinkedHashSet<>(seriesColorMap.keySet()));
    }
    return actualTopMapSelected;
  }

  private void addTopTab(JTabbedPane tabs, long begin, long end,
                         Map<CProfile, LinkedHashSet<String>> actualTopMapSelected) {
    MainTopDashboardPanel mainTopPanel = new MainTopDashboardPanel(
        dStore, scheduledExecutorService, tableInfo, metric, cProfile,
        begin, end, seriesColorMap, seriesType, actualTopMapSelected);
    tabs.add(TAB_TITLE_TOP, mainTopPanel);
  }

  private void addPivotTab(JTabbedPane tabs, long begin, long end) {
    if (GroupFunction.COUNT.equals(metric.getGroupFunction())) {
      if (Objects.isNull(seriesType) || SeriesType.COMMON.equals(seriesType)) {
        MainPivotDashboardPanel mainPivotPanel = new MainPivotDashboardPanel(
            dStore, scheduledExecutorService, tableInfo, metric,
            cProfile, begin, end, seriesColorMap);
        tabs.add(TAB_TITLE_PIVOT, mainPivotPanel);
      }
    }
  }

  private void addRawTab(JTabbedPane tabs, long begin, long end) {
    JPanel rawPlaceholder = new JPanel(new BorderLayout());
    tabs.addTab(TAB_TITLE_RAW, rawPlaceholder);

    tabs.addChangeListener(e -> handleRawTabSelection(e, rawPlaceholder, begin, end));
  }

  private void addRangeTab(JTabbedPane tabs, long begin, long end) {
    String rangeSeparator = DateHelper.getDateFormattedRange(DateLocale.RU, begin, end, false);

    int separatorIndex = tabs.getTabCount();
    tabs.addTab("", new JPanel());
    tabs.setEnabledAt(separatorIndex, false);
    tabs.setTabComponentAt(separatorIndex, createTextSeparator(rangeSeparator));
  }

  private void addBreakdownTab(JTabbedPane tabs, long begin, long end,
                               Map<CProfile, LinkedHashSet<String>> actualTopMapSelected) {
    JPanel breakdownPlaceholder = new JPanel(new BorderLayout());
    tabs.addTab(TAB_TITLE_BREAKDOWN, breakdownPlaceholder);

    ChartInfo chartInfoCopy = createCustomRangeChartInfo(begin, end);
    ChartConfig config = buildChartConfig(chartInfoCopy);
    RunMode runMode = resolveRunMode();
    DetailChartContext detailContext = new DetailChartContext(seriesColorMap, actualTopMapSelected);

    tabs.addChangeListener(e -> handleTabSelection(e, breakdownPlaceholder, chartInfoCopy, config, runMode, detailContext));
  }

  private ChartInfo createCustomRangeChartInfo(long begin, long end) {
    ChartInfo chartInfoCopy = chartInfo.copy();
    chartInfoCopy.setRangeHistory(RangeHistory.CUSTOM);
    chartInfoCopy.setCustomBegin(begin);
    chartInfoCopy.setCustomEnd(end);
    return chartInfoCopy;
  }

  private RunMode resolveRunMode() {
    if (unitView instanceof HistoryUnitView || unitView instanceof ReportChartView) {
      return RunMode.HISTORY;
    }
    return RunMode.REALTIME;
  }

  private void handleRawTabSelection(javax.swing.event.ChangeEvent e, JPanel rawPlaceholder, long begin, long end) {
    JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
    int selectedIndex = sourceTabbedPane.getSelectedIndex();

    if (selectedIndex == -1) {
      return;
    }

    boolean isRawTab = TAB_TITLE_RAW.equals(sourceTabbedPane.getTitleAt(selectedIndex))
        && sourceTabbedPane.getComponentAt(selectedIndex) == rawPlaceholder;

    if (isRawTab) {
      initializeRawTab(sourceTabbedPane, selectedIndex, rawPlaceholder, begin, end);
    }
  }

  private void handleTabSelection(javax.swing.event.ChangeEvent e, JPanel breakdownPlaceholder,
                                  ChartInfo chartInfoCopy, ChartConfig config, RunMode runMode,
                                  DetailChartContext detailContext) {
    JTabbedPane sourceTabbedPane = (JTabbedPane) e.getSource();
    int selectedIndex = sourceTabbedPane.getSelectedIndex();

    if (selectedIndex == -1) {
      return;
    }

    boolean isBreakdownTab = TAB_TITLE_BREAKDOWN.equals(sourceTabbedPane.getTitleAt(selectedIndex))
        && sourceTabbedPane.getComponentAt(selectedIndex) == breakdownPlaceholder;

    if (isBreakdownTab) {
      initializeBreakdownTab(sourceTabbedPane, selectedIndex, breakdownPlaceholder, chartInfoCopy, config, runMode, detailContext);
    }
  }

  private void initializeRawTab(JTabbedPane sourceTabbedPane, int selectedIndex, JPanel rawPlaceholder,
                                long begin, long end) {
    log.info("Lazy initializing '{}' tab content in background.", TAB_TITLE_RAW);

    rawPlaceholder.add(ProgressBarHelper.createProgressBar("Initializing..."), BorderLayout.CENTER);
    rawPlaceholder.revalidate();
    rawPlaceholder.repaint();

    scheduledExecutorService.submit(() -> {
      try {
        boolean isRealtimePanel = MessageBroker.Panel.REALTIME.equals(panel);
        final RawDataDashboardPanel rawDataPanel = new RawDataDashboardPanel(
            dStore, tableInfo, cProfile, begin, end, !isRealtimePanel);

        SwingUtilities.invokeLater(() -> sourceTabbedPane.setComponentAt(selectedIndex, rawDataPanel));
      } catch (Exception e) {
        log.error("Failed to initialize Raw tab content", e);
        SwingUtilities.invokeLater(() -> {
          JPanel errorPanel = new JPanel();
          errorPanel.add(new JLabel("Error loading data. See logs for details."));
          sourceTabbedPane.setComponentAt(selectedIndex, errorPanel);
        });
      }
    });
  }

  private void initializeBreakdownTab(JTabbedPane sourceTabbedPane, int selectedIndex, JPanel breakdownPlaceholder,
                                      ChartInfo chartInfoCopy, ChartConfig config, RunMode runMode,
                                      DetailChartContext detailContext) {
    log.info("Lazy initializing '{}' tab content.", TAB_TITLE_BREAKDOWN);

    breakdownPlaceholder.add(ProgressBarHelper.createProgressBar("Initializing..."), BorderLayout.CENTER);
    breakdownPlaceholder.revalidate();
    breakdownPlaceholder.repaint();

    ProfileTaskQueryKey key = config.getChartKey().getProfileTaskQueryKey();

    PreviewModule previewModule = new PreviewModule(
        runMode, key, metric, queryInfo, chartInfoCopy, tableInfo, sqlQueryState, dStore, detailContext);

    sourceTabbedPane.setComponentAt(selectedIndex, previewModule.getComponent());
  }

  private void displayTabs(JTabbedPane tabs) {
    mainPanel.removeAll();
    mainPanel.repaint();
    mainPanel.revalidate();
    mainPanel.add(tabs);
  }

  public void updateSeriesColor(Map<CProfile, LinkedHashSet<String>> topMapSelected,
                                Map<String, Color> newSeriesColorMap) {
    this.topMapSelected = topMapSelected;
    seriesColorMap.clear();
    seriesColorMap.putAll(newSeriesColorMap);
  }

  private ChartConfig buildChartConfig(ChartInfo chartInfo) {
    ChartConfig config = new ChartConfig();
    config.setTitle("");
    config.setChartKey(chartKey);
    config.setXAxisLabel(metric.getYAxis().getColName());
    config.setYAxisLabel("Value");
    config.setMetric(metric);
    config.setChartInfo(chartInfo);
    config.setQueryInfo(queryInfo);
    return config;
  }

  private static JComponent createTextSeparator(String text) {
    return new JPanel(new BorderLayout()) {
      {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        JLabel label = new JLabel(text);
        label.setForeground(new Color(0x0A8D0A));
        label.setFont(scaleFontSize(label.getFont()));

        add(label, BorderLayout.WEST);

        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 20));
        add(separator, BorderLayout.EAST);
      }
    };
  }

  private static Font scaleFontSize(Font currentFont) {
    float newSize = currentFont.getSize() * 1.2f;
    return currentFont.deriveFont(newSize);
  }
}