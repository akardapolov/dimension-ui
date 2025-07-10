package ru.dimension.ui.view.analyze.chart.realtime;

import static ru.dimension.ui.view.chart.RangeUtils.getSubRangeSize;

import java.awt.BorderLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.swing.table.TableColumn;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.OrderBy;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.ChartType;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.view.analyze.chart.ChartConfig;
import ru.dimension.ui.view.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.view.chart.RangeUtils;
import ru.dimension.ui.view.chart.RangeUtils.TimeRange;

@Log4j2
public class ServerRealtimeSCP extends RealtimeSCP {

  private final QueryInfo queryInfo;
  private final ChartInfo chartInfo;
  private final Metric metric;

  protected ExecutorService executorService = Executors.newSingleThreadExecutor();
  protected Map.Entry<CProfile, List<String>> filter;
  private boolean noDataFound = false;
  private TimeRange currentTimeRange = new TimeRange(0, 0);

  public ServerRealtimeSCP(SqlQueryState sqlQueryState,
                           DStore dStore,
                           ChartConfig config,
                           ProfileTaskQueryKey profileTaskQueryKey) {
    this(sqlQueryState, dStore, config, profileTaskQueryKey, null);
  }

  public ServerRealtimeSCP(SqlQueryState sqlQueryState,
                           DStore dStore,
                           ChartConfig config,
                           ProfileTaskQueryKey profileTaskQueryKey,
                           Map.Entry<CProfile, List<String>> filter) {
    super(config, profileTaskQueryKey, filter);

    super.setSqlQueryState(sqlQueryState);
    super.setDStore(dStore);

    this.chartInfo = config.getChartInfo();
    this.queryInfo = config.getQueryInfo();
    this.metric = config.getMetric();
    this.filter = filter;

    this.dataHandler = initFunctionDataHandler(metric, queryInfo, dStore);
    if (filter != null) {
      this.dataHandler.setFilter(filter);
    }
  }

  @Override
  public void initialize() {
    log.info(getHourMinSec(queryInfo.getDeltaLocalServerTime()));

    disablePlotUpdates();

    long serverTimeMillis = System.currentTimeMillis() - queryInfo.getDeltaLocalServerTime();
    long rangeStart = serverTimeMillis - (chartInfo.getRangeRealtime().getMinutes() * 60 * 1000L);

    ChartRange chartRange = new ChartRange(rangeStart, serverTimeMillis);

    try {
      if (MetricFunction.COUNT.equals(metric.getMetricFunction())) {
        List<String> distinct = new ArrayList<>();

        if (filter != null && !filter.getKey().equals(metric.getYAxis())) {
          distinct = dStore.getDistinct(queryInfo.getName(),
                                        metric.getYAxis(),
                                        OrderBy.DESC,
                                        MAX_SERIES,
                                        chartRange.getBegin(),
                                        chartRange.getEnd(),
                                        filter.getKey(),
                                        filter.getValue().toArray(new String[0]),
                                        CompareFunction.EQUAL);
        } else {
          distinct = getDistinct(dStore, queryInfo.getName(), metric.getYAxis(), chartRange, MAX_SERIES);
        }

        boolean useCustom = distinct.size() > THRESHOLD_SERIES;

        if (useCustom) {
          seriesType = SeriesType.CUSTOM;

          int showSeries = SHOW_SERIES;

          if (filter != null && filter.getKey().equals(metric.getYAxis())) {
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

          filter = Map.entry(metric.getYAxis(), getCheckBoxSelected());
          series.addAll(getCheckBoxSelected());
        } else {
          seriesType = SeriesType.COMMON;
          series.addAll(distinct);
        }
      } else {
        seriesType = SeriesType.COMMON;
        series.add(metric.getYAxis().getColName());
      }
    } catch (BeginEndWrongOrderException e) {
      log.error("Error fetching distinct series", e);
    }

    if (seriesType == SeriesType.CUSTOM) {
      initializeWithSeriesTable();
    } else {
      initializeGUI();
    }

    try {
      initializeChartData(rangeStart, serverTimeMillis);
    } catch (SqlColMetadataException | BeginEndWrongOrderException e) {
      log.error(e);
      throw new RuntimeException(e);
    }

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

  protected void setCustomFilter() {
    if (seriesType == SeriesType.CUSTOM) {
      filter = Map.entry(
          metric.getYAxis(),
          getCheckBoxSelected()
      );
      dataHandler.setFilter(filter);
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

  protected List<String> getCheckBoxSelected() {
    List<String> selected = new ArrayList<>();
    for (int i = 0; i < seriesSelectable.getJxTable().getRowCount(); i++) {
      if ((Boolean) seriesSelectable.getJxTable().getValueAt(i, 1)) {
        selected.add((String) seriesSelectable.getJxTable().getValueAt(i, 0));
      }
    }
    return selected;
  }

  protected void initializeWithSeriesTable() {
    this.setLayout(new BorderLayout());
    this.add(stackedChart.getChartPanel(), BorderLayout.CENTER);
    this.add(seriesSelectable.getJScrollPane(), BorderLayout.EAST);
  }

  @Override
  protected void reloadDataForCurrentRange() {
    try {
      long serverTimeMillis = System.currentTimeMillis() - queryInfo.getDeltaLocalServerTime();
      long rangeStart = serverTimeMillis - (chartInfo.getRangeRealtime().getMinutes() * 60 * 1000L);

      stackedChart.deleteAllSeriesData(chartInfo.getRangeRealtime().getMinutes());

      List<StackedColumn> sColumnList = dataHandler.handleFunctionComplex(rangeStart, serverTimeMillis);
      fillSeriesAnalyze(sColumnList);

      if (!series.isEmpty()) {
        fillChart(serverTimeMillis, chartInfo.getRangeRealtime().getMinutes());
      }
    } catch (Exception e) {
      log.error("Error reloading data", e);
    }
  }

  private void initializeChartData(long rangeStart, long serverTimeMillis)
      throws SqlColMetadataException, BeginEndWrongOrderException {
    List<StackedColumn> sColumnList = dataHandler.handleFunctionComplex(rangeStart, serverTimeMillis);
    fillSeriesAnalyze(sColumnList);

    if (series.isEmpty()) {
      handleEmptySeriesCase(rangeStart, serverTimeMillis);
    } else {
      sColumnList = dataHandler.handleFunctionComplex(rangeStart, serverTimeMillis);
      fillSeriesAnalyze(sColumnList);
      fillChart(serverTimeMillis, chartInfo.getRangeRealtime().getMinutes());
    }
  }

  private void handleEmptySeriesCase(long rangeStart, long serverTimeMillis)
      throws SqlColMetadataException, BeginEndWrongOrderException {
    long last = dStore.getLast(queryInfo.getName(), Long.MIN_VALUE, Long.MAX_VALUE);

    if (last != 0) {
      log.info("Get data from history");
      if (last < rangeStart) {
        List<StackedColumn> sColumnList = dataHandler.handleFunctionComplex(last - getRangeRealTime(chartInfo), last);
        fillSeriesAnalyze(sColumnList);
        fillChartEmpty(serverTimeMillis, chartInfo.getRangeRealtime().getMinutes());
      }
    } else {
      noDataFound = true;

      currentTimeRange = new TimeRange(serverTimeMillis - chartInfo.getPullTimeoutClient() * 1000L, serverTimeMillis);
      log.info("No data found in history");
    }
  }

  protected void fillChart(long serverTimeMillis, int rangeMinutes) {
    processTimeRanges(serverTimeMillis, rangeMinutes, false);
  }

  protected void fillChartEmpty(long serverTimeMillis,
                                int rangeMinutes) {
    List<TimeRange> ranges = RangeUtils.calculateRanges(serverTimeMillis, rangeMinutes);

    for (TimeRange range : ranges) {
      if (!updateCurrentTimeRange(range)) {
        break;
      }

      if (ChartType.LINEAR.equals(metric.getChartType())) {
        stackedChart.addSeriesValue(range.getStart(), 0.0, metric.getYAxis().getColName());
      } else {
        addZeroValuesForAllSeries(range.getStart());
      }

      if (sqlQueryState.getLastTimestamp(profileTaskQueryKey) == range.getEnd()) {
        break;
      }
    }
  }

  @Override
  public void loadData() {
    disablePlotUpdates();

    try {
      int subRangeSize = getSubRangeSize(chartInfo.getRangeRealtime().getMinutes());
      long serverTimeMillis = System.currentTimeMillis() - queryInfo.getDeltaLocalServerTime();
      long startRange = currentTimeRange.getEnd() + 1;

      if (noDataFound) {
        long last = dStore.getLast(queryInfo.getName(), startRange, serverTimeMillis);

        if (last != 0) {
          List<StackedColumn> sColumnList;
          try {
            sColumnList = dataHandler.handleFunctionComplex(serverTimeMillis - getRangeRealTime(chartInfo), serverTimeMillis);
          } catch (BeginEndWrongOrderException | SqlColMetadataException e) {
            throw new RuntimeException(e);
          }
          fillSeriesAnalyze(sColumnList);
          fillChartEmpty(currentTimeRange.getStart(), chartInfo.getRangeRealtime().getMinutes());

          noDataFound = false;
        } else {
          noDataFound = true;

          currentTimeRange = new TimeRange(serverTimeMillis - chartInfo.getPullTimeoutClient() * 1000L, serverTimeMillis);
          log.info("No data found");
        }
      }

      while (startRange < sqlQueryState.getLastTimestamp(profileTaskQueryKey)) {
        long endRange = startRange + subRangeSize;
        TimeRange range = new TimeRange(startRange, endRange);

        if (!updateCurrentTimeRange(range)) break;

        double k = chartInfo.getRangeRealtime().getMinutes();

        boolean applyFilter = false;
        CProfile cProfileFilter = null;
        String[] filterData = new String[0];
        CompareFunction compareFunction = CompareFunction.EQUAL;

        if (filter != null) {
          applyFilter = true;
          cProfileFilter = filter.getKey();
          filterData = filter.getValue().toArray(new String[0]);

          if (cProfileFilter.equals(metric.getYAxis())) {
            series.clear();
            series.addAll(filter.getValue());
          }
        }

        if (applyFilter) {
          dataHandler.handleFunction(range.getStart(), range.getEnd(), k, series, cProfileFilter, filterData, compareFunction, stackedChart);
        } else {
          dataHandler.handleFunction(range.getStart(), range.getEnd(), false, range.getStart(), k, series, stackedChart);
        }

        startRange = endRange + 1;

        if (sqlQueryState.getLastTimestamp(profileTaskQueryKey) == range.getEnd()) {
          break;
        }
      }

      stackedChart.deleteAllSeriesData(chartInfo.getRangeRealtime().getMinutes());
    } finally {
      enablePlotUpdates();
    }
  }

  private void processTimeRanges(long serverTimeMillis, int rangeMinutes, boolean fillEmpty) {
    List<TimeRange> ranges = RangeUtils.calculateRanges(serverTimeMillis, rangeMinutes);
    double k = rangeMinutes;

    for (TimeRange range : ranges) {
      if (!updateCurrentTimeRange(range)) break;

      if (fillEmpty) {
        if (ChartType.LINEAR.equals(metric.getChartType())) {
          stackedChart.addSeriesValue(range.getStart(), 0.0, metric.getYAxis().getColName());
        } else {
          addZeroValuesForAllSeries(range.getStart());
        }
      } else {
        boolean applyFilter = false;
        CProfile cProfileFilter = null;
        String[] filterData = new String[0];
        CompareFunction compareFunction = CompareFunction.EQUAL;

        if (filter != null) {
          applyFilter = true;
          cProfileFilter = filter.getKey();
          filterData = filter.getValue().toArray(new String[0]);

          if (cProfileFilter.equals(metric.getYAxis())) {
            series.clear();
            series.addAll(filter.getValue());
          }
        }

        if (applyFilter) {
          dataHandler.handleFunction(range.getStart(), range.getEnd(), k, series, cProfileFilter, filterData, compareFunction, stackedChart);
        } else {
          dataHandler.handleFunction(range.getStart(), range.getEnd(), false, range.getStart(), k, series, stackedChart);
        }
      }

      if (sqlQueryState.getLastTimestamp(profileTaskQueryKey) == range.getEnd()) {
        break;
      }
    }
  }

  private boolean updateCurrentTimeRange(TimeRange range) {
    if (sqlQueryState.getLastTimestamp(profileTaskQueryKey) >= range.getEnd()) {
      this.currentTimeRange = range;
      return true;
    }
    return false;
  }

  private void addZeroValuesForAllSeries(long xValue) {
    series.forEach(seriesName -> {
      try {
        stackedChart.addSeriesValue(xValue, 0.0, seriesName);
      } catch (Exception exception) {
        log.info(exception);
      }
    });
  }

  protected String getDate(long l) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    Date date = new Date(l);
    return dateFormat.format(date);
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