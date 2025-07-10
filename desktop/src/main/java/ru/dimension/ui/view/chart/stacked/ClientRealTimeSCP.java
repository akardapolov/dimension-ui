package ru.dimension.ui.view.chart.stacked;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.swing.table.TableColumn;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.OrderBy;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.config.prototype.query.WorkspaceQueryComponent;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.CategoryTableXYDatasetRealTime;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.view.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.view.chart.ChartDataLoader;
import ru.dimension.ui.view.chart.holder.DetailAndAnalyzeHolder;

@Log4j2
public class ClientRealTimeSCP extends StackChartPanel {

  private DetailAndAnalyzeHolder detailAndAnalyzeHolder;

  protected JXTableCase seriesSelectable;
  protected ExecutorService executorService = Executors.newSingleThreadExecutor();
  protected Map.Entry<CProfile, List<String>> filter;
  private final ChartDataLoader chartDataLoader;

  public ClientRealTimeSCP(WorkspaceQueryComponent workspaceQueryComponent,
                           CategoryTableXYDatasetRealTime chartDataset,
                           ProfileTaskQueryKey profileTaskQueryKey,
                           QueryInfo queryInfo,
                           ChartInfo chartInfo,
                           ProcessType processType,
                           Metric metric) {
    super(workspaceQueryComponent,
          chartDataset,
          profileTaskQueryKey,
          queryInfo,
          chartInfo,
          processType,
          metric);

    chartDataLoader = new ChartDataLoader(metric, chartInfo, stackedChart, dataHandler, true);
  }

  @Override
  public void initialize() {
    log.info(getHourMinSec(queryInfo.getDeltaLocalServerTime()));

    disablePlotUpdates();

    ChartRange chartRange = getBeginEndRange();

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
          } else if (filter != null && !filter.getKey().equals(metric.getYAxis())) {

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

    setCustomFilter();

    chartDataLoader.setSeries(series);
    chartDataLoader.setFilter(filter);

    if (seriesType == SeriesType.CUSTOM) {
      initializeWithSeriesTable();
    } else {
      initializeGUI();
    }

    try {
      chartDataLoader.loadDataFromBdbToDeque(dStore.getBlockKeyTailList(queryInfo.getName(), chartRange.getBegin(), chartRange.getEnd()));
    } catch (BeginEndWrongOrderException | SqlColMetadataException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }

    try {
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
    ChartRange currRange = getBeginEndRange();

    disablePlotUpdates();

    try {
      chartDataLoader.loadDataFromBdbToDeque(dStore.getBlockKeyTailList(queryInfo.getName(), chartDataLoader.getClientBegin(), currRange.getEnd()));
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

  protected void reloadDataForCurrentRange() {
    try {
      ChartRange currRange = getBeginEndRange();

      stackedChart.deleteAllSeriesData(chartInfo.getRangeRealtime().getMinutes());

      chartDataLoader.loadDataFromBdbToDeque(dStore.getBlockKeyTailList(queryInfo.getName(), currRange.getBegin(), currRange.getEnd()));

      chartDataLoader.loadDataFromDequeToChart(currRange.getBegin(), currRange.getEnd());
    } catch (Exception e) {
      log.error("Error reloading data", e);
    }
  }

  private void initializeSeriesSelectableTable(List<String> distinct, int showSeries) {
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
          chartDataset.clear();

          series.clear();
          series.addAll(newSelection);

          setCustomFilter();

          chartDataLoader.setSeries(series);
          chartDataLoader.setFilter(filter);

          clearSeriesColor();

          if (detailAndAnalyzeHolder != null) {
            detailAndAnalyzeHolder.detailAction().cleanMainPanel();
            detailAndAnalyzeHolder.customAction().setCustomSeriesFilter(metric.getYAxis(), List.copyOf(newSelection));
          }

          reloadDataForCurrentRange();
        }

        showChart();
      } finally {
        seriesSelectable.setBlockRunAction(false);
        seriesSelectable.getJxTable().setEnabled(true);

        enablePlotUpdates();
      }
    });
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

  @NotNull
  private ChartRange getBeginEndRange() {
    long serverDateTime = System.currentTimeMillis() - queryInfo.getDeltaLocalServerTime();
    long beginRange = serverDateTime - getRangeRealTime(chartInfo);
    return new ChartRange(beginRange, serverDateTime);
  }

  public void setHolderDetailsAndAnalyze(DetailAndAnalyzeHolder detailAndAnalyzeHolder) {
    this.detailAndAnalyzeHolder = detailAndAnalyzeHolder;
  }
}