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
import java.util.stream.Collectors;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swingx.JXTableTables;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.gantt.DrawingScale;
import ru.dimension.ui.model.gantt.GanttColumn;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.detail.GanttCommon;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;

@Log4j2
public abstract class GanttPanel extends GanttCommon {

  protected JSplitPane jSplitPane;

  protected TTTable<ColumnRow, JXTable> columnTable;

  public GanttPanel(TableInfo tableInfo,
                    CProfile cProfile,
                    long begin,
                    long end,
                    Map<String, Color> seriesColorMap) {
    super(tableInfo, cProfile, begin, end, seriesColorMap);
    initUI();
  }

  @Override
  protected JXTable loadGantt(CProfile firstLevelGroupBy,
                              List<GanttColumn> ganttColumnList,
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

    JXTable jxTable = ganttTable.getJXTable();

    jxTable.setShowGrid(true);
    jxTable.setShowVerticalLines(true);
    jxTable.setShowHorizontalLines(true);

    jxTable.setGridColor(Color.GRAY);
    jxTable.setIntercellSpacing(new java.awt.Dimension(1, 1));

    jxTable.setBackground(LaF.getBackgroundColor(TABLE_BACKGROUND, LaF.getLafType()));
    jxTable.setForeground(LaF.getBackgroundColor(TABLE_FONT, LaF.getLafType()));

    return jxTable;
  }

  protected abstract JScrollPane loadDimensionTop();

  @Override
  protected void initUI() {
    this.jSplitPane = new JSplitPane();
    this.jSplitPane.setDividerLocation(DIVIDER_LOCATION);

    TTRegistry registry = TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();

    this.columnTable = JXTableTables.create(
        registry,
        ColumnRow.class,
        TableUi.<ColumnRow>builder()
            .rowIcon(ModelIconProviders.forColumnRow())
            .rowIconInColumn("name")
            .build()
    );

    configureColumnTable();
    populateColumnTable();
  }

  private void configureColumnTable() {
    JXTable table = this.columnTable.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));
    table.setEditable(false);

    if (table.getColumnExt("ID") != null) {
      table.getColumnExt("ID").setVisible(false);
    }
    if (table.getColumnExt("Pick") != null) {
      table.getColumnExt("Pick").setVisible(false);
    }
    if (table.getColumnExt("pick") != null) {
      table.getColumnExt("pick").setVisible(false);
    }
  }

  private void populateColumnTable() {
    if (this.tableInfo != null && this.tableInfo.getCProfiles() != null) {
      List<ColumnRow> rows = this.tableInfo.getCProfiles().stream()
          .filter(cProfile -> !cProfile.getCsType().isTimeStamp())
          .map(cProfile -> new ColumnRow(cProfile, false))
          .collect(Collectors.toList());
      this.columnTable.setItems(rows);
    }
  }

  private Object[][] createData(String[][] columnNames,
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
}