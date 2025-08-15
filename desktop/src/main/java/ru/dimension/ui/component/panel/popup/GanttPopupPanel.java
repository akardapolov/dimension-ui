package ru.dimension.ui.component.panel.popup;

import static ru.dimension.ui.helper.GUIHelper.getJScrollPane;

import ext.egantt.swing.GanttDrawingPartHelper;
import ext.egantt.swing.GanttTable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.RowFilter;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.GanttColumnNotSupportedException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.output.GanttColumnSum;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.table.BType;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Block;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.AdHocChartKey;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.gantt.DrawingScale;
import ru.dimension.ui.model.gantt.GanttColumn;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.state.ChartKey;
import ru.dimension.ui.view.detail.GanttCommon;

@Log4j2
public class GanttPopupPanel extends GanttCommon {
  private final MessageBroker.Component component;

  public interface SelectionChangeListener {
    void onSelectionChange(boolean hasSelection);
  }

  @Setter
  private SelectionChangeListener selectionChangeListener;

  @Setter
  private Panel panelType;

  @Setter
  private ChartKey chartKey;
  @Setter
  private TableInfo tableInfo;
  @Setter
  private Metric metric;
  @Setter
  private DStore dStore;
  @Setter
  private long begin;
  @Setter
  private long end;
  @Setter
  private SeriesType seriesType;
  @Setter
  private Map<String, Color> seriesColorMap = new HashMap<>();

  @Setter
  @Getter
  private JXTable currentTable;

  @Getter
  private final Map<CProfile, LinkedHashSet<String>> filterSelectedMap = new HashMap<>();

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public GanttPopupPanel(MessageBroker.Component component) {
    super();

    this.component = component;

    PGHelper.cellXYRemainder(this, new JPanel(), false);
  }

  public void loadData(CProfile column, Map<String, Color> seriesColorMap) {
    final JXTable[] tableHolder = new JXTable[1];

    SwingTaskRunner.runWithProgress(
        this,
        executor,
        () -> {
          if (BType.BERKLEYDB.equals(tableInfo.getBackendType())) {
            tableHolder[0] = getTopJXTable(column, seriesColorMap);
            return () -> {
              JScrollPane scrollPane = getJScrollPane(tableHolder[0], 10);
              PGHelper.cellXYRemainder(this, scrollPane, false);
              this.currentTable = tableHolder[0];
            };
          } else {
            tableHolder[0] = getTopJXTable(column, seriesColorMap);
            return () -> {
              JScrollPane scrollPane = getJScrollPane(tableHolder[0], 10);
              PGHelper.cellXYRemainder(this, scrollPane, false);
              this.currentTable = tableHolder[0];
            };
          }
        },
        log::catching,
        () -> ProgressBarHelper.createProgressBar("Loading, please wait...", 150, 30)
    );
  }

  public void filter(String pattern) {
    if (currentTable == null) return;

    TableRowSorter<?> sorter = (TableRowSorter<?>) currentTable.getRowSorter();
    if (sorter == null) {
      sorter = new TableRowSorter<>(currentTable.getModel());
      currentTable.setRowSorter(sorter);
    }

    if (pattern == null || pattern.isEmpty()) {
      sorter.setRowFilter(null);
    } else {
      try {
        sorter.setRowFilter(RowFilter.regexFilter("(?iu)" + pattern, 1));
      } catch (PatternSyntaxException e) {
        sorter.setRowFilter(RowFilter.regexFilter("$^", 1));
      }
    }
  }

  private JXTable getTopJXTable(CProfile cProfile, Map<String, Color> seriesColorMap)
      throws BeginEndWrongOrderException, GanttColumnNotSupportedException, SqlColMetadataException {
    filterSelectedMap.computeIfAbsent(cProfile, k -> new LinkedHashSet<>());

    List<GanttColumn> ganttColumnList = loadGanttData(cProfile);

    DrawingScale drawingScale = new DrawingScale();

    JXTable jxTable = loadGantt(cProfile, ganttColumnList, seriesColorMap, drawingScale, 5, 20);
    if (jxTable != null) {
      jxTable.getColumnExt(ColumnNames.NAME.ordinal()).setVisible(true);

      DefaultCellEditor topPickEditor = new DefaultCellEditor(new JCheckBox());
      for (int i = 0; i < jxTable.getColumnCount(); i++) {
        TableColumn column = jxTable.getColumnModel().getColumn(i);
        if (column.getModelIndex() == 2) {
          column.setCellEditor(topPickEditor);
          break;
        }
      }

      topPickEditor.addCellEditorListener(new TopCheckboxListener(jxTable, cProfile, filterSelectedMap));
    }

    return jxTable;
  }

  private List<GanttColumn> loadGanttData(CProfile column)
      throws BeginEndWrongOrderException, GanttColumnNotSupportedException, SqlColMetadataException {
    if (dStore == null || tableInfo == null || metric == null)
      return Collections.emptyList();

    if (MetricFunction.COUNT.equals(metric.getMetricFunction())) {
      if (Objects.isNull(seriesType) || SeriesType.COMMON.equals(seriesType)) {
        return convertGanttColumns(dStore.getGantt(tableInfo.getTableName(),
                                                   column,
                                                   metric.getYAxis(),
                                                   begin,
                                                   end));
      } else if (SeriesType.CUSTOM.equals(seriesType)) {
        return convertGanttColumns(dStore.getGantt(tableInfo.getTableName(),
                                                   column,
                                                   metric.getYAxis(),
                                                   metric.getYAxis(),
                                                   seriesColorMap.keySet().toArray(String[]::new),
                                                   CompareFunction.EQUAL,
                                                   begin,
                                                   end));
      }
    } else {
      return convertSumToGanttColumns(dStore.getGanttSum(tableInfo.getTableName(),
                                                         column,
                                                         metric.getYAxis(),
                                                         begin,
                                                         end));
    }
    return Collections.emptyList();
  }

  @Override
  protected JXTable loadGantt(CProfile firstLevelGroupBy,
                              List<GanttColumn> ganttColumnList,
                              Map<String, Color> seriesColorMap,
                              DrawingScale drawingScale,
                              int visibleRowCount,
                              int rowHeightForJTable) {

    if (ganttColumnList == null || ganttColumnList.isEmpty()) {
      return null;
    }

    String[][] columnNames = {{"Activity %", firstLevelGroupBy.getColName(), ColumnNames.PICK.getColName()}};

    sortGanttColumns(ganttColumnList);

    Object[][] data = createData(firstLevelGroupBy, columnNames, ganttColumnList, drawingScale);

    GanttTable ganttTable = new GanttTable(
        data,
        columnNames,
        getBasicJTableList(),
        seriesColorMap);

    setGanttTableParameters(visibleRowCount, rowHeightForJTable, ganttTable);

    setTooltipAndPercent(ganttTable);

    JXTable jxTable = ganttTable.getJXTable();
    if (jxTable != null) {
      TableColumn col2 = jxTable.getColumnModel().getColumn(2);
      col2.setMinWidth(30);
      col2.setMaxWidth(35);

      jxTable.setShowVerticalLines(true);
      jxTable.setShowHorizontalLines(true);
      jxTable.setBackground(LaF.getBackgroundColor(LafColorGroup.TABLE_BACKGROUND, LaF.getLafType()));
      jxTable.setForeground(LaF.getBackgroundColor(LafColorGroup.TABLE_FONT, LaF.getLafType()));
    }

    return jxTable;
  }

  @Override
  protected void initUI() {}

  private Object[][] createData(CProfile firstLevelGroupBy,
                                String[][] columnNames,
                                List<GanttColumn> ganttColumnList,
                                DrawingScale drawingScale) {

    Object[][] data = new Object[ganttColumnList.size()][columnNames[0].length];

    final GanttDrawingPartHelper partHelper = new GanttDrawingPartHelper();

    double countOfAllRowsId = ganttColumnList.stream()
        .map(GanttColumn::getGantt)
        .filter(Objects::nonNull)
        .map(Map::values)
        .flatMap(Collection::stream)
        .mapToDouble(Double::doubleValue)
        .sum();

    AtomicInteger atomicInteger = new AtomicInteger(0);

    ganttColumnList.forEach(ganttColumn -> {
      int rowNumber = atomicInteger.getAndIncrement();
      data[rowNumber][0] = createDrawingState(drawingScale, partHelper, ganttColumn, countOfAllRowsId);
      data[rowNumber][1] = ganttColumn.getKey();

      LinkedHashSet<String> selectedSet = filterSelectedMap.get(firstLevelGroupBy);
      if (selectedSet != null && selectedSet.contains(ganttColumn.getKey())) {
        data[rowNumber][2] = true;
      } else {
        data[rowNumber][2] = false;
      }
    });

    return data;
  }

  protected void sortGanttColumns(List<GanttColumn> ganttColumnList) {
    ganttColumnList.sort((g1, g2) -> {
      Double g1Double = g1.getGantt().values().stream().mapToDouble(Double::doubleValue).sum();
      Double g2Double = g2.getGantt().values().stream().mapToDouble(Double::doubleValue).sum();
      return g2Double.compareTo(g1Double);
    });
  }

  private List<GanttColumn> convertSumToGanttColumns(List<GanttColumnSum> sumColumns) {
    return sumColumns.stream()
        .map(sc -> {
          GanttColumn gc = new GanttColumn();
          gc.setKey(sc.getKey());
          gc.setGantt(new HashMap<>(Map.of(metric.getYAxis().getColName(), sc.getValue())));
          return gc;
        })
        .collect(Collectors.toList());
  }

  private List<GanttColumn> convertGanttColumns(List<GanttColumnCount> ganttColumns) {
    return ganttColumns.stream().map(gc -> GanttColumn.builder()
            .key(gc.getKey())
            .gantt(gc.getGantt().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, e -> e.getValue().doubleValue())))
            .build())
        .collect(Collectors.toList());
  }

  public void clear() {
    PGHelper.cellXYRemainder(this, new JPanel(), false);
    this.revalidate();
    this.repaint();

    currentTable = null;
    filterSelectedMap.clear();
  }

  class TopCheckboxListener implements CellEditorListener {

    private final JXTable jxTable;
    private final CProfile cProfile;
    private final Map<CProfile, LinkedHashSet<String>> topMapSelected;

    public TopCheckboxListener(JXTable jxTable,
                               CProfile cProfile,
                               Map<CProfile, LinkedHashSet<String>> topMapSelected) {
      this.jxTable = jxTable;
      this.cProfile = cProfile;
      this.topMapSelected = topMapSelected;
    }

    @Override
    public void editingStopped(ChangeEvent e) {
      TableCellEditor editor = (TableCellEditor) e.getSource();
      Boolean cValue = (Boolean) editor.getCellEditorValue();

      if (jxTable.getSelectedRow() != -1) {
        String filterValue = getValue();

        if (filterValue == null) {
          return;
        }

        LinkedHashSet<String> selectedSet = topMapSelected.computeIfAbsent(cProfile, k -> new LinkedHashSet<>());

        if (cValue) {
          selectedSet.add(filterValue);
        } else {
          selectedSet.remove(filterValue);
        }

        Destination destination =
            panelType == Panel.REALTIME ?
                getDestination(component, Panel.REALTIME, chartKey) :
                getDestination(component, Panel.HISTORY, chartKey);

        List<String> selectedValues = new ArrayList<>(selectedSet);

        if (!selectedValues.isEmpty()) {
          sendFilterMessage(destination, Action.ADD_CHART_FILTER, selectedValues);
        } else {
          sendFilterMessage(destination, Action.REMOVE_CHART_FILTER, Collections.emptyList());
        }

        if (selectionChangeListener != null) {
          selectionChangeListener.onSelectionChange(!selectedSet.isEmpty());
        }
      }
    }

    private static Destination getDestination(MessageBroker.Component component,
                                              Panel panel,
                                              ChartKey chartKey) {
      if (chartKey instanceof AdHocChartKey key) {
        return Destination.builder()
            .component(component)
            .module(Module.CHART)
            .panel(panel)
            .block(Block.CHART)
            .chartKey(key)
            .build();
      }

      return Destination.builder()
          .component(component)
          .module(Module.CHART)
          .panel(panel)
          .block(Block.CHART)
          .chartKey(chartKey)
          .build();
    }

    private void sendFilterMessage(Destination destination, Action action, List<String> filterValues) {
      log.info("Message send to " + destination + " with action: " + action);

      MessageBroker broker = MessageBroker.getInstance();
      broker.sendMessage(Message.builder()
                             .destination(destination)
                             .action(action)
                             .parameter("cProfileFilter", cProfile)
                             .parameter("filter", filterValues)
                             .parameter("seriesColorMap", seriesColorMap)
                             .build());
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
    }

    private String getValue() {
      if (jxTable.getSelectedRow() != -1) {
        int modelRow = jxTable.convertRowIndexToModel(jxTable.getSelectedRow());
        Object valueFromModel = jxTable.getModel().getValueAt(modelRow, 1);

        return valueFromModel != null ? valueFromModel.toString() : "";
      }
      return "";
    }
  }
}