package ru.dimension.ui.view.analyze;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import javax.swing.JSplitPane;
import lombok.Getter;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.SourceConfig;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.AnalyzeType;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.analyze.module.ChartConfigModule;
import ru.dimension.ui.view.analyze.module.ChartListModule;
import ru.dimension.ui.view.analyze.module.DimensionModule;
import ru.dimension.ui.view.analyze.module.TopModule;
import ru.dimension.ui.view.analyze.router.Message;
import ru.dimension.ui.view.analyze.router.MessageRouter;
import ru.dimension.ui.view.analyze.router.MessageRouter.Action;
import ru.dimension.ui.view.analyze.router.MessageRouter.Destination;
import ru.dimension.ui.view.chart.HelperChart;

public class AnalyzeComponent implements HelperChart {

  private final EventListener eventListener;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  private final ChartInfo chartInfo;
  private final QueryInfo queryInfo;
  private final TableInfo tableInfo;
  private final ProfileTaskQueryKey profileTaskQueryKey;
  private final SourceConfig sourceConfig;
  private final AnalyzeType analyzeType;
  private final Metric metric;
  private final Map<String, Color> seriesColorMap;
  private final SeriesType seriesType;

  @Getter
  private JSplitPane mainSplitPane;

  private final MessageRouter router;

  public AnalyzeComponent(EventListener eventListener,
                          SqlQueryState sqlQueryState,
                          DStore dStore,
                          MessageRouter router,
                          QueryInfo queryInfo,
                          TableInfo tableInfo,
                          ChartInfo chartInfo,
                          ProfileTaskQueryKey profileTaskQueryKey,
                          SourceConfig sourceConfig,
                          AnalyzeType analyzeType,
                          Metric metric,
                          Map<String, Color> seriesColorMap,
                          SeriesType seriesType) {
    this.eventListener = eventListener;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;
    this.router = router;

    this.queryInfo = queryInfo;
    this.tableInfo = tableInfo;
    this.chartInfo = chartInfo;
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.sourceConfig = sourceConfig;
    this.analyzeType = analyzeType;
    this.metric = metric;
    this.seriesColorMap = seriesColorMap;
    this.seriesType = seriesType;

    router.clearRegistration();

    initializeComponents();
  }

  private void initializeComponents() {
    mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    mainSplitPane.setDividerLocation(250);
    mainSplitPane.setResizeWeight(0.3);

    JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    leftSplitPane.setDividerLocation(200);
    leftSplitPane.setResizeWeight(0.5);

    JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    rightSplitPane.setDividerLocation(50);
    rightSplitPane.setResizeWeight(0.1);

    DimensionModule dimensionModule = new DimensionModule(router, queryInfo, tableInfo, metric, sourceConfig, seriesColorMap);
    TopModule topModule = new TopModule(router, tableInfo, metric, seriesColorMap);
    ChartConfigModule configModule = new ChartConfigModule(router);
    ChartListModule chartListModule = new ChartListModule(router, sqlQueryState, dStore, metric, queryInfo, chartInfo, tableInfo, analyzeType, profileTaskQueryKey);

    leftSplitPane.setTopComponent(dimensionModule);
    leftSplitPane.setBottomComponent(topModule);
    rightSplitPane.setTopComponent(configModule);
    rightSplitPane.setBottomComponent(chartListModule);

    mainSplitPane.setLeftComponent(leftSplitPane);
    mainSplitPane.setRightComponent(rightSplitPane);

    router.registerReceiver(Destination.DIMENSION, dimensionModule);
    router.registerReceiver(Destination.TOP, topModule);
    router.registerReceiver(Destination.CHART_CONFIG, configModule);
    router.registerReceiver(Destination.CHART_LIST, chartListModule);

    initialize();

    if (AnalyzeType.REAL_TIME.equals(analyzeType)) {
      eventListener.addCollectStartStopAnalyzeListener(profileTaskQueryKey, chartListModule);
    }
  }

  private void initialize() {
    if (AnalyzeType.REAL_TIME.equals(analyzeType)) {
      if (SeriesType.COMMON.equals(seriesType)) {
        router.sendMessage(Message.builder()
                               .destination(Destination.CHART_LIST)
                               .action(Action.ADD_CHART)
                               .parameter("cProfile", this.metric.getYAxis())
                               .parameter("seriesColorMap", seriesColorMap)
                               .build());
      } else if (SeriesType.CUSTOM.equals(seriesType)) {
        router.sendMessage(Message.builder()
                               .destination(Destination.CHART_LIST)
                               .action(Action.ADD_CHART_FILTER)
                               .parameter("metric", this.metric)
                               .parameter("cProfileFilter", this.metric.getYAxis())
                               .parameter("filter", List.copyOf(seriesColorMap.keySet()))
                               .parameter("seriesColorMap", seriesColorMap)
                               .build());
        router.sendMessage(Message.builder()
                               .destination(Destination.DIMENSION)
                               .action(Action.SET_FILTER)
                               .parameter("cProfile",  this.metric.getYAxis())
                               .parameter("filter", List.copyOf(seriesColorMap.keySet()))
                               .build());
      }
    } else if (AnalyzeType.HISTORY.equals(analyzeType) | AnalyzeType.AD_HOC.equals(analyzeType)) {
      if (AnalyzeType.HISTORY.equals(analyzeType)) {
        ChartRange chartRange = getChartRange(chartInfo);
        chartInfo.setCustomBegin(chartRange.getBegin());
        chartInfo.setCustomEnd(chartRange.getEnd());
      }

      if (SeriesType.COMMON.equals(seriesType)) {
        router.sendMessage(Message.builder()
                               .destination(Destination.CHART_LIST)
                               .action(Action.ADD_CHART)
                               .parameter("cProfile", this.metric.getYAxis())
                               .parameter("seriesColorMap", seriesColorMap)
                               .build());
      } else {
        router.sendMessage(Message.builder()
                               .destination(Destination.CHART_LIST)
                               .action(Action.ADD_CHART_FILTER)
                               .parameter("metric", this.metric)
                               .parameter("cProfileFilter", this.metric.getYAxis())
                               .parameter("filter", List.copyOf(seriesColorMap.keySet()))
                               .parameter("seriesColorMap", seriesColorMap)
                               .build());
        router.sendMessage(Message.builder()
                               .destination(Destination.DIMENSION)
                               .action(Action.SET_FILTER)
                               .parameter("cProfile",  this.metric.getYAxis())
                               .parameter("filter", List.copyOf(seriesColorMap.keySet()))
                               .build());
      }
    }
  }
}
