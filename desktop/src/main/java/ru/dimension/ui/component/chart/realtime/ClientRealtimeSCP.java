package ru.dimension.ui.component.chart.realtime;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.OrderBy;
import ru.dimension.db.model.filter.CompositeFilter;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.ChartDataLoader;
import ru.dimension.ui.component.module.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.helper.FilterHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class ClientRealtimeSCP extends RealtimeSCP {

  private final ChartDataLoader chartDataLoader;
  protected Map<CProfile, LinkedHashSet<String>> topMapSelected;

  private JTextField seriesSearch;
  private TableRowSorter<?> seriesSorter;

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
                           Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    super(config, profileTaskQueryKey);

    super.setSqlQueryState(sqlQueryState);
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

    chartDataLoader = new ChartDataLoader(config.getMetric(), config.getChartInfo(), stackedChart, dataHandler, true);
  }

  @Override
  public void initialize() {
    log.info(getHourMinSec(config.getQueryInfo().getDeltaLocalServerTime()));

    disablePlotUpdates();

    ChartRange chartRange = getBeginEndRange();

    try {
      if (GroupFunction.COUNT.equals(config.getMetric().getGroupFunction())) {
        List<String> distinct = dStore.getDistinct(
            config.getQueryInfo().getName(),
            config.getMetric().getYAxis(),
            OrderBy.DESC,
            null,
            MAX_SERIES,
            chartRange.getBegin(),
            chartRange.getEnd()
        );

        boolean useCustom = distinct.size() > THRESHOLD_SERIES;
        if (useCustom) {
          seriesType = SeriesType.CUSTOM;
          int showSeries = SHOW_SERIES;

          initializeSeriesSelectableTable(distinct, showSeries);

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

    setCustomFilter();

    chartDataLoader.setSeries(series);
    chartDataLoader.setTopMapSelected(topMapSelected);

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
      chartDataLoader.loadDataFromBdbToDeque(dStore.getBlockKeyTailList(config.getQueryInfo().getName(),
                                                                        chartDataLoader.getClientBegin(),
                                                                        currRange.getEnd()));
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

  protected void initializeSeriesSelectableTable(List<String> distinct,
                                                 int showSeries) {
    Consumer<String> runAction = this::runAction;
    String[] columnNames = {ColumnNames.NAME.getColName(), ColumnNames.PICK.getColName()};
    int nameId = 0;
    int checkBoxId = 1;

    this.seriesSelectable = GUIHelper.getJXTableCaseCheckBoxAdHoc(10, columnNames, checkBoxId);
    TableColumn colMetric = seriesSelectable.getJxTable().getColumnModel().getColumn(checkBoxId);
    colMetric.setMinWidth(40);
    colMetric.setMaxWidth(45);

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

    // Initialize row sorter for search
    seriesSorter = new TableRowSorter<>(seriesSelectable.getJxTable().getModel());
    seriesSelectable.getJxTable().setRowSorter(seriesSorter);
  }

  protected void initializeWithSeriesTable() {
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

      chartDataLoader.loadDataFromBdbToDeque(dStore.getBlockKeyTailList(config.getQueryInfo()
                                                                            .getName(), currRange.getBegin(), currRange.getEnd()));

      chartDataLoader.loadDataFromDequeToChart(currRange.getBegin(), currRange.getEnd());
    } catch (Exception e) {
      log.error("Error reloading data", e);
    }
  }

  @Override
  public Map<CProfile, LinkedHashSet<String>> getFilter() {
    return topMapSelected;
  }

  public void reinitializeChartInCustomMode() {
    this.removeAll();

    series.clear();

    clearSeriesColor();

    seriesType = SeriesType.CUSTOM;

    disablePlotUpdates();
    initializeSeriesComponents();

    if (detailAndAnalyzeHolder != null) {
      detailAndAnalyzeHolder.detailAction().cleanMainPanel();
    }

    reloadDataForCurrentRange();

    if (detailAndAnalyzeHolder != null) {
      detailAndAnalyzeHolder.detailAction().cleanMainPanel();
      detailAndAnalyzeHolder.customAction().setCustomSeriesFilter(topMapSelected, getSeriesColorMap());
    }

    enablePlotUpdates();
  }

  private void initializeSeriesComponents() {
    try {
      ChartRange chartRange = getBeginEndRange();
      List<String> distinct = fetchDistinctSeries(chartRange);

      boolean useCustom = seriesType == SeriesType.CUSTOM || distinct.size() > THRESHOLD_SERIES;

      if (useCustom) {
        seriesType = SeriesType.CUSTOM;

        initializeSeriesSelectableTable(distinct, SHOW_SERIES);

        series.clear();
        series.addAll(getCheckBoxSelected());
      }

      setCustomFilter();

      chartDataLoader.setSeries(series);
      chartDataLoader.setTopMapSelected(topMapSelected);

      initializeWithSeriesTable();

    } catch (BeginEndWrongOrderException e) {
      log.error("Error reinitializing chart", e);
    }
  }

  private List<String> fetchDistinctSeries(ChartRange chartRange) throws BeginEndWrongOrderException {
    if (topMapSelected != null) {
      CompositeFilter compositeFilter = FilterHelper.toCompositeFilter(topMapSelected);

      return dStore.getDistinct(
          config.getQueryInfo().getName(),
          config.getMetric().getYAxis(),
          OrderBy.DESC,
          compositeFilter,
          MAX_SERIES,
          chartRange.getBegin(),
          chartRange.getEnd());
    } else {
      return dStore.getDistinct(
          config.getQueryInfo().getName(),
          config.getMetric().getYAxis(),
          OrderBy.DESC,
          null,
          MAX_SERIES,
          chartRange.getBegin(),
          chartRange.getEnd()
      );
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

          if (topMapSelected.get(config.getMetric().getYAxis()).isEmpty()) {
            topMapSelected.putIfAbsent(config.getMetric().getYAxis(), new LinkedHashSet<>(newSelection));
          } else {
            topMapSelected.get(config.getMetric().getYAxis()).clear();
            topMapSelected.get(config.getMetric().getYAxis()).addAll(newSelection);
          }

          chartDataLoader.setSeries(series);
          chartDataLoader.setTopMapSelected(topMapSelected);

          if (detailAndAnalyzeHolder != null) {
            detailAndAnalyzeHolder.detailAction().cleanMainPanel();
          }

          reloadDataForCurrentRange();

          if (detailAndAnalyzeHolder != null) {
            detailAndAnalyzeHolder.detailAction().cleanMainPanel();
            detailAndAnalyzeHolder.customAction().setCustomSeriesFilter(topMapSelected, getSeriesColorMap());
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

  protected void setCustomFilter() {
    if (seriesType == SeriesType.CUSTOM) {
      Set<String> newSelection = new HashSet<>(getCheckBoxSelected());

      if (topMapSelected != null) {
        topMapSelected.clear();
        topMapSelected.putIfAbsent(config.getMetric().getYAxis(), new LinkedHashSet<>(newSelection));
      } else {
        topMapSelected = new HashMap<>() {{
          put(config.getMetric().getYAxis(), new LinkedHashSet<>(newSelection));
        }};
      }

      dataHandler.setFilter(topMapSelected);
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
}
