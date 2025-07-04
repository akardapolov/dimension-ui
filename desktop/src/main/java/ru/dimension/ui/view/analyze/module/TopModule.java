package ru.dimension.ui.view.analyze.module;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import ext.egantt.swing.GanttDrawingPartHelper;
import ext.egantt.swing.GanttTable;
import java.awt.Color;
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
import java.util.stream.Collectors;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.GanttColumnNotSupportedException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.output.GanttColumnSum;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.table.BType;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jdesktop.swingx.VerticalLayout;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.function.MetricFunction;
import ru.dimension.ui.model.gantt.DrawingScale;
import ru.dimension.ui.model.gantt.GanttColumn;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.analyze.router.Message;
import ru.dimension.ui.view.analyze.router.MessageAction;
import ru.dimension.ui.view.analyze.router.MessageRouter;
import ru.dimension.ui.view.analyze.router.MessageRouter.Action;
import ru.dimension.ui.view.analyze.router.MessageRouter.Destination;
import ru.dimension.ui.view.detail.GanttCommon;
import ru.dimension.ui.model.view.SeriesType;

@Log4j2
public class TopModule extends GanttCommon implements MessageAction {

  private final MessageRouter router;

  private final Metric metric;

  protected Map<CProfile, LinkedHashSet<String>> topMapFilterSelected;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private SeriesType seriesType;

  public TopModule(MessageRouter router,
                   TableInfo tableInfo,
                   Metric metric,
                   Map<String, Color> seriesColorMap) {
    super(tableInfo, metric.getYAxis(), 0L, 0L, seriesColorMap);

    this.router = router;

    this.metric = metric;

    this.topMapFilterSelected = new HashMap<>();

    PGHelper.cellXYRemainder(this, new JPanel(), false);
  }

  @Override
  public void receive(Message message) {
    if (message.getAction() == Action.LOAD_TOP) {
      DStore dStore = message.getParameters().get("dStore");
      begin = message.getParameters().get("begin");
      end = message.getParameters().get("end");

      seriesType = message.getParameters().get("seriesType");

      if (seriesType == null || SeriesType.COMMON.equals(seriesType)) {
        loadDataToDetail(dStore, begin, end);
      } else {
        log.info("Message action not fired for series type: " + seriesType);
      }

      log.info("Message action: " + message.getAction() + " for begin: " + begin + " and for end: " + end);
    }

    if (message.getAction() == Action.CLEAR_TOP) {
      clearTop();

      log.info("Message action: " + message.getAction());
    }
  }

  public void loadDataToDetail(DStore dStore,
                               long begin,
                               long end) {
    SwingTaskRunner.runWithProgress(
        this,
        executor,
        () -> {
          if (BType.BERKLEYDB.equals(tableInfo.getBackendType())) {
            JPanel topPanel = loadTopPanel(begin, end, dStore);
            JScrollPane scrollPane = GUIHelper.getJScrollPane(topPanel, 15);
            return () -> PGHelper.cellXYRemainder(this, scrollPane, false);
          } else {
            return () -> PGHelper.cellXYRemainder(this, new JPanel(), false);
          }
        },
        log::catching,
        () -> ProgressBarHelper.createProgressBar("Loading, please wait...", 150, 30)
    );
  }

  public void clearTop() {
    SwingTaskRunner.runWithProgress(
        this,
        executor,
        () -> () -> PGHelper.cellXYRemainder(this, new JPanel(), false),
        log::catching,
        () -> ProgressBarHelper.createProgressBar("Loading, please wait...", 150, 30)
    );
  }

  private JPanel loadTopPanel(long begin,
                              long end,
                              DStore dStore) {
    JPanel topPanelAll = new JPanel(new VerticalLayout());

    try {
      topPanelAll.add(new JXTitledSeparator(cProfile.getColName()));
      topPanelAll.add(getJScrollPane(getTopJXTable(cProfile, begin, end, dStore)));

      tableInfo.getCProfiles().forEach(cProfile -> {
        try {
          if (!cProfile.getColName().equals(this.cProfile.getColName())
              && !cProfile.getCsType().isTimeStamp()) {
            topPanelAll.add(new JXTitledSeparator(cProfile.getColName()));
            topPanelAll.add(getJScrollPane(getTopJXTable(cProfile, begin, end, dStore)));
          }
        } catch (Exception exception) {
          log.catching(exception);
        }
      });
      topPanelAll.repaint();

    } catch (Exception exception) {
      log.catching(exception);
    }

    return topPanelAll;
  }

  private JXTable getTopJXTable(CProfile cProfile,
                                long begin,
                                long end,
                                DStore dStore)
      throws BeginEndWrongOrderException, GanttColumnNotSupportedException, SqlColMetadataException {
    topMapFilterSelected.computeIfAbsent(cProfile, k -> new LinkedHashSet<>());

    CProfile firstLevelGroupBy = tableInfo.getCProfiles().stream()
        .filter(f -> f.getColName().equalsIgnoreCase(cProfile.getColName()))
        .findFirst()
        .orElseThrow();

    List<GanttColumn> ganttColumnList = loadGanttData(firstLevelGroupBy, dStore, begin, end);

    DrawingScale drawingScale = new DrawingScale();

    JXTable jxTable = loadGantt(firstLevelGroupBy, ganttColumnList, seriesColorMap, drawingScale, 5, 20);
    jxTable.getColumnExt(1).setVisible(false);

    DefaultCellEditor topPickEditor = new DefaultCellEditor(new JCheckBox());
    jxTable.getColumnModel().getColumn(1).setCellEditor(topPickEditor);

    topPickEditor.addCellEditorListener(new TopCheckboxListener(jxTable, firstLevelGroupBy, topMapFilterSelected));

    return jxTable;
  }

  private List<GanttColumn> loadGanttData(CProfile firstLevelGroupBy, DStore dStore, long begin, long end)
      throws BeginEndWrongOrderException, GanttColumnNotSupportedException, SqlColMetadataException {
    if (MetricFunction.COUNT.equals(metric.getMetricFunction())) {
      if (Objects.isNull(seriesType) || SeriesType.COMMON.equals(seriesType)) {
        return convertGanttColumns(dStore.getGantt(tableInfo.getTableName(), firstLevelGroupBy, cProfile, begin, end));
      } else if (SeriesType.CUSTOM.equals(seriesType)) {
        return convertGanttColumns(dStore.getGantt(tableInfo.getTableName(),
                                                   firstLevelGroupBy,
                                                   cProfile,
                                                   cProfile,
                                                   seriesColorMap.keySet().toArray(String[]::new),
                                                   CompareFunction.EQUAL,
                                                   begin,
                                                   end));
      }
    } else {
      return convertSumToGanttColumns(dStore.getGanttSum(tableInfo.getTableName(), firstLevelGroupBy, cProfile, begin, end));
    }

    return Collections.emptyList();
  }

  private List<GanttColumn> convertSumToGanttColumns(List<GanttColumnSum> sumColumns) {
    return sumColumns.stream()
        .map(sc -> {
          GanttColumn gc = new GanttColumn();
          gc.setKey(sc.getKey());
          gc.setGantt(new HashMap<>(Map.of(cProfile.getColName(), sc.getValue())));
          return gc;
        })
        .collect(Collectors.toList());
  }

  private List<GanttColumn> convertGanttColumns(List<GanttColumnCount> ganttColumns) {
    return ganttColumns.stream()
        .map(gc -> GanttColumn.builder()
            .key(gc.getKey())
            .gantt(gc.getGantt().entrySet().stream()
                       .collect(Collectors.toMap(
                           Map.Entry::getKey,
                           e -> e.getValue().doubleValue())))
            .build())
        .collect(Collectors.toList());
  }

  @Override
  protected JXTable loadGantt(CProfile firstLevelGroupBy,
                              List<GanttColumn> ganttColumnList,
                              Map<String, Color> seriesColorMap,
                              DrawingScale drawingScale,
                              int visibleRowCount,
                              int rowHeightForJTable) {

    String[][] columnNames = {{"Activity %", firstLevelGroupBy.getColName(), MetricsColumnNames.PICK.getColName()}};

    sortGanttColumns(ganttColumnList);

    Object[][] data = createData(firstLevelGroupBy, columnNames, ganttColumnList, drawingScale);

    GanttTable ganttTable = new GanttTable(
        data,
        columnNames,
        getBasicJTableList(),
        seriesColorMap);

    setGanttTableParameters(visibleRowCount, rowHeightForJTable, ganttTable);

    setTooltipAndPercent(ganttTable);

    TableColumn col2 = ganttTable.getJXTable().getColumnModel().getColumn(2);
    col2.setMinWidth(30);
    col2.setMaxWidth(35);

    ganttTable.getJXTable().setShowVerticalLines(true);
    ganttTable.getJXTable().setShowHorizontalLines(true);
    ganttTable.getJXTable().setBackground(LaF.getBackgroundColor(LafColorGroup.TABLE_BACKGROUND, LaF.getLafType()));
    ganttTable.getJXTable().setForeground(LaF.getBackgroundColor(LafColorGroup.TABLE_FONT, LaF.getLafType()));

    return ganttTable.getJXTable();
  }

  private Object[][] createData(CProfile firstLevelGroupBy,
                                String[][] columnNames,
                                List<GanttColumn> ganttColumnList,
                                DrawingScale drawingScale) {

    Object[][] data = new Object[ganttColumnList.size()][columnNames[0].length];

    final GanttDrawingPartHelper partHelper = new GanttDrawingPartHelper();

    double countOfAllRowsId = ganttColumnList.stream()
        .map(GanttColumn::getGantt)
        .map(Map::values)
        .flatMap(Collection::stream)
        .mapToDouble(Double::doubleValue)
        .sum();

    AtomicInteger atomicInteger = new AtomicInteger(0);

    ganttColumnList.forEach(ganttColumn -> {
      int rowNumber = atomicInteger.getAndIncrement();
      data[rowNumber][0] = createDrawingState(drawingScale, partHelper, ganttColumn, countOfAllRowsId);
      data[rowNumber][1] = ganttColumn.getKey();

      if (topMapFilterSelected.get(firstLevelGroupBy).contains(ganttColumn.getKey())) {
        data[rowNumber][2] = true;
      } else {
        data[rowNumber][2] = false;
      }
    });

    return data;
  }

  @Override
  protected void initUI() {}

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
        if (cValue) {
          topMapSelected.get(cProfile).add(getValue());

          router.sendMessage(Message.builder()
                                 .destination(Destination.CHART_LIST)
                                 .action(Action.ADD_CHART_FILTER)
                                 .parameter("metric", metric)
                                 .parameter("cProfileFilter", cProfile)
                                 .parameter("filter", Collections.singletonList(getValue()))
                                 .parameter("seriesColorMap", seriesColorMap)
                                 .build());
        } else {
          topMapSelected.get(cProfile).remove(getValue());

          router.sendMessage(Message.builder()
                                 .destination(Destination.CHART_LIST)
                                 .action(Action.REMOVE_CHART_FILTER)
                                 .parameter("metric", metric)
                                 .parameter("cProfileFilter", cProfile)
                                 .parameter("filter", Collections.singletonList(getValue()))
                                 .build());
        }
      }
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