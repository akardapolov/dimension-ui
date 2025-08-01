package ru.dimension.ui.view.detail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.column.DimensionValuesNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.date.DateLocale;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.view.analyze.DetailAction;
import ru.dimension.ui.view.analyze.chart.ChartConfig;
import ru.dimension.ui.view.analyze.chart.SCP;
import ru.dimension.ui.view.analyze.chart.history.HistorySCP;
import ru.dimension.ui.view.analyze.timeseries.AnalyzeAnomalyPanel;
import ru.dimension.ui.view.analyze.timeseries.AnalyzeForecastPanel;
import ru.dimension.ui.view.detail.pivot.MainPivotDashboardPanel;
import ru.dimension.ui.view.detail.raw.RawDataDashboardPanel;
import ru.dimension.ui.view.detail.top.MainTopDashboardPanel;

@Log4j2
public class DetailDashboardPanel extends JPanel implements IDetailPanel, DetailAction {

  private JPanel mainPanel;

  private final QueryInfo queryInfo;
  private final TableInfo tableInfo;
  private final ProcessType processType;
  private final Metric metric;
  private final CProfile cProfile;
  private final ChartType chartType;
  private final ExecutorService executorService;
  private final Map<String, Color> seriesColorMap;
  private final SeriesType seriesType;
  private final DStore dStore;

  private Entry<CProfile, List<String>> filter;

  private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);;

  public DetailDashboardPanel(DStore dStore,
                              QueryInfo queryInfo,
                              TableInfo tableInfo,
                              Metric metric,
                              Map<String, Color> seriesColorMap,
                              ProcessType processType,
                              SeriesType seriesType,
                              Entry<CProfile, List<String>> filter) {
    this.dStore = dStore;
    this.queryInfo = queryInfo;
    this.tableInfo = tableInfo;
    this.metric = metric;
    this.cProfile = metric.getYAxis();
    this.chartType = metric.getChartType();
    this.seriesColorMap = seriesColorMap;
    this.processType = processType;
    this.seriesType = seriesType;
    this.filter = filter;

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
    executorService.submit(() -> {
      mainPanel.removeAll();
      mainPanel.add(ProgressBarHelper.createProgressBar("Loading, please wait..."));
      mainPanel.repaint();
      mainPanel.revalidate();

      try {
        JTabbedPane mainJTabbedPane = new JTabbedPane();

        if (!MetricFunction.COUNT.equals(metric.getMetricFunction())) {
          seriesColorMap.put(metric.getYAxis().getColName(), new Color(255, 93, 93));
        }

        MainTopDashboardPanel mainTopPanel = new MainTopDashboardPanel(
            dStore, scheduledExecutorService, tableInfo, metric, cProfile,
            begin, end, seriesColorMap, seriesType, filter);
        mainJTabbedPane.add("Top", mainTopPanel);

        if (MetricFunction.COUNT.equals(metric.getMetricFunction())) {
          if (Objects.isNull(seriesType) || SeriesType.COMMON.equals(seriesType)) {
            MainPivotDashboardPanel mainPivotPanel = new MainPivotDashboardPanel(
                dStore, scheduledExecutorService, tableInfo, metric,
                cProfile, begin, end, seriesColorMap);
            mainJTabbedPane.add("Pivot", mainPivotPanel);
          }
        }

        RawDataDashboardPanel rawDataPanel;
        if (ProcessType.REAL_TIME.equals(processType)) {
          rawDataPanel = new RawDataDashboardPanel(dStore, tableInfo, cProfile, begin, end, false);
        } else {
          rawDataPanel = new RawDataDashboardPanel(dStore, tableInfo, cProfile, begin, end, true);
        }
        mainJTabbedPane.add("Raw", rawDataPanel);

        // Analyze range >>
        String range = DateHelper.getDateFormattedRange(DateLocale.RU, begin, end, false);

        int separatorIndex = mainJTabbedPane.getTabCount();
        mainJTabbedPane.addTab("", new JPanel());
        mainJTabbedPane.setEnabledAt(separatorIndex, false);
        mainJTabbedPane.setTabComponentAt(separatorIndex, createTextSeparator(range));

        Map.Entry<CProfile, List<String>> filter = null;

        if (SeriesType.CUSTOM.equals(seriesType)) {
          filter = Map.entry(cProfile, seriesColorMap.keySet().stream().toList());
        }

        ChartInfo chartInfo = new ChartInfo();
        chartInfo.setCustomBegin(begin);
        chartInfo.setCustomEnd(end);
        SCP chart = new HistorySCP(dStore, buildChartConfig(chartInfo), null, filter);
        chart.loadSeriesColor(metric, seriesColorMap);
        chart.initialize();

        AnalyzeAnomalyPanel anomalyPanel =
            new AnalyzeAnomalyPanel(GUIHelper.getJXTableCase(6, getTableColumnNames()),
                                    chart.getSeriesColorMap(),
                                    chart.getChartDataset());
        anomalyPanel.hideSettings();

        AnalyzeForecastPanel forecastPanel =
            new AnalyzeForecastPanel(GUIHelper.getJXTableCase(6, getTableColumnNames()),
                                     chart.getSeriesColorMap(),
                                     chart.getChartDataset());
        forecastPanel.hideSettings();

        mainJTabbedPane.add("Data", chart);
        mainJTabbedPane.add("Anomaly", anomalyPanel);
        mainJTabbedPane.add("Forecast", forecastPanel);

        mainPanel.removeAll();
        mainPanel.repaint();
        mainPanel.revalidate();

        mainPanel.add(mainJTabbedPane);

      } catch (Exception exception) {
        log.catching(exception);
      }
    });
  }

  public void updateSeriesColor(Entry<CProfile, List<String>> filter, Map<String, Color> newSeriesColorMap) {
    this.filter = filter;

    seriesColorMap.clear();
    seriesColorMap.putAll(newSeriesColorMap);
  }

  private ChartConfig buildChartConfig(ChartInfo chartInfo) {
    ChartConfig config = new ChartConfig();
    config.setTitle("");
    config.setXAxisLabel(metric.getYAxis().getColName());
    config.setYAxisLabel("Value");
    config.setMetric(metric);
    config.setChartInfo(chartInfo);
    config.setQueryInfo(queryInfo);
    config.setProcessType(ProcessType.HISTORY_ANALYZE);
    return config;
  }

  private String[] getTableColumnNames() {
    return new String[]{
        DimensionValuesNames.COLOR.getColName(),
        DimensionValuesNames.VALUE.getColName()
    };
  }


  private static JComponent createTextSeparator(String text) {
    return new JPanel(new BorderLayout()) {
      {
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // Text label
        JLabel label = new JLabel(text);
        label.setForeground(new Color(0x0A8D0A));
        add(label, BorderLayout.WEST);

        // Vertical separator
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 20));
        add(separator, BorderLayout.EAST);
      }
    };
  }
}