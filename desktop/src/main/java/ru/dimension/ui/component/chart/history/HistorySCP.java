package ru.dimension.ui.component.chart.history;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jfree.chart.util.IDetailPanel;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.chart.ChartConfig;
import ru.dimension.ui.component.chart.FunctionDataHandler;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.module.analyze.handler.TableSelectionHandler;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.chart.ChartRange;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.function.GroupFunction;
import ru.dimension.ui.model.function.NormFunction;
import ru.dimension.ui.model.function.TimeRangeFunction;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.SeriesType;

@Log4j2
public class HistorySCP extends SCP {

  protected FunctionDataHandler dataHandler;
  @Getter
  protected Map<CProfile, LinkedHashSet<String>> topMapSelected;

  private JXTableCase seriesSelectable;
  private ExecutorService executorService;
  private JTextField seriesSearch;
  private TableRowSorter<?> seriesSorter;

  public HistorySCP(ChartConfig config,
                    ProfileTaskQueryKey profileTaskQueryKey) {
    super(config, profileTaskQueryKey);
  }

  public HistorySCP(DStore dStore,
                    ChartConfig config,
                    ProfileTaskQueryKey profileTaskQueryKey,
                    Map<CProfile, LinkedHashSet<String>> topMapSelected) {
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
    chartDataset.clear();
    Set<String> filteredSeries = new HashSet<>(series);

    if (Objects.isNull(seriesType) || SeriesType.COMMON.equals(seriesType)) {
      dataHandler.fillSeriesData(chartRange.getBegin(), chartRange.getEnd(), filteredSeries);
    }

    boolean applyFilter = topMapSelected != null
        && !topMapSelected.isEmpty()
        && topMapSelected.values().stream().anyMatch(set -> !set.isEmpty());

    TimeRangeFunction timeRangeFunction = config.getMetric().getTimeRangeFunction();
    double range = switch (timeRangeFunction) {
      case MINUTE -> 60 * 1000;
      case HOUR -> 60 * 60 * 1000;
      case DAY -> 24 * 60 * 60 * 1000;
      case MONTH -> calculateMonthlyRange(chartRange);
      default -> calculateAutoRange(chartRange);
    };

    NormFunction normFunction = config.getMetric().getNormFunction();
    log.info("Normalization function: " + normFunction);

    for (long dtBegin = chartRange.getBegin(); dtBegin <= chartRange.getEnd(); dtBegin += Math.round(range)) {
      long dtEnd = dtBegin + Math.round(range) - 1;

      double k = switch (normFunction) {
        case NONE -> 1.0;
        case SECOND -> (double) Math.round(range) / 1000;
        case MINUTE -> (double) Math.round(range) / (60 * 1000);
        case HOUR -> (double) Math.round(range) / (60 * 60 * 1000);
        case DAY -> (double) Math.round(range) / (24 * 60 * 60 * 1000);
      };

      if (applyFilter) {
        dataHandler.handleFunction(dtBegin, dtEnd, k, filteredSeries, topMapSelected, stackedChart);
      } else {
        dataHandler.handleFunction(dtBegin, dtEnd, false, dtBegin, k, filteredSeries, stackedChart);
      }
    }
  }

  private long calculateMonthlyRange(ChartRange chartRange) {
    long totalMillis = chartRange.getEnd() - chartRange.getBegin();
    long totalMonths = ChronoUnit.MONTHS.between(
        Instant.ofEpochMilli(chartRange.getBegin()).atZone(ZoneId.systemDefault()).toLocalDate(),
        Instant.ofEpochMilli(chartRange.getEnd()).atZone(ZoneId.systemDefault()).toLocalDate()
    );

    if (totalMonths == 0) {
      return totalMillis;
    }

    return totalMillis / totalMonths;
  }

  private double calculateAutoRange(ChartRange chartRange) {
    if (config.getChartInfo().getCustomBegin() != 0 & config.getChartInfo().getCustomEnd() != 0) {
      return (double) (chartRange.getEnd() - chartRange.getBegin()) / config.getMaxPointsPerGraph();
    }
    return (double) getRangeHistory(config.getChartInfo()) / config.getMaxPointsPerGraph();
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