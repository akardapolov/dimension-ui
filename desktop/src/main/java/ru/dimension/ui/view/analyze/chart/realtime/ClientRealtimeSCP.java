package ru.dimension.ui.view.analyze.chart.realtime;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.table.TableColumn;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.OrderBy;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.analyze.chart.ChartConfig;
import ru.dimension.ui.view.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.view.chart.ChartDataLoader;

@Log4j2
public class ClientRealtimeSCP extends RealtimeSCP {

  private final ChartDataLoader chartDataLoader;

  public ClientRealtimeSCP(SqlQueryState sqlQueryState,
                           DStore dStore,
                           ChartConfig config,
                           ProfileTaskQueryKey profileTaskQueryKey) {
    this(sqlQueryState, dStore, config, profileTaskQueryKey, null);
  }

  public ClientRealtimeSCP(SqlQueryState sqlQueryState,
                           DStore dStore,
                           ChartConfig config,
                           ProfileTaskQueryKey profileTaskQueryKey,
                           Map.Entry<CProfile, List<String>> filter) {
    super(config, profileTaskQueryKey, filter);

    super.setSqlQueryState(sqlQueryState);
    super.setDStore(dStore);

    this.dataHandler = initFunctionDataHandler(config.getMetric(), config.getQueryInfo(), dStore);
    if (filter != null) {
      this.dataHandler.setFilter(filter);
    }

    chartDataLoader = new ChartDataLoader(config.getMetric(), config.getChartInfo(), stackedChart, dataHandler, true);
  }

  @Override
  public void initialize() {
    log.info(getHourMinSec(config.getQueryInfo().getDeltaLocalServerTime()));

    disablePlotUpdates();

    ChartRange chartRange = getBeginEndRange();

    try {
      if (MetricFunction.COUNT.equals(config.getMetric().getMetricFunction())) {
        List<String> distinct = new ArrayList<>();

        if (filter != null && !filter.getKey().equals(config.getMetric().getYAxis())) {
          distinct = dStore.getDistinct(
              config.getQueryInfo().getName(),
              config.getMetric().getYAxis(),
              OrderBy.DESC,
              MAX_SERIES,
              chartRange.getBegin(),
              chartRange.getEnd(),
              filter.getKey(),
              filter.getValue().toArray(new String[0]),
              CompareFunction.EQUAL
          );
        } else {
          distinct = dStore.getDistinct(
              config.getQueryInfo().getName(),
              config.getMetric().getYAxis(),
              OrderBy.DESC,
              MAX_SERIES,
              chartRange.getBegin(),
              chartRange.getEnd()
          );
        }

        boolean useCustom = distinct.size() > THRESHOLD_SERIES;
        if (useCustom) {
          seriesType = SeriesType.CUSTOM;
          int showSeries = SHOW_SERIES;

          if (filter != null && filter.getKey().equals(config.getMetric().getYAxis())) {
            List<String> combined = new ArrayList<>(filter.getValue());
            for (String value : distinct) {
              if (!combined.contains(value)) {
                combined.add(value);
              }
            }
            distinct = combined;
            showSeries = filter.getValue().size();
          }

          initializeSeriesSelectableTable(distinct, showSeries);

          filter = Map.entry(config.getMetric().getYAxis(), getCheckBoxSelected());
          series.addAll(getCheckBoxSelected());
        } else {
          seriesType = SeriesType.COMMON;
          series.addAll(distinct);
        }
      } else {
        seriesType = SeriesType.COMMON;
        series.add(config.getMetric().getYAxis().getColName());
      }
    } catch (BeginEndWrongOrderException e) {
      log.error("Error fetching distinct series", e);
    }

    setCustomFilter();
    chartDataLoader.setSeries(series);
    chartDataLoader.setFilter(filter);

    if (seriesType == SeriesType.CUSTOM) {
      initializeWithSeriesTable();
    } else {
      initializeGUI();
    }

    try {
      chartDataLoader.loadDataFromBdbToDeque(
          dStore.getBlockKeyTailList(config.getQueryInfo().getName(), chartRange.getBegin(), chartRange.getEnd())
      );
      chartDataLoader.loadDataFromDequeToChart(chartRange.getBegin(), chartRange.getEnd());
    } catch (BeginEndWrongOrderException | SqlColMetadataException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }

    chartDataLoader.setClientBegin(chartRange.getEnd() + 1);

    enablePlotUpdates();
  }

  @Override
  public void loadData() {
    disablePlotUpdates();

    ChartRange currRange = getBeginEndRange();

    try {
      chartDataLoader.loadDataFromBdbToDeque(dStore.getBlockKeyTailList(config.getQueryInfo().getName(), chartDataLoader.getClientBegin(), currRange.getEnd()));
    } catch (BeginEndWrongOrderException | SqlColMetadataException e) {
      log.error("Data loading error: ", e);
      log.catching(e);
      throw new RuntimeException(e);
    }

    try {
      chartDataLoader.loadDataFromDequeToChart(chartDataLoader.getClientBegin(), currRange.getEnd());
    } catch (BeginEndWrongOrderException | SqlColMetadataException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }

    chartDataLoader.setClientBegin(currRange.getEnd() + 1);

    enablePlotUpdates();
  }

  protected void initializeSeriesSelectableTable(List<String> distinct, int showSeries) {
    Consumer<String> runAction = this::runAction;
    String[] columnNames = {ColumnNames.NAME.getColName(), ColumnNames.PICK.getColName()};
    int nameId = 0;
    int checkBoxId = 1;

    this.seriesSelectable = GUIHelper.getJXTableCaseCheckBoxAdHoc(10, columnNames, checkBoxId);
    TableColumn colMetric = seriesSelectable.getJxTable().getColumnModel().getColumn(checkBoxId);
    colMetric.setMinWidth(30);
    colMetric.setMaxWidth(35);

    TableSelectionHandler handler = new TableSelectionHandler(seriesSelectable,
                                                              ColumnNames.NAME.getColName(), runAction);
    handler.initializeTableModelListener(nameId, checkBoxId);

    seriesSelectable.setBlockRunAction(true);
    distinct.forEach(seriesName ->
                         seriesSelectable.getDefaultTableModel().addRow(new Object[]{seriesName, false})
    );
    for (int i = 0; i < Math.min(showSeries, distinct.size()); i++) {
      seriesSelectable.getDefaultTableModel().setValueAt(true, i, 1);
    }
    seriesSelectable.setBlockRunAction(false);
  }

  @NotNull
  private ChartRange getBeginEndRange() {
    long serverDateTime = System.currentTimeMillis() - config.getQueryInfo().getDeltaLocalServerTime();
    long beginRange = serverDateTime - getRangeRealTime(config.getChartInfo());
    return new ChartRange(beginRange, serverDateTime);
  }

  @Override
  protected void reloadDataForCurrentRange() {
    try {
      ChartRange currRange = getBeginEndRange();

      stackedChart.deleteAllSeriesData(config.getChartInfo().getRangeRealtime().getMinutes());

      chartDataLoader.loadDataFromBdbToDeque(dStore.getBlockKeyTailList(config.getQueryInfo().getName(), currRange.getBegin(), currRange.getEnd()));

      chartDataLoader.loadDataFromDequeToChart(currRange.getBegin(), currRange.getEnd());
    } catch (Exception e) {
      log.error("Error reloading data", e);
    }
  }

  protected void runAction(String seriesName) {
    if (seriesSelectable.isBlockRunAction()) {
      return;
    }

    executorService.submit(() -> {
      try {
        seriesSelectable.setBlockRunAction(true);
        seriesSelectable.getJxTable().setEnabled(false);

        disablePlotUpdates();

        showProgressBar();

        Set<String> newSelection = new HashSet<>(getCheckBoxSelected());
        if (!newSelection.equals(series)) {
          clearSeriesColor();

          series.clear();
          series.addAll(newSelection);

          setCustomFilter();

          chartDataLoader.setSeries(series);
          chartDataLoader.setFilter(filter);

          reloadDataForCurrentRange();

          if (detailAndAnalyzeHolder != null) {
            detailAndAnalyzeHolder.detailAction().cleanMainPanel();
            detailAndAnalyzeHolder.customAction()
                .setCustomSeriesFilter(config.getMetric().getYAxis(),
                                       List.copyOf(newSelection),
                                       getSeriesColorMap());
          }
        }
        showChart();
      } finally {
        seriesSelectable.setBlockRunAction(false);
        seriesSelectable.getJxTable().setEnabled(true);

        enablePlotUpdates();
      }
    });
  }

  private void showProgressBar() {
    BorderLayout layout = (BorderLayout) getLayout();
    if (layout.getLayoutComponent(BorderLayout.CENTER) != null) {
      remove(layout.getLayoutComponent(BorderLayout.CENTER));
    }
    add(ProgressBarHelper.createProgressBar("Loading, please wait..."), BorderLayout.CENTER);
    revalidate();
    repaint();
  }

  private void showChart() {
    BorderLayout layout = (BorderLayout) getLayout();
    if (layout.getLayoutComponent(BorderLayout.CENTER) != null) {
      remove(layout.getLayoutComponent(BorderLayout.CENTER));
    }
    add(stackedChart.getChartPanel(), BorderLayout.CENTER);
    revalidate();
    repaint();
  }
}
