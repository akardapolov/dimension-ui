package ru.dimension.ui.view.detail.top;

import static ru.dimension.ui.laf.LafColorGroup.TABLE_BACKGROUND;
import static ru.dimension.ui.laf.LafColorGroup.TABLE_FONT;

import ext.egantt.swing.GanttDrawingPartHelper;
import ext.egantt.swing.GanttTable;
import java.awt.Color;
import java.awt.Font;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.profile.CProfile;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.column.MetricsColumnNames;
import ru.dimension.ui.model.gantt.DrawingScale;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.detail.GanttPivotCommon;

@Log4j2
public abstract class GanttReportPanel extends GanttPivotCommon {

  protected JSplitPane jSplitPane;
  protected JXTableCase jxTableCase;

  public GanttReportPanel(TableInfo tableInfo,
                          CProfile cProfile,
                          long begin,
                          long end,
                          Map<String, Color> seriesColorMap) {
    super(tableInfo, cProfile, begin, end, seriesColorMap);

    initUI();
  }

  @Override
  protected JXTable loadGantt(CProfile firstLevelGroupBy,
                              List<GanttColumnCount> ganttColumnList,
                              Map<String, Color> seriesColorMap,
                              DrawingScale drawingScale,
                              int visibleRowCount,
                              int rowHeightForJTable) {

    String[][] columnNames = {{"Activity %", firstLevelGroupBy.getColName()}};

    sortGanttColumns(ganttColumnList);

    Object[][] data = createData(columnNames, ganttColumnList, drawingScale);

    GanttTable ganttTable = new GanttTable(
        data,
        columnNames,
        getBasicJTableList(),
        seriesColorMap);

    setGanttTableParameters(visibleRowCount, rowHeightForJTable, ganttTable);

    setTableHeaderFont(ganttTable, new Font("Arial", Font.BOLD, 14));

    setTooltipAndPercent(ganttTable);

    ganttTable.getJXTable().setShowVerticalLines(true);
    ganttTable.getJXTable().setShowHorizontalLines(true);
    ganttTable.getJXTable().setBackground(LaF.getBackgroundColor(TABLE_BACKGROUND, LaF.getLafType()));
    ganttTable.getJXTable().setForeground(LaF.getBackgroundColor(TABLE_FONT, LaF.getLafType()));

    return ganttTable.getJXTable();
  }

  protected abstract JScrollPane loadDimensionTop();

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

  private Object[][] createData(String[][] columnNames,
                                List<GanttColumnCount> ganttColumnList,
                                DrawingScale drawingScale) {
    Object[][] data = new Object[ganttColumnList.size()][columnNames[0].length];

    final GanttDrawingPartHelper partHelper = new GanttDrawingPartHelper();

    long countOfAllRowsId = ganttColumnList.stream()
        .map(GanttColumnCount::getGantt)
        .map(Map::values)
        .flatMap(Collection::stream)
        .mapToInt(Integer::intValue)
        .sum();

    AtomicInteger atomicInteger = new AtomicInteger(0);

    ganttColumnList.forEach(ganttColumn -> {
      int rowNumber = atomicInteger.getAndIncrement();
      data[rowNumber][0] = createDrawingState(drawingScale, partHelper, ganttColumn, countOfAllRowsId);
      data[rowNumber][1] = ganttColumn.getKey();
    });

    return data;
  }
}