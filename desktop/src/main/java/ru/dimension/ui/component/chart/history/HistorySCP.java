package ru.dimension.ui.component.chart.history;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.output.BlockKeyTail;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.ChartDataLoader;
import ru.dimension.ui.component.chart.FunctionDataHandler;
import ru.dimension.ui.component.chart.HelperChart;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.module.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.function.NormFunction;
import ru.dimension.ui.model.function.TimeRangeFunction;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.SeriesType;

@Log4j2
public class HistorySCP extends SCP {

  protected FunctionDataHandler dataHandler;
  @Getter
  protected Map<CProfile, LinkedHashSet<String>> topMapSelected;

  private JXTableCase seriesSelectable;
  private JTextField seriesSearch;
  private TableRowSorter<?> seriesSorter;

  private boolean isDetail = false;

  private final ExecutorService executorService;

  public HistorySCP(DStore dStore,
                    ChartConfig config,
                    ProfileTaskQueryKey profileTaskQueryKey,
                    Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    this(dStore, config, profileTaskQueryKey, topMapSelected, false);
  }

  public HistorySCP(DStore dStore,
                    ChartConfig config,
                    ProfileTaskQueryKey profileTaskQueryKey,
                    Map<CProfile, LinkedHashSet<String>> topMapSelected,
                    boolean isDetail) {
    super(config, profileTaskQueryKey);

    super.setDStore(dStore);

    this.dataHandler = initFunctionDataHandler(profileTaskQueryKey, config.getMetric(), config.getQueryInfo(), dStore);

    if (topMapSelected != null) {
      if (topMapSelected.values().stream().allMatch(LinkedHashSet::isEmpty)) {
        this.topMapSelected = null;
        this.dataHandler.setFilter(null);
      } else {
        this.topMapSelected = topMapSelected;
        this.dataHandler.setFilter(topMapSelected);
      }
    }

    this.isDetail = isDetail;
    this.executorService = Executors.newSingleThreadExecutor();
  }

  @Override
  public void initialize() {
    dStore.syncBackendDb();

    ChartRange chartRange = getChartRangeExact(config.getChartInfo());

    try {
      if (GroupFunction.COUNT.equals(config.getMetric().getGroupFunction())) {
        List<String> distinct = getDistinct(dStore,
                                            config.getQueryInfo().getName(),
                                            config.getMetric().getYAxis(),
                                            chartRange,
                                            MAX_SERIES);

        boolean useCustom = distinct.size() > THRESHOLD_SERIES;

        if (useCustom) {
          seriesType = SeriesType.CUSTOM;

          int showSeries = SHOW_SERIES;

          initializeSeriesSelectable(distinct, showSeries);

          List<String> checkBoxSelected = getCheckBoxSelected();
          this.topMapSelected = new HashMap<>() {{
            put(config.getMetric().getYAxis(), new LinkedHashSet<>(checkBoxSelected));
          }};
          this.dataHandler.setFilter(topMapSelected);

          series.clear();
          series.addAll(checkBoxSelected);
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

    loadDataHistory(chartRange);

    initializeDateAxis(chartRange.getBegin(), chartRange.getEnd());

    if (SeriesType.CUSTOM.equals(seriesType)) {
      initializeWithSeriesTable();
    } else {
      initializeGUI();
    }
  }

  private void initializeSeriesSelectable(List<String> distinct,
                                          int showSeries) {
    Consumer<String> runAction = this::runAction;
    String[] columnNames = {ColumnNames.NAME.getColName(), ColumnNames.PICK.getColName()};
    int nameId = 0;
    int checkBoxId = 1;

    this.seriesSelectable = GUIHelper.getJXTableCaseCheckBoxAdHoc(10, columnNames, checkBoxId);
    TableColumn colMetric = seriesSelectable.getJxTable().getColumnModel().getColumn(checkBoxId);
    colMetric.setMinWidth(40);
    colMetric.setMaxWidth(45);

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

    seriesSorter = new TableRowSorter<>(seriesSelectable.getJxTable().getModel());
    seriesSelectable.getJxTable().setRowSorter(seriesSorter);
  }

  private void initializeWithSeriesTable() {
    this.setLayout(new BorderLayout());
    this.add(stackedChart.getChartPanel(), BorderLayout.CENTER);

    JPanel eastPanel = new JPanel(new BorderLayout());

    seriesSearch = new JTextField();
    seriesSearch.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        updateSeriesFilter();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        updateSeriesFilter();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        updateSeriesFilter();
      }
    });
    seriesSearch.setToolTipText("Search...");

    eastPanel.add(seriesSearch, BorderLayout.NORTH);
    eastPanel.add(seriesSelectable.getJScrollPane(), BorderLayout.CENTER);

    this.add(eastPanel, BorderLayout.EAST);

    this.revalidate();
    this.repaint();
  }

  private void updateSeriesFilter() {
    String searchText = seriesSearch.getText();

    if (seriesSelectable == null) {
      return;
    }

    if (seriesSorter == null) {
      seriesSorter = new TableRowSorter<>(seriesSelectable.getJxTable().getModel());
      seriesSelectable.getJxTable().setRowSorter(seriesSorter);
    }

    if (searchText == null || searchText.isEmpty()) {
      seriesSorter.setRowFilter(null);
    } else {
      try {
        seriesSorter.setRowFilter(RowFilter.regexFilter("(?iu)" + searchText, 0));
      } catch (PatternSyntaxException e) {
        seriesSorter.setRowFilter(RowFilter.regexFilter("$^", 0));
      }
    }
  }

  private void runAction(String seriesName) {
    if (seriesSelectable.isBlockRunAction()) {
      return;
    }

    final List<String> currentSelection = getCheckBoxSelected();

    executorService.submit(() -> {
      try {
        seriesSelectable.setBlockRunAction(true);
        seriesSelectable.getJxTable().setEnabled(false);
        showProgressBar();

        Set<String> newSelection = new HashSet<>(currentSelection);

        if (!newSelection.equals(series)) {
          clearSeriesColor();
          series.clear();
          series.addAll(newSelection);

          if (topMapSelected.get(config.getMetric().getYAxis()).isEmpty()) {
            topMapSelected.putIfAbsent(config.getMetric().getYAxis(), new LinkedHashSet<>(newSelection));
          } else {
            topMapSelected.get(config.getMetric().getYAxis()).clear();
            topMapSelected.get(config.getMetric().getYAxis()).addAll(newSelection);
          }

          ChartRange chartRange = getChartRangeExact(config.getChartInfo());

          if (detailAndAnalyzeHolder != null) {
            detailAndAnalyzeHolder.detailAction().cleanMainPanel();
          }

          loadDataHistory(chartRange);

          if (detailAndAnalyzeHolder != null) {
            detailAndAnalyzeHolder.detailAction().cleanMainPanel();
            detailAndAnalyzeHolder.customAction().setCustomSeriesFilter(topMapSelected, getSeriesColorMap());
          }
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
    add(createProgressBar("Loading, please wait..."), BorderLayout.CENTER);
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
    int modelRowCount = seriesSelectable.getDefaultTableModel().getRowCount();
    for (int modelRow = 0; modelRow < modelRowCount; modelRow++) {
      if ((Boolean) seriesSelectable.getDefaultTableModel().getValueAt(modelRow, 1)) {
        selected.add((String) seriesSelectable.getDefaultTableModel().getValueAt(modelRow, 0));
      }
    }
    return selected;
  }

  private void loadDataHistory(ChartRange chartRange) {
    System.out.println("debug..");
    System.out.println(config.getChartKey());
    System.out.println(profileTaskQueryKey);
    System.out.println(isDetail);

    if (isDetail) {
      switch (config.getQueryInfo().getGatherDataMode()) {
        case BY_SERVER_JDBC -> {
          log.info(GatherDataMode.BY_SERVER_JDBC.name());
          loadDataHistoryServer(chartRange);
        }
        case BY_CLIENT_JDBC, BY_CLIENT_HTTP -> {
          log.info(GatherDataMode.BY_CLIENT_JDBC.name());
          loadDataHistoryClientExp(chartRange, config.getChartInfo().getPullTimeoutClient());
        }
      }
      return;
    }

    loadDataHistoryCommon(chartRange, null);
  }

  private void loadDataHistoryCommon(ChartRange chartRange, Integer pullTimeout) {
    chartDataset.clear();
    Set<String> filteredSeries = new HashSet<>(series);

    if (Objects.isNull(seriesType) || SeriesType.COMMON.equals(seriesType)) {
      dataHandler.fillSeriesData(chartRange.getBegin(), chartRange.getEnd(), filteredSeries);
    }

    boolean applyFilter = topMapSelected != null
        && !topMapSelected.isEmpty()
        && topMapSelected.values().stream().anyMatch(set -> !set.isEmpty());

    double range = HelperChart.calculateRange(config.getMetric(), chartRange, config.getMaxPointsPerGraph());

    if (pullTimeout != null && range / 1000 < pullTimeout) {
      range = (double) pullTimeout * 1000;
    }

    double k = HelperChart.calculateK(range, config.getMetric().getNormFunction());

    for (long dtBegin = chartRange.getBegin(); dtBegin <= chartRange.getEnd(); dtBegin += Math.round(range)) {
      long dtEnd = dtBegin + Math.round(range) - 1;
      if (applyFilter) {
        dataHandler.handleFunction(dtBegin, dtEnd, k, filteredSeries, topMapSelected, stackedChart);
      } else {
        dataHandler.handleFunction(dtBegin, dtEnd, false, dtBegin, k, filteredSeries, stackedChart);
      }
    }
  }

  private void loadDataHistoryClientExp(ChartRange chartRange, Integer pullTimeout) {
    if (NormFunction.SECOND.equals(config.getMetric().getNormFunction())
        && TimeRangeFunction.AUTO.equals(config.getMetric().getTimeRangeFunction())) {
      loadDataHistoryCommonGaps(chartRange, pullTimeout);
    } else {
      loadDataHistoryCommon(chartRange, pullTimeout);
    }
  }

  private void loadDataHistoryCommonGaps(ChartRange chartRange, Integer pullTimeout) {
    log.info("Starting new history data loading algorithm for range: {} - {}", chartRange.getBegin(), chartRange.getEnd());
    chartDataset.clear();
    Set<String> filteredSeries = new HashSet<>(series);

    if (Objects.isNull(seriesType) || SeriesType.COMMON.equals(seriesType)) {
      dataHandler.fillSeriesData(chartRange.getBegin(), chartRange.getEnd(), filteredSeries);
    }

    boolean applyFilter = topMapSelected != null
        && !topMapSelected.isEmpty()
        && topMapSelected.values().stream().anyMatch(set -> !set.isEmpty());

    double globalRange = HelperChart.calculateRange(config.getMetric(), chartRange, config.getMaxPointsPerGraph());
    if (pullTimeout != null && globalRange / 1000 < pullTimeout) {
      globalRange = (double) pullTimeout * 1000;
    }
    double globalK = HelperChart.calculateK(globalRange, config.getMetric().getNormFunction());
    int batchSize = Math.toIntExact(Math.round((globalRange / 1000) / config.getChartInfo().getPullTimeoutClient()));

    log.info("Global parameters calculated: globalRange={}, globalK={}, batchSize={}", globalRange, globalK, batchSize);

    List<BlockKeyTail> blockKeyTails;
    try {
      blockKeyTails = dStore.getBlockKeyTailList(config.getQueryInfo().getName(), chartRange.getBegin(), chartRange.getEnd());
    } catch (BeginEndWrongOrderException e) {
      log.error("Error fetching BlockKeyTail list", e);
      return;
    } catch (SqlColMetadataException e) {
      throw new RuntimeException(e);
    }

    List<ContinuousRange> continuousRanges = new ArrayList<>();
    if (!blockKeyTails.isEmpty()) {
      BlockKeyTail rangeStart = blockKeyTails.getFirst();
      BlockKeyTail previousTail = blockKeyTails.getFirst();

      for (int i = 1; i < blockKeyTails.size(); i++) {
        BlockKeyTail currentTail = blockKeyTails.get(i);
        long gap = currentTail.getKey() - previousTail.getTail();

        if (gap > globalRange * GAP) {
          continuousRanges.add(new ContinuousRange(rangeStart, previousTail));
          log.debug("Detected continuous range: {} - {}", rangeStart.getKey(), previousTail.getTail());
          rangeStart = currentTail;
        }
        previousTail = currentTail;
      }
      continuousRanges.add(new ContinuousRange(rangeStart, previousTail));
      log.debug("Detected final continuous range: {} - {}", rangeStart.getKey(), previousTail.getTail());
    }

    long currentPosition = chartRange.getBegin();

    for (ContinuousRange range : continuousRanges) {
      long continuousStart = range.getStart().getKey();
      if (currentPosition < continuousStart) {
        log.info("Processing GAP range from {} to {}", currentPosition, continuousStart - 1);
        processGapRange(currentPosition, continuousStart - 1, globalRange, globalK, filteredSeries, applyFilter);
      }

      long continuousEnd = range.getEnd().getTail();
      if (continuousEnd - continuousStart > globalRange) {
        log.info("Processing CONTINUOUS range from {} to {} via loadDataHistoryClient", continuousStart, continuousEnd);
        loadDataHistoryForContinuousRange(new ChartRange(continuousStart, continuousEnd), globalRange, batchSize, filteredSeries);
      } else {
        log.info("Processing SMALL CONTINUOUS range from {} to {} via dataHandler", continuousStart, continuousEnd);
        if (applyFilter) {
          dataHandler.handleFunction(continuousStart, continuousEnd, globalK, filteredSeries, topMapSelected, stackedChart);
        } else {
          dataHandler.handleFunction(continuousStart, continuousEnd, false, continuousStart, globalK, filteredSeries, stackedChart);
        }
      }

      currentPosition = continuousEnd + 1;
    }

    if (currentPosition <= chartRange.getEnd()) {
      log.info("Processing FINAL GAP range from {} to {}", currentPosition, chartRange.getEnd());
      processGapRange(currentPosition, chartRange.getEnd(), globalRange, globalK, filteredSeries, applyFilter);
    }
  }

  private void processGapRange(long begin, long end, double range, double k, Set<String> filteredSeries, boolean applyFilter) {
    for (long dtBegin = begin; dtBegin <= end; dtBegin += Math.round(range)) {
      long dtEnd = dtBegin + Math.round(range) - 1;
      if (applyFilter) {
        dataHandler.handleFunction(dtBegin, dtEnd, k, filteredSeries, topMapSelected, stackedChart);
      } else {
        dataHandler.handleFunction(dtBegin, dtEnd, false, dtBegin, k, filteredSeries, stackedChart);
      }
    }
  }

  private void loadDataHistoryForContinuousRange(ChartRange chartRange, double range, int batchSize, Set<String> filteredSeries) {
    ChartDataLoader chartDataLoader = new ChartDataLoader(
        config.getMetric(),
        config.getChartInfo(),
        stackedChart,
        dataHandler,
        false,
        topMapSelected
    );
    chartDataLoader.setSeries(filteredSeries);

    chartDataLoader.setRange(range);
    chartDataLoader.setBatchSize(batchSize);

    try {
      List<BlockKeyTail> localBlockKeyTails = dStore.getBlockKeyTailList(config.getQueryInfo().getName(), chartRange.getBegin(), chartRange.getEnd());
      chartDataLoader.loadDataFromBdbToDeque(localBlockKeyTails);
      chartDataLoader.loadDataFromDequeToChart(chartRange.getBegin(), chartRange.getEnd());
    } catch (BeginEndWrongOrderException | SqlColMetadataException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  @Data
  @AllArgsConstructor
  private static class ContinuousRange {
    private BlockKeyTail start;
    private BlockKeyTail end;
  }

  @Deprecated
  private void loadDataHistoryClient(ChartRange chartRange) {
    chartDataset.clear();
    Set<String> filteredSeries = new HashSet<>(series);

    ChartDataLoader chartDataLoader = new ChartDataLoader(
        config.getMetric(),
        config.getChartInfo(),
        stackedChart,
        dataHandler,
        false,
        topMapSelected
    );
    chartDataLoader.setSeries(filteredSeries);

    double range = HelperChart.calculateRange(config.getMetric(), chartRange, config.getMaxPointsPerGraph());

    if (range / 1000 < config.getChartInfo().getPullTimeoutClient()) {
      range = (double) config.getChartInfo().getPullTimeoutClient() * 1000;
    }

    int batchSize = Math.toIntExact(Math.round((range / 1000) / config.getChartInfo().getPullTimeoutClient()));

    chartDataLoader.setRange(range);
    chartDataLoader.setBatchSize(batchSize);

    try {
      chartDataLoader.loadDataFromBdbToDeque(
          dStore.getBlockKeyTailList(config.getQueryInfo().getName(), chartRange.getBegin(), chartRange.getEnd())
      );
      chartDataLoader.loadDataFromDequeToChart(chartRange.getBegin(), chartRange.getEnd());
    } catch (BeginEndWrongOrderException | SqlColMetadataException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  private void loadDataHistoryServer(ChartRange chartRange) {
    loadDataHistoryCommon(chartRange, config.getChartInfo().getPullTimeoutServer());
  }

  @Override
  public void loadData() {
  }

  @Override
  public void addChartListenerReleaseMouse(IDetailPanel iDetailPanel) {
    stackedChart.addChartListenerReleaseMouse(iDetailPanel);
  }

  @Override
  public void removeChartListenerReleaseMouse(IDetailPanel iDetailPanel) {
    stackedChart.removeChartListenerReleaseMouse(iDetailPanel);
  }
}