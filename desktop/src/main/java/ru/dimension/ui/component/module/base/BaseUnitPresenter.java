package ru.dimension.ui.component.module.base;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.chart.holder.DetailAndAnalyzeHolder;
import ru.dimension.ui.component.module.analyze.CustomAction;
import ru.dimension.ui.component.module.api.UnitPresenter;
import ru.dimension.ui.component.module.api.UnitView;
import ru.dimension.ui.component.module.chart.main.ChartModel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.helper.FilterHelper;
import ru.dimension.ui.helper.LogHelper;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.UIState;
import ru.dimension.ui.view.detail.DetailDashboardPanel;

@Log4j2
public abstract class BaseUnitPresenter<V extends UnitView> implements UnitPresenter {

  protected final V view;
  protected final ExecutorService executor;
  protected final MessageBroker.Component component;
  protected final ChartModel model;

  @Getter
  protected final Metric metric;

  @Getter
  protected SCP chart;
  protected JPanel detail;

  public BaseUnitPresenter(MessageBroker.Component component, ChartModel model, V view, ExecutorService executor) {
    this.component = component;
    this.model = model;
    this.view = view;
    this.executor = executor;
    this.metric = model.getMetric().copy();
  }

  @Override
  public abstract void initializePresenter();

  @Override
  public abstract void initializeCharts();

  @Override
  public abstract void updateChart();

  protected abstract LegendPanel getLegendPanel();

  protected abstract void updateChartInternal(Map<String, Color> seriesColorMap, Map<CProfile, LinkedHashSet<String>> topMapSelected);

  protected abstract MessageBroker.Panel getPanelType();

  @Override
  public void handleLegendChangeAll(Boolean showLegend) {
    boolean visibility = showLegend != null && showLegend;

    updateLegendVisibility(visibility);

    UIState.INSTANCE.putShowLegend(model.getChartKey(), visibility);
  }

  public void handleLegendChange(Boolean showLegend) {
    boolean visibility = showLegend != null ? showLegend : true;
    updateLegendVisibility(visibility);
    UIState.INSTANCE.putShowLegend(model.getChartKey(), visibility);
  }

  protected void updateLegendVisibility(boolean visibility) {
    if (chart != null) {
      chart.snapshotSelectionRegion();

      getLegendPanel().setSelected(visibility);

      if (chart.getjFreeChart().getLegend() != null) {
        chart.getjFreeChart().getLegend().setVisible(visibility);
      }
      chart.setLegendTitleVisible(visibility);

      chart.clearSelectionRegion();

      chart.restoreSelectionRegionAfterNextDraw();
    }
  }

  @Override
  public void handleFilterChange(Map<CProfile, LinkedHashSet<String>> topMapSelected, Map<String, Color> seriesColorMap) {
    Map<String, Color> preservedColorMap = new HashMap<>(seriesColorMap != null ? seriesColorMap : Map.of());
    Map<CProfile, LinkedHashSet<String>> sanitizedTopMapSelected =
        FilterHelper.sanitizeTopMapSelected(topMapSelected, metric);

    updateChartInternal(preservedColorMap, sanitizedTopMapSelected);

    if (detail instanceof DetailDashboardPanel detailDashboardPanel) {
      detailDashboardPanel.updateSeriesColor(sanitizedTopMapSelected, preservedColorMap);
    }

    LogHelper.logMapSelected(sanitizedTopMapSelected);
  }

  protected DetailDashboardPanel buildDetail(SCP chart,
                                             Map<String, Color> initialSeriesColorMap,
                                             SeriesType seriesType,
                                             Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    Map<String, Color> seriesColorMapToUse = initialSeriesColorMap != null ?
        initialSeriesColorMap : chart.getSeriesColorMap();

    if (SeriesType.CUSTOM.equals(seriesType)) {
      seriesColorMapToUse = chart.getSeriesColorMap();
    }

    Metric chartMetric = chart.getConfig().getMetric();

    DetailDashboardPanel detailPanel =
        new DetailDashboardPanel(view,
                                 model.getChartKey(),
                                 model.getQueryInfo(),
                                 model.getChartInfo(),
                                 model.getTableInfo(),
                                 chartMetric,
                                 seriesColorMapToUse,
                                 getPanelType(),
                                 seriesType,
                                 model.getSqlQueryState(),
                                 model.getDStore(),
                                 topMapSelected);

    CustomAction customAction = new CustomAction() {
      @Override
      public void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
      }

      @Override
      public void setCustomSeriesFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected,
                                        Map<String, Color> seriesColorMap) {
        Map<String, Color> newSeriesColorMap = new HashMap<>();
        topMapSelected.values()
            .forEach(set -> set.forEach(val -> newSeriesColorMap.put(val, seriesColorMap.get(val))));

        detailPanel.updateSeriesColor(topMapSelected, newSeriesColorMap);
        detailPanel.setSeriesType(SeriesType.CUSTOM);
      }

      @Override
      public void setBeginEnd(long begin, long end) {
      }
    };

    chart.setHolderDetailsAndAnalyze(new DetailAndAnalyzeHolder(detailPanel, customAction));
    chart.addChartListenerReleaseMouse(detailPanel);

    return detailPanel;
  }

  protected Map<String, Color> getFilterSeriesColorMap(Metric metric,
                                                       Map<String, Color> seriesColorMap,
                                                       Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    if (topMapSelected == null
        || topMapSelected.isEmpty()
        || topMapSelected.values().stream().allMatch(LinkedHashSet::isEmpty)
        || !topMapSelected.containsKey(metric.getYAxis())) {
      return seriesColorMap;
    }

    Map<String, Color> filteredMap = new HashMap<>();
    for (String key : topMapSelected.get(metric.getYAxis())) {
      if (seriesColorMap.containsKey(key)) {
        filteredMap.put(key, seriesColorMap.get(key));
      }
    }
    return filteredMap;
  }

  protected void clearChartPanel() {
    view.getChartPanel().removeAll();
    view.getChartPanel().revalidate();
    view.getChartPanel().repaint();
  }

  protected void clearDetailPanel() {
    view.getDetailPanel().ifPresent(panel -> {
      panel.removeAll();
      panel.revalidate();
      panel.repaint();
    });
  }

  protected void addChartToPanel(SCP chart) {
    view.getChartPanel().add(chart, BorderLayout.CENTER);
  }

  protected void addDetailToPanel(JPanel detail) {
    view.getDetailPanel().ifPresent(panel ->
                                        panel.add(detail, BorderLayout.CENTER));
  }
}