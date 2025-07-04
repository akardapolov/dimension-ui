package ru.dimension.ui.view.detail;

import com.egantt.model.drawing.ContextResources;
import com.egantt.model.drawing.DrawingState;
import com.egantt.model.drawing.part.ListDrawingPart;
import com.egantt.model.drawing.state.BasicDrawingState;
import com.egantt.swing.component.ComponentResources;
import com.egantt.swing.component.context.BasicComponentContext;
import com.egantt.swing.component.tooltip.ToolTipState;
import com.egantt.swing.table.list.BasicJTableList;
import ext.egantt.drawing.module.BasicPainterModule;
import ext.egantt.drawing.painter.context.BasicPainterContext;
import ext.egantt.swing.GanttDrawingPartHelper;
import ext.egantt.swing.GanttTable;
import java.awt.Color;
import java.awt.Font;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.model.gantt.DrawingScale;
import ru.dimension.ui.model.gantt.GanttColumn;
import ru.dimension.ui.model.info.TableInfo;

@Log4j2
public abstract class GanttCommon extends JPanel {

  protected static final String TEXT_PAINTER = "TxtPainter";
  protected static final int DIVIDER_LOCATION = 200;

  protected final TableInfo tableInfo;
  protected final CProfile cProfile;
  protected long begin;
  protected long end;
  protected final Map<String, Color> seriesColorMap;

  public GanttCommon(TableInfo tableInfo,
                     CProfile cProfile,
                     long begin,
                     long end,
                     Map<String, Color> seriesColorMap) {
    this.tableInfo = tableInfo;
    this.cProfile = cProfile;
    this.begin = begin;
    this.end = end;
    this.seriesColorMap = seriesColorMap;
  }

  protected abstract JXTable loadGantt(CProfile firstLevelGroupBy,
                                       List<GanttColumn> ganttColumnList,
                                       Map<String, Color> seriesColorMap,
                                       DrawingScale drawingScale,
                                       int visibleRowCount,
                                       int rowHeightForJTable);

  protected abstract void initUI();

  protected JScrollPane getJScrollPane(JXTable jxTable) {
    JScrollPane jScrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    jScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    jScrollPane.setViewportView(jxTable);
    jScrollPane.setVerticalScrollBar(jScrollPane.getVerticalScrollBar());

    return jScrollPane;
  }

  protected DrawingState createDrawingState(DrawingScale drawingScale,
                                            GanttDrawingPartHelper helper,
                                            GanttColumn me,
                                            double countOfEntries) {

    Random random = new SecureRandom();

    BasicDrawingState state = helper.createDrawingState();
    ListDrawingPart part = helper.createDrawingPart(false);
    ListDrawingPart textLayer = helper.createDrawingPart(true);

    double countPerEntry = me.getGantt().values().stream().mapToDouble(Double::doubleValue).sum();

    double percent = round(countPerEntry / countOfEntries * 100, 2);

    String percentText = "" + percent + "%";
    String keyAndPercentText = me.getKey() + " " + percentText;

    if (drawingScale.getPercentPrev() == 0) {
      if (percent > 70) {
        drawingScale.setScaleToggle(0);
      } else if (percent < 70 && percent > 30) {
        drawingScale.setScaleToggle(1);
      } else if (percent < 30) {
        drawingScale.setScaleToggle(2);
      }
    }

    if (percent < 0.6) {
      // Show only percent
      helper.createActivityEntry(new StringBuffer(percentText), new Date(0), new Date(100), BasicPainterModule.BASIC_STRING_PAINTER, TEXT_PAINTER, textLayer);
      helper.createActivityEntry(keyAndPercentText, new Date(0), new Date(100), String.valueOf(random.nextLong()), part);

      state.addDrawingPart(part);
      state.addDrawingPart(textLayer);
      return state;
    }

    final boolean[] isBreak = {false};
    final long[] start = {0};

    drawingScale.setScale(getScale(drawingScale.getScaleToggle(), percent));

    me.getGantt().entrySet().stream()
        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
        .forEach(entry -> {
          if (!isBreak[0]) {
            String keyString = entry.getKey();
            double value = entry.getValue();

            // Show only not zero activities.
            if (value != 0) {
              double currentGroupPercentNotScale = (value / countPerEntry) * percent;
              double currentGroupPercent = currentGroupPercentNotScale * drawingScale.getScale();

              if (currentGroupPercent < 1.0 && currentGroupPercent >= 0.6) {
                currentGroupPercent = round(currentGroupPercent, 0);
              }

              long currGroupPercentL = (long) round(currentGroupPercent, 0);

              // Set tooltip
              final StringBuffer o = new StringBuffer();
              {
                o.append("<HTML>");
                o.append("<b>").append(keyString).append(" ")
                    .append(round(currentGroupPercentNotScale, 2)).append("%").append("</b>");
                o.append("</HTML>");
              }

              // Exit when previous egantt < than current egantt graph
              if (drawingScale.getPercentPrev() != 0 &&
                  (start[0] + currGroupPercentL) > drawingScale.getPercentPrev()) {
                currGroupPercentL = drawingScale.getPercentPrev() - start[0];
                helper.createActivityEntry(o, new Date(start[0]), new Date(
                    start[0] + currGroupPercentL), keyString, part);
                start[0] = start[0] + currGroupPercentL;
                isBreak[0] = true;
              }

              // If row only one
              if (!isBreak[0]) {
                if (currentGroupPercent == 100) {
                  helper.createActivityEntry(o, new Date(start[0]), new Date(currGroupPercentL), keyString, part);
                } else {
                  helper.createActivityEntry(o, new Date(start[0]), new Date(
                      start[0] + currGroupPercentL), keyString, part);
                  start[0] = start[0] + currGroupPercentL;
                }
              }
            }
          }
        });

    // Show percent
    helper.createActivityEntry(new StringBuffer(percentText), new Date(start[0]), new Date(100), BasicPainterModule.BASIC_STRING_PAINTER, TEXT_PAINTER, textLayer);
    helper.createActivityEntry(keyAndPercentText, new Date(start[0]), new Date(100), String.valueOf(random.nextLong()), part);

    state.addDrawingPart(part);
    state.addDrawingPart(textLayer);

    drawingScale.setPercentPrev(start[0]);

    return state;
  }

  static public double round(double d,
                             int decimalPlace) {
    BigDecimal bd;
    try {
      bd = new BigDecimal(Double.toString(d));
      bd = bd.setScale(decimalPlace, RoundingMode.HALF_UP);
      return bd.doubleValue();
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  static public double getScale(int scaleToggle,
                                double percent) {
    return switch (scaleToggle) {
      case 0 -> 125 / (percent + 51);
      case 1 -> 147 / (percent + 51);
      case 2 -> 200 / (percent + 51);
      default -> 130 / (percent + 51);
    };
  }

  protected BasicJTableList getBasicJTableList() {
    BasicJTableList tableList = new BasicJTableList();
    {
      BasicComponentContext componentContext = new BasicComponentContext();
      ToolTipState state = (event, cellState) -> {
        DrawingState drawing = cellState.getDrawing();
        Object key = drawing != null ? drawing.getValueAt(event.getPoint()) : null;
        if (key == null) {
          return "";
        }
        return key.toString();
      };
      componentContext.put(ComponentResources.TOOLTIP_STATE, state);
      tableList.setRendererComponentContext(componentContext);
    }
    return tableList;
  }

  protected void setTooltipAndPercent(GanttTable gantttable) {
    final String textPainter = TEXT_PAINTER;
    BasicPainterContext graphics = new BasicPainterContext();
    graphics.setPaint(Color.BLACK);
    graphics.put(textPainter, new Font(null, Font.BOLD, 10));
    gantttable.getDrawingContext().put(textPainter,
                                       ContextResources.GRAPHICS_CONTEXT, graphics);
  }

  protected static void setGanttTableParameters(int visibleRowCount,
                                                int rowHeightForJTable,
                                                GanttTable ganttTable) {
    ganttTable.setVisibleRowCount(visibleRowCount);
    ganttTable.setRowHeightForJTable(rowHeightForJTable);
    ganttTable.getJXTable().setColumnControlVisible(true);
    ganttTable.getJXTable().setCellSelectionEnabled(true);
  }

  protected String[][] createColumnNames(List<GanttColumn> ganttColumnList2) {
    String[][] columnNames = new String[1][ganttColumnList2.size() + 3];
    for (int index = 0; index < ganttColumnList2.size(); index++) {
      columnNames[0][index] = null;
    }
    return columnNames;
  }

  protected void sortGanttColumns(List<GanttColumn> ganttColumnList) {
    ganttColumnList.sort((g1, g2) -> {
      Double g1Double = g1.getGantt().values().stream().mapToDouble(Double::doubleValue).sum();
      Double g2Double = g2.getGantt().values().stream().mapToDouble(Double::doubleValue).sum();
      return g2Double.compareTo(g1Double);
    });
  }

  protected void setTableHeaderFont(GanttTable ganttTable,
                                    Font font) {
    ganttTable.getJXTable().getTableHeader().setFont(font);
  }
}