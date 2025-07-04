package ru.dimension.ui.view.analyze.module;

import java.awt.Color;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JTabbedPane;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import org.jdesktop.swingx.JXTaskPane;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.column.DimensionValuesNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.analyze.chart.ChartConfig;
import ru.dimension.ui.view.analyze.chart.history.HistorySCP;
import ru.dimension.ui.view.analyze.chart.realtime.ClientRealtimeSCP;
import ru.dimension.ui.view.analyze.chart.realtime.ServerRealtimeSCP;
import ru.dimension.ui.view.analyze.timeseries.AnalyzeAnomalyPanel;
import ru.dimension.ui.view.analyze.timeseries.AnalyzeForecastPanel;
import ru.dimension.ui.model.view.AnalyzeType;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.view.analyze.chart.SCP;

@Log4j2
public class ChartAnalyzeModule extends JXTaskPane {

  private final SqlQueryState sqlQueryState;
  private final DStore dStore;
  private final Metric metric;
  private final QueryInfo queryInfo;
  private final ChartInfo chartInfo;
  private final AnalyzeType analyzeType;
  private final ProfileTaskQueryKey profileTaskQueryKey;
  private final Map<String, Color> seriesColorMap;
  private final Map.Entry<CProfile, List<String>> filter;
  private ChartConfig chartConfig;

  @Getter
  private SCP scp;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public ChartAnalyzeModule(SqlQueryState sqlQueryState,
                            DStore dStore,
                            Metric metric,
                            QueryInfo queryInfo,
                            ChartInfo chartInfo,
                            AnalyzeType analyzeType,
                            ProfileTaskQueryKey profileTaskQueryKey,
                            Map<String, Color> seriesColorMap) {
    this(sqlQueryState, dStore, metric, queryInfo, chartInfo, analyzeType, profileTaskQueryKey, seriesColorMap, null);
  }

  public ChartAnalyzeModule(SqlQueryState sqlQueryState,
                            DStore dStore,
                            Metric metric,
                            QueryInfo queryInfo,
                            ChartInfo chartInfo,
                            AnalyzeType analyzeType,
                            ProfileTaskQueryKey profileTaskQueryKey,
                            Map<String, Color> seriesColorMap,
                            Map.Entry<CProfile, List<String>> filter) {
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;
    this.metric = metric;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo;
    this.analyzeType = analyzeType;
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.seriesColorMap = seriesColorMap;
    this.filter = filter;

    initializeChartConfig();

    this.setAnimated(false);
  }

  private void initializeChartConfig() {
    chartConfig = new ChartConfig();

    chartConfig.setTitle("");
    chartConfig.setXAxisLabel(metric.getYAxis().getColName());
    chartConfig.setYAxisLabel("Value");

    chartConfig.setMetric(metric);
    chartConfig.setChartInfo(chartInfo);
    chartConfig.setQueryInfo(queryInfo);

    if (AnalyzeType.REAL_TIME.equals(analyzeType)) {
      chartConfig.setProcessType(ProcessType.REAL_TIME_ANALYZE);
    } else if (AnalyzeType.HISTORY.equals(analyzeType)) {
      chartConfig.setProcessType(ProcessType.HISTORY_ANALYZE);
    }
  }

  public JTabbedPane initializeUI() {
    JTabbedPane tabbedPane = new JTabbedPane();

    // Data Tab
    SCP chart = getStackChartPanel();
    chart.setPreferredSize(new Dimension(100, 270));
    chart.setMaximumSize(new Dimension(100, 270));

    // Anomaly tab
    AnalyzeAnomalyPanel anomalyPanel =
        new AnalyzeAnomalyPanel(GUIHelper.getJXTableCase(6, getTableColumnNames()),
                                chart.getSeriesColorMap(),
                                chart.getChartDataset());

    // Forecast tab
    AnalyzeForecastPanel forecastPanel =
        new AnalyzeForecastPanel(GUIHelper.getJXTableCase(6, getTableColumnNames()),
                                 chart.getSeriesColorMap(),
                                 chart.getChartDataset());

    chart.addChartListenerReleaseMouse(anomalyPanel);
    chart.addChartListenerReleaseMouse(forecastPanel);

    tabbedPane.addTab("Data", chart);
    tabbedPane.addTab("Anomaly", anomalyPanel);
    tabbedPane.addTab("Forecast", forecastPanel);

    return tabbedPane;
  }

  private SCP getStackChartPanel() {
    SCP chart = null;

    if (AnalyzeType.REAL_TIME.equals(analyzeType)) {
      if (GatherDataMode.BY_CLIENT_JDBC.equals(queryInfo.getGatherDataMode())) {
        chart = new ClientRealtimeSCP(
            sqlQueryState,
            dStore,
            buildChartConfig(),
            profileTaskQueryKey,
            filter
        );
      } else if (GatherDataMode.BY_SERVER_JDBC.equals(queryInfo.getGatherDataMode())) {
        chart = new ServerRealtimeSCP(
            sqlQueryState,
            dStore,
            buildChartConfig(),
            profileTaskQueryKey,
            filter
        );
      } else {
        chart = new ClientRealtimeSCP(
            sqlQueryState,
            dStore,
            buildChartConfig(),
            profileTaskQueryKey,
            filter
        );
      }
    } else if (AnalyzeType.HISTORY.equals(analyzeType)) {
      chart = new HistorySCP(
          dStore,
          buildChartConfig(),
          profileTaskQueryKey,
          filter
      );
    } else if (AnalyzeType.AD_HOC.equals(analyzeType)) {
      chart = new HistorySCP(
          dStore,
          buildChartConfig(),
          null,
          filter
      );
    }

    assert chart != null;
    chart.loadSeriesColor(metric, seriesColorMap);
    chart.initialize();

    this.scp = chart;

    return chart;
  }

  private ChartConfig buildChartConfig() {
    ChartConfig config = new ChartConfig();
    config.setTitle("");
    config.setXAxisLabel(metric.getYAxis().getColName());
    config.setYAxisLabel("Value");
    config.setMetric(metric);
    config.setChartInfo(chartInfo);
    config.setQueryInfo(queryInfo);
    if (AnalyzeType.REAL_TIME.equals(analyzeType)) {
      config.setProcessType(ProcessType.REAL_TIME_ANALYZE);
    } else if (AnalyzeType.HISTORY.equals(analyzeType)) {
      config.setProcessType(ProcessType.HISTORY_ANALYZE);
    } else if (AnalyzeType.AD_HOC.equals(analyzeType)) {
      config.setProcessType(ProcessType.ADHOC_ANALYZE);
    }
    return config;
  }

  private String[] getTableColumnNames() {
    return new String[]{
        DimensionValuesNames.COLOR.getColName(),
        DimensionValuesNames.VALUE.getColName()
    };
  }
}
