package ru.dimension.ui.view.detail;

import static ru.dimension.ui.laf.LafColorGroup.REPORT;

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
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.VerticalLayout;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.history.HistoryAdHocSCP;
import ru.dimension.ui.component.module.PreviewModule;
import ru.dimension.ui.component.module.analyze.DetailAction;
import ru.dimension.ui.component.module.analyze.timeseries.AnalyzeAnomalyPanel;
import ru.dimension.ui.component.module.analyze.timeseries.AnalyzeForecastPanel;
import ru.dimension.ui.component.module.chart.preview.DetailChartContext;
import ru.dimension.ui.component.module.preview.spi.RunMode;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartType;
import ru.dimension.ui.model.column.DimensionValuesNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.date.DateLocale;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.RangeHistory;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.view.detail.pivot.MainPivotDashboardPanel;
import ru.dimension.ui.view.detail.raw.RawDataDashboardPanel;
import ru.dimension.ui.view.detail.top.MainTopDashboardPanel;

@Log4j2
public class DetailAdHocPanel extends JPanel implements IDetailPanel, DetailAction {

  private static final String TAB_TITLE_TOP = "Top";
  private static final String TAB_TITLE_PIVOT = "Pivot";
  private static final String TAB_TITLE_RAW = "Raw";
  private static final String TAB_TITLE_BREAKDOWN = "Breakdown";
  private static final String TAB_TITLE_INSIGHT = "Insight";

  private final JPanel mainPanel;

  private final QueryInfo queryInfo;
  private final ChartInfo chartInfo;
  private final TableInfo tableInfo;
  private final MessageBroker.Panel panel;
  private final Metric metric;
  private final CProfile cProfile;
  private final ChartType chartType;
  private final ExecutorService executorService;
  private final Map<String, Color> seriesColorMap;
  private SeriesType seriesType;
  private final AdHocKey adHocKey;
  private final DStore dStore;

  private Map<CProfile, LinkedHashSet<String>> topMapSelected;

  private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

  public DetailAdHocPanel(DStore dStore,
                          QueryInfo queryInfo,
                          ChartInfo chartInfo,
                          TableInfo tableInfo,
                          Metric metric,
                          Map<String, Color> seriesColorMap,
                          MessageBroker.Panel panel,
                          SeriesType seriesType,
                          AdHocKey adHocKey,
                          Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    this.dStore = dStore;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo;
    this.tableInfo = tableInfo;
    this.metric = metric;
    this.cProfile = metric.getYAxis();
    this.chartType = metric.getChartType();
    this.seriesColorMap = seriesColorMap;
    this.panel = panel;
    this.seriesType = seriesType;
    this.adHocKey = adHocKey;
    this.topMapSelected = topMapSelected;

    this.executorService = Executors.newSingleThreadExecutor();

    this.setLayout(new BorderLayout());
    this.setBorder(new EtchedBorder());

    this.mainPanel = new JPanel();
    this.mainPanel.setLayout(new GridLayout(1, 1, 3, 3));

    this.add(this.mainPanel, BorderLayout.CENTER);
  }

  @Override
  public void cleanMainPanel() {
    executorService.submit(() -> {
      mainPanel.removeAll();
      mainPanel.repaint();
      mainPanel.revalidate();
    });
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

      JPanel rawPlaceholder = new JPanel(new BorderLayout());
      JPanel breakdownPlaceholder = new JPanel(new BorderLayout());
      JPanel insightPlaceholder = new JPanel(new BorderLayout());

      addTopTab(tabs, begin, end, actualTopMapSelected);
      addPivotTab(tabs, begin, end);
      tabs.addTab(TAB_TITLE_RAW, rawPlaceholder);

      addRangeTab(tabs, begin, end);

      tabs.addTab(TAB_TITLE_BREAKDOWN, breakdownPlaceholder);
      tabs.addTab(TAB_TITLE_INSIGHT, insightPlaceholder);

      tabs.addChangeListener(e -> {
        JTabbedPane source = (JTabbedPane) e.getSource();
        int selectedIndex = source.getSelectedIndex();
        if (selectedIndex == -1) {
          return;
        }

        if (source.getComponentAt(selectedIndex) == rawPlaceholder) {
          initializeRawTab(source, selectedIndex, rawPlaceholder, begin, end);
        } else if (source.getComponentAt(selectedIndex) == breakdownPlaceholder) {
          initializeBreakdownTab(source, selectedIndex, breakdownPlaceholder, begin, end, actualTopMapSelected);
        } else if (source.getComponentAt(selectedIndex) == insightPlaceholder) {
          initializeInsightTab(source, selectedIndex, insightPlaceholder, begin, end, actualTopMapSelected);
        }
      });

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

  private void addTopTab(JTabbedPane tabs, long begin, long end, Map<CProfile, LinkedHashSet<String>> actualTopMapSelected) {
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

  private void addRangeTab(JTabbedPane tabs, long begin, long end) {
    String rangeSeparator = DateHelper.getDateFormattedRange(DateLocale.RU, begin, end, false);
    int separatorIndex = tabs.getTabCount();
    tabs.addTab("", new JPanel());
    tabs.setEnabledAt(separatorIndex, false);
    tabs.setTabComponentAt(separatorIndex, createTextSeparator(rangeSeparator));
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
        final RawDataDashboardPanel rawDataPanel = new RawDataDashboardPanel(dStore, tableInfo, cProfile, begin, end, !isRealtimePanel);

        SwingUtilities.invokeLater(() -> sourceTabbedPane.setComponentAt(selectedIndex, rawDataPanel));
      } catch (Exception e) {
        log.error("Failed to initialize Raw tab content", e);
        SwingUtilities.invokeLater(() -> {
          JPanel errorPanel = new JPanel(new BorderLayout());
          errorPanel.add(new JLabel("Error loading data. See logs for details."), BorderLayout.CENTER);
          sourceTabbedPane.setComponentAt(selectedIndex, errorPanel);
        });
      }
    });
  }

  private void initializeBreakdownTab(JTabbedPane sourceTabbedPane, int selectedIndex, JPanel breakdownPlaceholder,
                                      long begin, long end, Map<CProfile, LinkedHashSet<String>> actualTopMapSelected) {
    log.info("Lazy initializing '{}' tab content.", TAB_TITLE_BREAKDOWN);

    breakdownPlaceholder.add(ProgressBarHelper.createProgressBar("Initializing..."), BorderLayout.CENTER);
    breakdownPlaceholder.revalidate();
    breakdownPlaceholder.repaint();

    ChartInfo chartInfoCopy = createCustomRangeChartInfo(begin, end);

    RunMode runMode = RunMode.HISTORY;
    DetailChartContext detailContext = new DetailChartContext(seriesColorMap, actualTopMapSelected);

    PreviewModule previewModule =
        new PreviewModule(runMode, adHocKey, metric, queryInfo, chartInfoCopy, tableInfo, dStore, detailContext);

    sourceTabbedPane.setComponentAt(selectedIndex, previewModule.getComponent());
  }

  private ChartInfo createCustomRangeChartInfo(long begin, long end) {
    ChartInfo chartInfoCopy = chartInfo.copy();
    chartInfoCopy.setRangeHistory(RangeHistory.CUSTOM);
    chartInfoCopy.setCustomBegin(begin);
    chartInfoCopy.setCustomEnd(end);
    return chartInfoCopy;
  }

  private void initializeInsightTab(JTabbedPane sourceTabbedPane, int selectedIndex, JPanel insightPlaceholder,
                                    long begin, long end, Map<CProfile, LinkedHashSet<String>> actualTopMapSelected) {
    log.info("Lazy initializing '{}' tab content.", TAB_TITLE_INSIGHT);

    insightPlaceholder.add(ProgressBarHelper.createProgressBar("Initializing..."), BorderLayout.CENTER);
    insightPlaceholder.revalidate();
    insightPlaceholder.repaint();

    scheduledExecutorService.submit(() -> {
      try {
        ChartInfo chartInfoCopy = chartInfo.copy();
        chartInfoCopy.setCustomBegin(begin);
        chartInfoCopy.setCustomEnd(end);

        ChartConfig config = buildChartConfig(chartInfoCopy);
        ProfileTaskQueryKey key = new ProfileTaskQueryKey(0, 0, 0);

        // Data
        SCP chart = new HistoryAdHocSCP(dStore, config, key, actualTopMapSelected);
        chart.loadSeriesColor(metric, seriesColorMap);
        chart.initialize();

        // Anomaly
        AnalyzeAnomalyPanel anomalyPanel =
            new AnalyzeAnomalyPanel(GUIHelper.getJXTableCase(6, getTableColumnNames()),
                                    chart.getSeriesColorMap(),
                                    chart.getChartDataset());
        anomalyPanel.hideSettings();

        // Forecast
        AnalyzeForecastPanel forecastPanel =
            new AnalyzeForecastPanel(GUIHelper.getJXTableCase(6, getTableColumnNames()),
                                     chart.getSeriesColorMap(),
                                     chart.getChartDataset());
        forecastPanel.hideSettings();

        Dimension dimension = new Dimension(100, 200);

        JXTaskPaneContainer container = new JXTaskPaneContainer();
        LaF.setBackgroundColor(REPORT, container);
        container.setBackgroundPainter(null);

        chart.setPreferredSize(dimension);
        chart.setMaximumSize(dimension);

        chart.setBorder(GUIHelper.getConfigureBorder(1));
        anomalyPanel.setBorder(GUIHelper.getConfigureBorder(1));
        forecastPanel.setBorder(GUIHelper.getConfigureBorder(1));

        container.add(createTaskPane("Data", chart));
        container.add(createTaskPane("Anomaly", anomalyPanel));
        container.add(createTaskPane("Forecast", forecastPanel));

        JPanel cardPanel = new JPanel(new VerticalLayout());
        LaF.setBackgroundColor(REPORT, cardPanel);
        cardPanel.add(container);

        JScrollPane scroll = new JScrollPane(cardPanel);
        GUIHelper.setScrolling(scroll);

        SwingUtilities.invokeLater(() -> sourceTabbedPane.setComponentAt(selectedIndex, scroll));

      } catch (Exception ex) {
        log.catching(ex);
        SwingUtilities.invokeLater(() -> {
          JPanel errorPanel = new JPanel(new BorderLayout());
          errorPanel.add(new JLabel("Error loading insights: " + ex.getMessage()), BorderLayout.CENTER);
          sourceTabbedPane.setComponentAt(selectedIndex, errorPanel);
        });
      }
    });
  }

  private void displayTabs(JTabbedPane tabs) {
    mainPanel.removeAll();
    mainPanel.add(tabs);
    mainPanel.revalidate();
    mainPanel.repaint();
  }

  public void updateSeriesColor(Map<CProfile, LinkedHashSet<String>> topMapSelected, Map<String, Color> newSeriesColorMap) {
    this.topMapSelected = topMapSelected;

    seriesColorMap.clear();
    seriesColorMap.putAll(newSeriesColorMap);
  }

  public void setSeriesType(SeriesType seriesType) {
    this.seriesType = seriesType;
  }

  private ChartConfig buildChartConfig(ChartInfo chartInfo) {
    ChartConfig config = new ChartConfig();
    config.setTitle("");
    config.setXAxisLabel(metric.getYAxis().getColName());
    config.setYAxisLabel("Value");
    config.setMetric(metric);
    config.setChartInfo(chartInfo);
    config.setQueryInfo(queryInfo);
    return config;
  }

  private String[] getTableColumnNames() {
    return new String[]{
        DimensionValuesNames.COLOR.getColName(),
        DimensionValuesNames.VALUE.getColName()
    };
  }

  private JXTaskPane createTaskPane(String title, JComponent content) {
    JXTaskPane pane = new JXTaskPane();
    ((JComponent) pane.getContentPane()).setBorder(BorderFactory.createEmptyBorder());
    pane.setTitle(title);
    pane.setAnimated(false);
    pane.add(content);
    return pane;
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