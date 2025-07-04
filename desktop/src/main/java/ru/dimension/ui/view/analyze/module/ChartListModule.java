package ru.dimension.ui.view.analyze.module;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.profile.CProfile;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.VerticalLayout;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.router.listener.CollectStartStopListener;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.analyze.model.ChartCardState;
import ru.dimension.ui.view.analyze.model.ChartLegendState;
import ru.dimension.ui.view.analyze.router.Message;
import ru.dimension.ui.view.analyze.router.MessageAction;
import ru.dimension.ui.view.analyze.router.MessageRouter;
import ru.dimension.ui.view.analyze.router.MessageRouter.Action;
import ru.dimension.ui.model.view.AnalyzeType;

@Log4j2
public class ChartListModule extends JPanel implements MessageAction, CollectStartStopListener {

  private final MessageRouter router;

  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  protected final Metric metric;
  private final QueryInfo queryInfo;
  private final ChartInfo chartInfo;
  private final TableInfo tableInfo;
  private final AnalyzeType analyzeType;
  private final ProfileTaskQueryKey profileTaskQueryKey;

  private ChartLegendState chartLegendState = ChartLegendState.SHOW;
  private ChartCardState chartCardState = ChartCardState.EXPAND_ALL;

  private final JScrollPane cardScrollPane;
  private final JPanel cardPanel;

  private final JXTaskPaneContainer cardContainer;
  private final Map<CProfile, ChartAnalyzeModule> chartPanes = new ConcurrentHashMap<>();
  private final Map<String, ChartAnalyzeModule> chartPanesFilter = new ConcurrentHashMap<>();

  private final Map<CProfile, LinkedHashSet<String>> chartFilters = new ConcurrentHashMap<>();

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public ChartListModule(MessageRouter router,
                         SqlQueryState sqlQueryState,
                         DStore dStore,
                         Metric metric,
                         QueryInfo queryInfo,
                         ChartInfo chartInfo,
                         TableInfo tableInfo,
                         AnalyzeType analyzeType,
                         ProfileTaskQueryKey profileTaskQueryKey) {
    this.router = router;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;
    this.metric = metric;
    this.queryInfo = queryInfo;
    this.chartInfo = chartInfo;
    this.tableInfo = tableInfo;
    this.analyzeType = analyzeType;
    this.profileTaskQueryKey = profileTaskQueryKey;

    cardContainer = new JXTaskPaneContainer();
    LaF.setBackgroundColor(LafColorGroup.REPORT, cardContainer);
    cardContainer.setBackgroundPainter(null);

    this.cardScrollPane = new JScrollPane();
    GUIHelper.setScrolling(cardScrollPane);

    cardPanel = new JPanel(new VerticalLayout());
    LaF.setBackgroundColor(LafColorGroup.REPORT, cardPanel);

    cardPanel.add(cardContainer);

    cardScrollPane.setViewportView(cardPanel);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    gbl.row().cellXRemainder(cardScrollPane).fillXY();
    gbl.done();
  }

  @Override
  public void receive(Message message) {
    switch (message.getAction()) {
      case Action.ADD_CHART -> {
        CProfile cProfile = message.getParameters().get("cProfile");
        Map<String, Color> seriesColorMap = message.getParameters().get("seriesColorMap");
        addChartCard(cProfile, seriesColorMap);
        logChartAction(message.getAction(), cProfile);
      }

      case Action.REMOVE_CHART -> {
        CProfile cProfile = message.getParameters().get("cProfile");
        removeChartCard(cProfile);
        logChartAction(message.getAction(), cProfile);
      }

      case Action.REMOVE_ALL_CHART -> {
        chartPanes.forEach((key, value) -> removeChartCard(key));
        chartPanesFilter.forEach((key, value) -> removeChartFilterCard(key));
        log.info("Message action: " + message.getAction());
      }

      case Action.ADD_CHART_FILTER -> {
        Metric metric = message.getParameters().get("metric");
        CProfile cProfileFilter = message.getParameters().get("cProfileFilter");
        List<String> filter = message.getParameters().get("filter");
        Map<String, Color> seriesColorMap = message.getParameters().get("seriesColorMap");
        addChartFilterCard(metric, cProfileFilter, filter, seriesColorMap);
        logChartFilterAction(message.getAction(), metric, cProfileFilter, filter);
      }

      case Action.REMOVE_CHART_FILTER -> {
        Metric metric = message.getParameters().get("metric");
        CProfile cProfileFilter = message.getParameters().get("cProfileFilter");
        List<String> filter = message.getParameters().get("filter");
        removeChartFilterCard(metric, cProfileFilter, filter);
        logChartFilterAction(message.getAction(), metric, cProfileFilter, filter);
      }

      case Action.SET_CHART_LEGEND_STATE -> {
        chartLegendState = message.getParameters().get("chartLegendState");
        updateLegendVisibility();
        log.info("Message action: " + message.getAction() + " for " + chartLegendState);
      }

      case Action.SET_CHART_CARD_STATE -> {
        chartCardState = message.getParameters().get("chartCardState");
        expandCollapseChartCard();
        log.info("Message action: " + message.getAction() + " for " + chartCardState);
      }

      case Action.SET_BEGIN_END -> {
        long begin = message.getParameters().get("begin");
        long end = message.getParameters().get("end");
        chartInfo.setCustomBegin(begin);
        chartInfo.setCustomEnd(end);
        log.info("Message action: " + message.getAction());
      }
    }
  }

  private void logChartAction(Action action, CProfile cProfile) {
    log.info("Message action: " + action + " for " + cProfile);
  }

  private void logChartFilterAction(Action action, Metric metric, CProfile cProfileFilter, List<String> filter) {
    log.info("Message action: " + action + " for " + metric.getYAxis().getColName()
                 + " and filter: " + cProfileFilter.getColName() + " value: "
                 + filter.stream().sorted().findFirst().orElseThrow());
  }

  private void updateLegendVisibility() {
    boolean isVisible = ChartLegendState.SHOW.equals(chartLegendState);
    chartPanes.forEach((key, value) ->
                           value.getScp().getjFreeChart().getLegend().setVisible(isVisible));
    chartPanesFilter.forEach((key, value) ->
                                 value.getScp().getjFreeChart().getLegend().setVisible(isVisible));
  }

  private void addChartCard(CProfile cProfile, Map<String, Color> seriesColorMap) {
    if (chartPanes.containsKey(cProfile)) {
      return;
    }

    ChartAnalyzeModule taskPane;
    if (metric.getYAxis().equals(cProfile)) {
      taskPane = new ChartAnalyzeModule(sqlQueryState, dStore, metric, queryInfo, chartInfo, analyzeType, profileTaskQueryKey, seriesColorMap);
    } else {
      Metric metric = new Metric(tableInfo, cProfile);
      taskPane = new ChartAnalyzeModule(sqlQueryState, dStore, metric, queryInfo, chartInfo, analyzeType, profileTaskQueryKey, Collections.emptyMap());
    }

    taskPane.setTitle(cProfile.getColName());

    chartPanes.put(cProfile, taskPane);
    cardContainer.add(taskPane);
    cardContainer.revalidate();
    cardContainer.repaint();

    SwingTaskRunner.runWithProgress(
        taskPane,
        executor,
        () -> initializeChartPane(taskPane),
        log::catching,
        () -> ProgressBarHelper.createProgressBar("Loading, please wait...")
    );
  }

  private String getFilterKey(CProfile cProfile, CProfile cProfileFilter, List<String> filter) {
    String filterKey = cProfile.getColName() + " by column filter " + cProfileFilter.getColName()
        + " = " + filter.stream().sorted().findFirst().orElseThrow();

    return filterKey.length() > 100 ? filterKey.substring(0, 100) + " ... " : filterKey;
  }

  private void addChartFilterCard(Metric metric, CProfile cProfileFilter, List<String> filter, Map<String, Color> seriesColorMap) {
    LinkedHashSet<String> filters = chartFilters.computeIfAbsent(cProfileFilter, k -> new LinkedHashSet<>());
    String filterKey = getFilterKey(metric.getYAxis(), cProfileFilter, filter);

    if (filters.contains(filterKey)) {
      return;
    }

    ChartAnalyzeModule taskPane =
        new ChartAnalyzeModule(sqlQueryState, dStore, metric,
                               queryInfo, chartInfo, analyzeType, profileTaskQueryKey,
                               seriesColorMap, Map.entry(cProfileFilter, filter));

    taskPane.setTitle(filterKey);

    filters.add(filterKey);
    chartPanesFilter.put(filterKey, taskPane);
    cardContainer.add(taskPane);
    cardContainer.revalidate();
    cardContainer.repaint();

    SwingTaskRunner.runWithProgress(
        taskPane,
        executor,
        () -> initializeChartPane(taskPane),
        log::catching,
        () -> ProgressBarHelper.createProgressBar("Loading, please wait...")
    );
  }

  private Runnable initializeChartPane(ChartAnalyzeModule taskPane) {
    JTabbedPane tabbedPane = taskPane.initializeUI();
    taskPane.getScp().getjFreeChart().getLegend().setVisible(ChartLegendState.SHOW.equals(chartLegendState));

    boolean collapseAll = chartCardState.equals(ChartCardState.COLLAPSE_ALL);
    taskPane.setCollapsed(collapseAll);

    return () -> PGHelper.cellXYRemainder(taskPane, tabbedPane, false);
  }

  private void removeChartFilterCard(Metric metric, CProfile cProfileFilter, List<String> filter) {
    LinkedHashSet<String> filters = chartFilters.get(cProfileFilter);
    String filterKey = getFilterKey(metric.getYAxis(), cProfileFilter, filter);

    if (filters == null || !filters.contains(filterKey)) {
      return;
    }


    JXTaskPane taskPane = chartPanesFilter.remove(filterKey);
    if (taskPane != null) {
      cardContainer.remove(taskPane);
      cardContainer.revalidate();
      cardContainer.repaint();
    }

    filters.remove(filterKey);
  }

  private void removeChartFilterCard(String filterKey) {
    JXTaskPane taskPane = chartPanesFilter.remove(filterKey);
    if (taskPane != null) {
      cardContainer.remove(taskPane);
      cardContainer.revalidate();
      cardContainer.repaint();
    }

    chartFilters.clear();
  }

  private void removeChartCard(CProfile cProfile) {
    JXTaskPane taskPane = chartPanes.remove(cProfile);
    if (taskPane != null) {
      cardContainer.remove(taskPane);
      cardContainer.revalidate();
      cardContainer.repaint();
    }
  }

  private void updateChartCard(CProfile cProfile, JPanel newPanel) {
    JXTaskPane taskPane = chartPanes.get(cProfile);
    if (taskPane != null) {
      taskPane.removeAll();
      taskPane.add(newPanel);
      taskPane.revalidate();
      taskPane.repaint();
    }
  }

  private void expandCollapseChartCard() {
    ArrayList<Component> containerCards = new ArrayList<>(List.of(cardContainer.getComponents()));

    boolean collapseAll = chartCardState.equals(ChartCardState.COLLAPSE_ALL);

    containerCards.stream()
        .filter(c -> c instanceof JXTaskPane)
        .map(c -> (JXTaskPane) c)
        .forEach(cardChart -> cardChart.setCollapsed(collapseAll));
  }

  private void clearAllCharts() {
    chartPanes.clear();
    cardContainer.removeAll();
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  @Override
  public void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Start collect for " + profileTaskQueryKey);
  }

  @Override
  public void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    log.info("Stop collect for " + profileTaskQueryKey);

    try {
      chartPanes.forEach((key, value) -> {
        if (Objects.nonNull(value.getScp())) {
          value.getScp().loadData();
        }
      });
      chartPanesFilter.forEach((key, value) -> {
        if (Objects.nonNull(value.getScp())) {
          value.getScp().loadData();
        }
      });
    } catch (Exception e) {
      log.catching(e);
    }
  }
}
