package ru.dimension.ui.view.chart.stacked;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.swing.table.TableColumn;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.exception.BeginEndWrongOrderException;
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
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.gui.ChartInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.view.chart.holder.DetailAndAnalyzeHolder;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.SeriesType;

@Log4j2
public class ServerClientHistorySCP extends StackChartPanel {

  private final TableInfo tableInfo;
  private JXTableCase seriesSelectable;

  private DetailAndAnalyzeHolder detailAndAnalyzeHolder;

  private ExecutorService executorService;

  public ServerClientHistorySCP(WorkspaceQueryComponent workspaceQueryComponent,
                                CategoryTableXYDatasetRealTime chartDataset,
                                ProfileTaskQueryKey profileTaskQueryKey,
                                QueryInfo queryInfo,
                                TableInfo tableInfo,
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
    this.tableInfo = tableInfo;
    this.executorService = Executors.newSingleThreadExecutor();
  }

  @Override
  public void initialize() {
    dStore.syncBackendDb();

    ChartRange chartRange = getChartRange(chartInfo);

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

          initializeSeriesSelectable(distinct, showSeries);

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

    loadDataHistory(chartRange);

    initializeDateAxis(chartRange.getBegin(), chartRange.getEnd());

    if (SeriesType.CUSTOM.equals(seriesType)) {
      initializeWithSeriesTable();
    } else {
      initializeGUI();
    }
  }

  @Override
  public void loadData() {}

  public void setHolderDetailsAndAnalyze(DetailAndAnalyzeHolder detailAndAnalyzeHolder) {
    this.detailAndAnalyzeHolder = detailAndAnalyzeHolder;
  }

  private void initializeSeriesSelectable(List<String> distinct, int showSeries) {
    Consumer<String> runAction = this::runAction;
    String[] columnNames = {ColumnNames.NAME.getColName(), ColumnNames.PICK.getColName()};
    int nameId = 0;
    int checkBoxId = 1;

    this.seriesSelectable = GUIHelper.getJXTableCaseCheckBoxAdHoc(10, columnNames, checkBoxId);
    TableColumn colMetric = seriesSelectable.getJxTable().getColumnModel().getColumn(checkBoxId);
    colMetric.setMinWidth(30);
    colMetric.setMaxWidth(35);

    TableSelectionHandler handler = new TableSelectionHandler(seriesSelectable, ColumnNames.NAME.getColName(), runAction);
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

  private void initializeWithSeriesTable() {
    this.setLayout(new BorderLayout());
    this.add(stackedChart.getChartPanel(), BorderLayout.CENTER);
    this.add(seriesSelectable.getJScrollPane(), BorderLayout.EAST);
  }

  private void runAction(String seriesName) {
    if (seriesSelectable.isBlockRunAction()) return;

    executorService.submit(() -> {
      try {
        seriesSelectable.setBlockRunAction(true);
        seriesSelectable.getJxTable().setEnabled(false);

        showProgressBar();

        Set<String> newSelection = new LinkedHashSet<>(getCheckBoxSelected());

        if (!newSelection.equals(series)) {
          series.clear();
          series.addAll(newSelection);

          ChartRange chartRange = getChartRange(chartInfo);

          clearSeriesColor();

          if (detailAndAnalyzeHolder != null) {
            detailAndAnalyzeHolder.detailAction().cleanMainPanel();
            detailAndAnalyzeHolder.customAction().setBeginEnd(chartRange.getBegin(), chartRange.getEnd());
            detailAndAnalyzeHolder.customAction().setCustomSeriesFilter(metric.getYAxis(), List.copyOf(newSelection));
          }

          loadDataHistory(chartRange);
        }

        showChart();
      } finally {
        seriesSelectable.setBlockRunAction(false);
        seriesSelectable.getJxTable().setEnabled(true);
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

  private List<String> getCheckBoxSelected() {
    List<String> selected = new ArrayList<>();
    for (int i = 0; i < seriesSelectable.getJxTable().getRowCount(); i++) {
      if ((Boolean) seriesSelectable.getJxTable().getValueAt(i, 1)) {
        selected.add((String) seriesSelectable.getJxTable().getValueAt(i, 0));
      }
    }
    return selected;
  }

  private void loadDataHistory(ChartRange chartRange) {
    chartDataset.clear();

    Set<String> filteredSeries = SeriesType.CUSTOM.equals(seriesType) ?
        new HashSet<>(getCheckBoxSelected()) :
        series;

    double range = (double) getRangeHistory(chartInfo) / MAX_POINTS_PER_GRAPH;
    for (long dtBegin = chartRange.getBegin(); dtBegin <= chartRange.getEnd(); dtBegin += Math.round(range)) {
      long dtEnd = dtBegin + Math.round(range) - 1;
      double k = (double) Math.round(range) / 1000;

      if (SeriesType.CUSTOM.equals(seriesType)) {
        CProfile cProfileFilter = metric.getYAxis();
        String[] filterData = getCheckBoxSelected().toArray(String[]::new);
        CompareFunction compareFunction = CompareFunction.EQUAL;

        dataHandler.handleFunction(dtBegin, dtEnd, k, filteredSeries, cProfileFilter, filterData, compareFunction, stackedChart);
      } else {
        dataHandler.handleFunction(dtBegin, dtEnd, false, dtBegin, k, filteredSeries, stackedChart);
      }
    }
  }
}