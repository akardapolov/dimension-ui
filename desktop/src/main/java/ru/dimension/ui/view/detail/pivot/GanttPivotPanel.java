package ru.dimension.ui.view.detail.pivot;

import static ru.dimension.ui.laf.LafColorGroup.TABLE_BACKGROUND;
import static ru.dimension.ui.laf.LafColorGroup.TABLE_FONT;

import com.egantt.model.drawing.DrawingState;
import ext.egantt.swing.GanttDrawingPartHelper;
import ext.egantt.swing.GanttTable;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swingx.JXTableTables;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.gantt.DrawingScale;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.detail.GanttPivotCommon;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;

@Log4j2
public abstract class GanttPivotPanel extends GanttPivotCommon {

  protected JXTable combinedTable;
  protected JSplitPane jSplitPane;

  protected TTTable<ColumnRow, JXTable> columnTable;

  public GanttPivotPanel(TableInfo tableInfo,
                         CProfile cProfile,
                         long begin,
                         long end,
                         Map<String, Color> seriesColorMap) {
    super(tableInfo, cProfile, begin, end, seriesColorMap);

    this.combinedTable = new JXTable();

    initUI();
  }

  @Override
  protected void initUI() {
    this.jSplitPane = new JSplitPane();
    this.jSplitPane.setDividerLocation(DIVIDER_LOCATION);

    TTRegistry registry = TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();

    this.columnTable = createProfileTable(registry);
    populateProfileTable();
  }

  private TTTable<ColumnRow, JXTable> createProfileTable(TTRegistry registry) {
    TTTable<ColumnRow, JXTable> tt = JXTableTables.create(
        registry,
        ColumnRow.class,
        TableUi.<ColumnRow>builder()
            .rowIcon(ModelIconProviders.forColumnRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));
    table.setEditable(false);

    // Hide ID
    if (table.getColumnExt("ID") != null) {
      table.getColumnExt("ID").setVisible(false);
    }

    // HIDE PICK (Checkbox)
    if (table.getColumnExt("Pick") != null) {
      table.getColumnExt("Pick").setVisible(false);
    }
    if (table.getColumnExt("pick") != null) {
      table.getColumnExt("pick").setVisible(false);
    }

    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    return tt;
  }

  private void populateProfileTable() {
    if (tableInfo == null || tableInfo.getCProfiles() == null) {
      columnTable.setItems(List.of());
      return;
    }

    List<ColumnRow> rows = tableInfo.getCProfiles().stream()
        .filter(cp -> !cp.getCsType().isTimeStamp())
        .map(cp -> new ColumnRow(cp, false))
        .collect(Collectors.toList());

    columnTable.setItems(rows);
  }

  @Override
  protected JXTable loadGantt(CProfile firstLevelGroupBy,
                              List<GanttColumnCount> ganttColumnList,
                              Map<String, Color> seriesColorMap,
                              DrawingScale drawingScale,
                              int visibleRowCount,
                              int rowHeightForJTable) {
    return null;
  }

  protected JXTable loadPivotGantt(CProfile firstGrpBy,
                                   CProfile secondGrpBy,
                                   List<GanttColumnCount> ganttColumnList1,
                                   List<GanttColumnCount> ganttColumnList2,
                                   Map<String, Color> seriesColorMap,
                                   int visibleRowCount,
                                   int rowHeightForJTable,
                                   Boolean isColumnSortable) {

    DrawingScale drawingScale1 = new DrawingScale();
    DrawingScale drawingScale2 = new DrawingScale();

    String[][] columnNames = createColumnNames(ganttColumnList2);

    sortGanttColumns(ganttColumnList1);
    sortGanttColumns(ganttColumnList2);

    Object[][] data = createData(drawingScale1, drawingScale2, ganttColumnList1, ganttColumnList2, secondGrpBy, firstGrpBy);

    GanttTable ganttTable = new GanttTable(
        data,
        columnNames,
        getBasicJTableList(),
        seriesColorMap,
        isColumnSortable
    );

    setGanttTableParameters(visibleRowCount, rowHeightForJTable, ganttTable);
    setTooltipAndPercent(ganttTable);

    ganttTable.getJXTable().setShowVerticalLines(true);
    ganttTable.getJXTable().setShowHorizontalLines(true);
    ganttTable.getJXTable().setBackground(LaF.getBackgroundColor(TABLE_BACKGROUND, LaF.getLafType()));
    ganttTable.getJXTable().setForeground(LaF.getBackgroundColor(TABLE_FONT, LaF.getLafType()));

    return ganttTable.getJXTable();
  }

  protected JXTable loadPivotGanttSum(CProfile firstGrpBy,
                                      CProfile secondGrpBy,
                                      List<GanttColumnCount> ganttColumnList1,
                                      List<GanttColumnCount> ganttColumnList2,
                                      Map<String, Color> seriesColorMap,
                                      int visibleRowCount,
                                      int rowHeightForJTable,
                                      Boolean isColumnSortable) {

    DrawingScale drawingScale1 = new DrawingScale();
    DrawingScale drawingScale2 = new DrawingScale();

    SumPivotData sumPivotData = buildSumPivotData(ganttColumnList1);

    ganttColumnList1.sort((a, b) -> Double.compare(
        sumPivotData.rowTotals.getOrDefault(b.getKey(), 0.0d),
        sumPivotData.rowTotals.getOrDefault(a.getKey(), 0.0d)
    ));

    ganttColumnList2.sort((a, b) -> Double.compare(
        sumPivotData.colTotals.getOrDefault(b.getKey(), 0.0d),
        sumPivotData.colTotals.getOrDefault(a.getKey(), 0.0d)
    ));

    String[][] columnNames = createColumnNames(ganttColumnList2);

    Object[][] data = createDataSum(
        drawingScale1,
        drawingScale2,
        ganttColumnList1,
        ganttColumnList2,
        secondGrpBy,
        firstGrpBy,
        sumPivotData
    );

    GanttTable ganttTable = new GanttTable(
        data,
        columnNames,
        getBasicJTableList(),
        seriesColorMap,
        isColumnSortable
    );

    setGanttTableParameters(visibleRowCount, rowHeightForJTable, ganttTable);
    setTooltipAndPercent(ganttTable);

    ganttTable.getJXTable().setShowVerticalLines(true);
    ganttTable.getJXTable().setShowHorizontalLines(true);
    ganttTable.getJXTable().setBackground(LaF.getBackgroundColor(TABLE_BACKGROUND, LaF.getLafType()));
    ganttTable.getJXTable().setForeground(LaF.getBackgroundColor(TABLE_FONT, LaF.getLafType()));

    return ganttTable.getJXTable();
  }

  protected JXTable loadPivotGanttSumTotal(CProfile firstGrpBy,
                                           CProfile secondGrpBy,
                                           List<GanttColumnCount> ganttColumnList1,
                                           List<GanttColumnCount> ganttColumnList2,
                                           Map<String, Color> seriesColorMap,
                                           int visibleRowCount,
                                           int rowHeightForJTable,
                                           Boolean isColumnSortable) {

    DrawingScale drawingScale1 = new DrawingScale();
    DrawingScale drawingScale2 = new DrawingScale();

    SumPivotData sumPivotData = buildSumPivotData(ganttColumnList1);

    ganttColumnList2.sort((a, b) -> Double.compare(
        sumPivotData.colTotals.getOrDefault(b.getKey(), 0.0d),
        sumPivotData.colTotals.getOrDefault(a.getKey(), 0.0d)
    ));

    String[][] columnNames = createColumnNames(ganttColumnList2);

    Object[][] data = createDataSumTotal(
        drawingScale1,
        drawingScale2,
        ganttColumnList2,
        secondGrpBy,
        firstGrpBy,
        sumPivotData
    );

    GanttTable ganttTable = new GanttTable(
        data,
        columnNames,
        getBasicJTableList(),
        seriesColorMap,
        isColumnSortable
    );

    setGanttTableParameters(visibleRowCount, rowHeightForJTable, ganttTable);
    setTooltipAndPercent(ganttTable);

    ganttTable.getJXTable().setShowVerticalLines(true);
    ganttTable.getJXTable().setShowHorizontalLines(true);
    ganttTable.getJXTable().setBackground(LaF.getBackgroundColor(TABLE_BACKGROUND, LaF.getLafType()));
    ganttTable.getJXTable().setForeground(LaF.getBackgroundColor(TABLE_FONT, LaF.getLafType()));

    return ganttTable.getJXTable();
  }

  private static final class SumPivotData {
    private final Map<String, Map<String, Double>> rowToColSums = new HashMap<>();
    private final Map<String, Double> rowTotals = new HashMap<>();
    private final Map<String, Double> colTotals = new HashMap<>();
    private double totalAll = 0.0d;
  }

  private SumPivotData buildSumPivotData(List<GanttColumnCount> ganttColumnList1) {
    SumPivotData d = new SumPivotData();

    for (GanttColumnCount row : ganttColumnList1) {
      String rowKey = row.getKey();
      double rowNumeric = safeParseDouble(rowKey);

      Map<String, Double> rowMap = d.rowToColSums.computeIfAbsent(rowKey, k -> new HashMap<>());

      for (Map.Entry<String, Integer> e : row.getGantt().entrySet()) {
        String colKey = e.getKey();
        int count = (e.getValue() == null) ? 0 : e.getValue();

        double sum = rowNumeric * (double) count;

        if (sum != 0.0d) {
          rowMap.put(colKey, sum);
        }

        d.rowTotals.merge(rowKey, sum, Double::sum);
        d.colTotals.merge(colKey, sum, Double::sum);
        d.totalAll += sum;
      }
    }

    return d;
  }

  private Object[][] createDataSum(DrawingScale drawingScale1,
                                   DrawingScale drawingScale2,
                                   List<GanttColumnCount> ganttColumnList1,
                                   List<GanttColumnCount> ganttColumnList2,
                                   CProfile secondGrpBy,
                                   CProfile firstGrpBy,
                                   SumPivotData sumData) {

    final GanttDrawingPartHelper partHelper = new GanttDrawingPartHelper();

    String hLine = secondGrpBy.getColName();
    String vLine = firstGrpBy.getColName();

    int rowCount = ganttColumnList1.size();
    int colCount = ganttColumnList2.size();

    Object[][] data = new Object[rowCount + 3][colCount + 3];

    data[0][0] = createDrawingStateHeader(partHelper, "Pivot table");
    data[1][0] = vLine;
    data[0][1] = hLine;
    data[1][1] = "";

    data[0][colCount + 2] = "";
    data[1][colCount + 2] = "TOTAL";
    data[rowCount + 2][0] = "";
    data[rowCount + 2][1] = "TOTAL";

    double totalAll = sumData.totalAll;

    for (int col = 0; col < colCount; col++) {
      String colKey = ganttColumnList2.get(col).getKey();
      double colTotal = sumData.colTotals.getOrDefault(colKey, 0.0d);

      Map<String, Double> singleCategory = Collections.singletonMap(colKey, colTotal);
      DrawingState state = createDrawingStateSum(drawingScale1, partHelper, colKey, singleCategory, totalAll);

      data[0][col + 2] = state;
      data[1][col + 2] = colKey;
    }

    for (int row = 0; row < rowCount; row++) {
      String rowKey = ganttColumnList1.get(row).getKey();
      Map<String, Double> rowBreakdown = sumData.rowToColSums.getOrDefault(rowKey, Collections.emptyMap());

      DrawingState state = createDrawingStateSum(drawingScale2, partHelper, rowKey, rowBreakdown, totalAll);
      data[row + 2][0] = state;
      data[row + 2][1] = rowKey;
    }

    for (int row = 0; row < rowCount; row++) {
      String rowKey = ganttColumnList1.get(row).getKey();
      double totalRowSum = sumData.rowTotals.getOrDefault(rowKey, 0.0d);

      for (int col = 0; col < colCount; col++) {
        String colKey = ganttColumnList2.get(col).getKey();

        Double cell = sumData.rowToColSums
            .getOrDefault(rowKey, Collections.emptyMap())
            .get(colKey);

        if (cell != null && cell != 0.0d) {
          data[row + 2][col + 2] = normalizeDouble(cell);
        } else {
          data[row + 2][col + 2] = null;
        }
      }

      data[row + 2][colCount + 2] = normalizeDouble(totalRowSum);
    }

    for (int col = 0; col < colCount; col++) {
      String colKey = ganttColumnList2.get(col).getKey();
      double totalColSum = sumData.colTotals.getOrDefault(colKey, 0.0d);
      data[rowCount + 2][col + 2] = normalizeDouble(totalColSum);
    }

    data[rowCount + 2][colCount + 2] = normalizeDouble(totalAll);

    return data;
  }

  private Object[][] createDataSumTotal(DrawingScale drawingScale1,
                                        DrawingScale drawingScale2,
                                        List<GanttColumnCount> ganttColumnList2,
                                        CProfile secondGrpBy,
                                        CProfile firstGrpBy,
                                        SumPivotData sumData) {

    final GanttDrawingPartHelper partHelper = new GanttDrawingPartHelper();

    String hLine = secondGrpBy.getColName();
    String vLine = firstGrpBy.getColName();

    int rowCount = 1;
    int colCount = ganttColumnList2.size();

    Object[][] data = new Object[rowCount + 3][colCount + 3];

    data[0][0] = createDrawingStateHeader(partHelper, "Pivot table");
    data[1][0] = vLine;
    data[0][1] = hLine;
    data[1][1] = "";

    data[0][colCount + 2] = "";
    data[1][colCount + 2] = "TOTAL";
    data[rowCount + 2][0] = "";
    data[rowCount + 2][1] = "TOTAL";

    double totalAll = sumData.totalAll;

    for (int col = 0; col < colCount; col++) {
      String colKey = ganttColumnList2.get(col).getKey();
      double colTotal = sumData.colTotals.getOrDefault(colKey, 0.0d);

      Map<String, Double> singleCategory = Collections.singletonMap(colKey, colTotal);
      DrawingState state = createDrawingStateSum(drawingScale1, partHelper, colKey, singleCategory, totalAll);

      data[0][col + 2] = state;
      data[1][col + 2] = colKey;
    }

    String totalRowLabel = "TOTAL";

    DrawingState rowState = createDrawingStateSum(drawingScale2, partHelper, totalRowLabel, sumData.colTotals, totalAll);
    data[2][0] = rowState;
    data[2][1] = totalRowLabel;

    double rowTotal = 0.0d;
    for (int col = 0; col < colCount; col++) {
      String colKey = ganttColumnList2.get(col).getKey();
      double colTotal = sumData.colTotals.getOrDefault(colKey, 0.0d);

      if (colTotal != 0.0d) {
        data[2][col + 2] = normalizeDouble(colTotal);
      } else {
        data[2][col + 2] = null;
      }
      rowTotal += colTotal;
    }

    data[2][colCount + 2] = normalizeDouble(rowTotal);

    for (int col = 0; col < colCount; col++) {
      data[rowCount + 2][col + 2] = null;
    }
    data[rowCount + 2][colCount + 2] = null;

    return data;
  }

  private static double safeParseDouble(String s) {
    if (s == null) {
      return 0.0d;
    }
    String trimmed = s.trim();
    if (trimmed.isEmpty()) {
      return 0.0d;
    }
    String normalized = trimmed.replace(" ", "").replace(',', '.');
    try {
      return Double.parseDouble(normalized);
    } catch (Exception ex) {
      return 0.0d;
    }
  }

  private static Double normalizeDouble(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      return 0.0d;
    }
    BigDecimal bd = BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
    return bd.doubleValue();
  }

  private Object[][] createData(DrawingScale drawingScale1,
                                DrawingScale drawingScale2,
                                List<GanttColumnCount> ganttColumnList1,
                                List<GanttColumnCount> ganttColumnList2,
                                CProfile secondGrpBy,
                                CProfile firstGrpBy) {

    final GanttDrawingPartHelper partHelper = new GanttDrawingPartHelper();

    long countOfAllRowsId = ganttColumnList1.stream()
        .map(GanttColumnCount::getGantt)
        .map(Map::values)
        .flatMap(java.util.Collection::stream)
        .mapToInt(Integer::intValue)
        .sum();

    String hLine = secondGrpBy.getColName();
    String vLine = firstGrpBy.getColName();

    Object[][] data = new Object[ganttColumnList1.size() + 3][ganttColumnList2.size() + 3];

    data[0][0] = createDrawingStateHeader(partHelper, "Pivot table");
    data[1][0] = vLine;
    data[0][1] = hLine;
    data[1][1] = "";

    data[0][ganttColumnList2.size() + 2] = "";
    data[1][ganttColumnList2.size() + 2] = "TOTAL";
    data[ganttColumnList1.size() + 2][0] = "";
    data[ganttColumnList1.size() + 2][1] = "TOTAL";

    for (int col = 0; col < ganttColumnList2.size(); col++) {
      data[0][col + 2] = createDrawingState(drawingScale1, partHelper, ganttColumnList2.get(col), countOfAllRowsId);
      data[1][col + 2] = ganttColumnList2.get(col).getKey();
    }

    for (int row = 0; row < ganttColumnList1.size(); row++) {
      data[row + 2][0] = createDrawingState(drawingScale2, partHelper, ganttColumnList1.get(row), countOfAllRowsId);
      data[row + 2][1] = ganttColumnList1.get(row).getKey();
    }

    int[][] result = new int[ganttColumnList1.size()][ganttColumnList2.size() + 1];
    for (int row = 2; row < ganttColumnList1.size() + 2; row++) {
      String rowName = ganttColumnList1.get(row - 2).getKey();
      int totalRowSum = 0;
      for (int column = 2; column < ganttColumnList2.size() + 3; column++) {
        if (column != ganttColumnList2.size() + 2) {
          String colName = ganttColumnList2.get(column - 2).getKey();
          for (GanttColumnCount ganttColumn : ganttColumnList1) {
            if (rowName != null && rowName.equals(ganttColumn.getKey())) {
              Map<String, Integer> gantt = ganttColumn.getGantt();
              Integer value = gantt.getOrDefault(colName, 0);
              result[row - 2][column - 2] = value;
              if (value != 0) {
                data[row][column] = value;
                totalRowSum += value;
              } else {
                data[row][column] = null;
              }
              break;
            }
          }
        } else {
          data[row][column] = String.valueOf(totalRowSum);
          result[row - 2][column - 2] = totalRowSum;
        }
      }
    }

    for (int c = 2; c < ganttColumnList2.size() + 3; c++) {
      int totalColumnSum = 0;
      for (int r = 2; r < ganttColumnList1.size() + 2; r++) {
        totalColumnSum += result[r - 2][c - 2];
      }
      data[ganttColumnList1.size() + 2][c] = String.valueOf(totalColumnSum);
    }

    return data;
  }

  private DrawingState createDrawingStateHeader(GanttDrawingPartHelper helper, String name) {
    var state = helper.createDrawingState();
    var part = helper.createDrawingPart(false);
    var textLayer = helper.createDrawingPart(true);

    helper.createActivityEntry(new StringBuffer(""), new java.util.Date(0), new java.util.Date(100),
                               ext.egantt.drawing.module.BasicPainterModule.BASIC_STRING_PAINTER, TEXT_PAINTER, textLayer);
    helper.createActivityEntry(name, new java.util.Date(0), new java.util.Date(100), "0XFA0124Of", part);

    state.addDrawingPart(part);
    state.addDrawingPart(textLayer);

    return state;
  }
}