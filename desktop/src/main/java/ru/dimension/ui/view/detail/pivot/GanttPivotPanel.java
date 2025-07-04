package ru.dimension.ui.view.detail.pivot;

import static ru.dimension.ui.laf.LafColorGroup.TABLE_BACKGROUND;
import static ru.dimension.ui.laf.LafColorGroup.TABLE_FONT;

import com.egantt.model.drawing.DrawingState;
import com.egantt.model.drawing.part.ListDrawingPart;
import com.egantt.model.drawing.state.BasicDrawingState;
import ext.egantt.drawing.module.BasicPainterModule;
import ext.egantt.swing.GanttDrawingPartHelper;
import ext.egantt.swing.GanttTable;
import java.awt.Color;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.profile.CProfile;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.gantt.DrawingScale;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.detail.GanttPivotCommon;

@Log4j2
public abstract class GanttPivotPanel extends GanttPivotCommon {

  protected JXTable combinedTable;
  protected JSplitPane jSplitPane;
  protected JXTableCase jxTableCase;

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
    this.jxTableCase = GUIHelper.getJXTableCase(5,
                                                new String[]{MetricsColumnNames.ID.getColName(),
                                                    MetricsColumnNames.COLUMN_NAME.getColName()});
    this.jxTableCase.getJxTable().getColumnExt(0).setVisible(false);
    this.jxTableCase.getJxTable().getColumnModel().getColumn(0)
        .setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());

    this.tableInfo.getCProfiles().forEach(cProfile -> {
      if (!cProfile.getCsType().isTimeStamp()) {
        if (!cProfile.getColName().equalsIgnoreCase(this.cProfile.getColName())) {
          this.jxTableCase.getDefaultTableModel().addRow(new Object[]{cProfile.getColId(), cProfile.getColName()});
        }
      }
    });
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
        isColumnSortable);

    setGanttTableParameters(visibleRowCount, rowHeightForJTable, ganttTable);

    setTooltipAndPercent(ganttTable);

    ganttTable.getJXTable().setShowVerticalLines(true);
    ganttTable.getJXTable().setShowHorizontalLines(true);
    ganttTable.getJXTable().setBackground(LaF.getBackgroundColor(TABLE_BACKGROUND, LaF.getLafType()));
    ganttTable.getJXTable().setForeground(LaF.getBackgroundColor(TABLE_FONT, LaF.getLafType()));

    return ganttTable.getJXTable();
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
        .flatMap(Collection::stream)
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

    //row =0,1 and all column
    for (int col = 0; col < ganttColumnList2.size(); col++) {
      data[0][col + 2] = createDrawingState(drawingScale1, partHelper, ganttColumnList2.get(col), countOfAllRowsId);
      data[1][col + 2] = ganttColumnList2.get(col).getKey();
    }

    //column=0,1 and all row
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
              if (rowName.equals(ganttColumn.getKey())) {
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

  private DrawingState createDrawingStateHeader(GanttDrawingPartHelper helper,
                                                String name) {
    BasicDrawingState state = helper.createDrawingState();
    ListDrawingPart part = helper.createDrawingPart(false);
    ListDrawingPart textLayer = helper.createDrawingPart(true);

    helper.createActivityEntry(new StringBuffer(""), new Date(0), new Date(100),
                               BasicPainterModule.BASIC_STRING_PAINTER, TEXT_PAINTER, textLayer);
    helper.createActivityEntry(name, new Date(0), new Date(100), "0XFA0124Of", part);

    state.addDrawingPart(part);
    state.addDrawingPart(textLayer);

    return state;
  }
}
