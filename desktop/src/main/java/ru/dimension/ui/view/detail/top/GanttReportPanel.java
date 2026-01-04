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
public abstract class GanttReportPanel extends GanttPivotCommon {

  protected JSplitPane jSplitPane;

  // was: protected JXTableCase jxTableCase;
  protected TTTable<ColumnRow, JXTable> columnTable;

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

    if (table.getColumnExt("ID") != null) table.getColumnExt("ID").setVisible(false);
    if (table.getColumnExt("Pick") != null) table.getColumnExt("Pick").setVisible(false);
    if (table.getColumnExt("pick") != null) table.getColumnExt("pick").setVisible(false);

    table.getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    return tt;
  }

  private void populateProfileTable() {
    if (tableInfo == null || tableInfo.getCProfiles() == null) {
      columnTable.setItems(List.of());
      return;
    }

    List<ColumnRow> rows = tableInfo.getCProfiles().stream()
        .filter(cp -> !cp.getCsType().isTimeStamp())
        .filter(cp -> !cp.getColName().equalsIgnoreCase(this.cProfile.getColName()))
        .map(cp -> new ColumnRow(cp, false))
        .collect(Collectors.toList());

    columnTable.setItems(rows);
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