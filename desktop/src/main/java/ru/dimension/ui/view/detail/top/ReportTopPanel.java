package ru.dimension.ui.view.detail.top;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.GanttColumnNotSupportedException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.profile.CProfile;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.ProgressBarHelper;
import ru.dimension.ui.model.column.TaskColumnNames;
import ru.dimension.ui.model.gantt.DrawingScale;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.view.detail.HelperGantt;

@Log4j2
public class ReportTopPanel extends GanttReportPanel implements ListSelectionListener, HelperGantt {

  private final DStore dStore;

  private final SeriesType seriesType;

  private final ScheduledExecutorService executorService;

  public ReportTopPanel(DStore dStore,
                        TableInfo tableInfo,
                        CProfile cProfile,
                        SeriesType seriesType,
                        long begin,
                        long end,
                        Map<String, Color> seriesColorMap) {
    super(tableInfo, cProfile, begin, end, seriesColorMap);

    this.dStore = dStore;
    this.seriesType = seriesType;
    this.executorService = Executors.newSingleThreadScheduledExecutor();

    super.jxTableCase.getJxTable().getSelectionModel().addListSelectionListener(this);

    this.jSplitPane.add(this.jxTableCase.getJScrollPane(), JSplitPane.LEFT);
    this.jSplitPane.add(this.dimensionTop(), JSplitPane.RIGHT);

    this.setLayout(new BorderLayout());
    this.add(this.jSplitPane, BorderLayout.CENTER);
  }

  private JPanel dimensionTop() {
    JPanel jPanel = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(jPanel, PGHelper.getPGConfig(), false);

    gbl.row()
        .cell(new JXTitledSeparator("Dimension")).fillX();

    gbl.row()
        .cellXYRemainder(loadDimensionTop()).fillXY();

    gbl.done();

    return jPanel;
  }

  protected JScrollPane loadDimensionTop() {
    final JPanel[] panel = {new JPanel()};

    List<JScrollPane> jScrollPaneList = new ArrayList<>();

    try {
      Optional.ofNullable(tableInfo.getDimensionColumnList())
          .orElse(Collections.emptyList())
          .forEach(colName -> {
            try {
              CProfile firstGrpBy = tableInfo.getCProfiles().stream()
                  .filter(f -> f.getColName().equalsIgnoreCase(colName))
                  .findFirst()
                  .orElseThrow();

              List<GanttColumnCount> ganttColumnList = getGanttColumnList(firstGrpBy, cProfile);

              DrawingScale drawingScale = new DrawingScale();

              JXTable jxTable = loadGantt(firstGrpBy, ganttColumnList, seriesColorMap, drawingScale, 5, 20);
              jScrollPaneList.add(getJScrollPane(jxTable));

            } catch (Exception exception) {
              log.catching(exception);
            }
          });

      if (jScrollPaneList.size() != 0) {
        int columns = 3;
        int rows = (int) Math.ceil((double) jScrollPaneList.size() / columns);

        panel[0] = new JPanel(new GridLayout(rows, columns));

        for (JScrollPane pane : jScrollPaneList) {
          panel[0].add(pane);
        }
      }

      panel[0].repaint();

    } catch (Exception exception) {
      log.catching(exception);
    }

    JScrollPane scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
    scrollPane.setViewportView(panel[0]);
    scrollPane.setVerticalScrollBar(scrollPane.getVerticalScrollBar());

    return scrollPane;
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

    // prevents double events
    if (!e.getValueIsAdjusting()) {

      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing query fields");
      } else {
        int columnId = GUIHelper.getIdByColumnName(jxTableCase.getJxTable(),
                                                   super.jxTableCase.getDefaultTableModel(), listSelectionModel, TaskColumnNames.ID.getColName());

        executorService.submit(() -> {
          GUIHelper.addToJSplitPane(jSplitPane, ProgressBarHelper.createProgressBar("Loading, please wait..."),
                                    JSplitPane.RIGHT, DIVIDER_LOCATION);

          try {
            CProfile firstGrpBy = tableInfo.getCProfiles().stream()
                .filter(f -> f.getColId() == columnId)
                .findFirst()
                .orElseThrow();

            List<GanttColumnCount> ganttColumnList = getGanttColumnList(firstGrpBy, cProfile);

            DrawingScale drawingScale = new DrawingScale();

            JXTable jxTable = loadGantt(firstGrpBy, ganttColumnList, seriesColorMap, drawingScale, 100, 23);

            GUIHelper.addToJSplitPane(jSplitPane, getJScrollPane(jxTable), JSplitPane.RIGHT, 200);

          } catch (Exception exception) {
            log.catching(exception);
          }
        });

        log.info(columnId);
      }
    }
  }

  private List<GanttColumnCount> getGanttColumnList(CProfile firstGrpBy, CProfile cProfileFilter)
      throws BeginEndWrongOrderException, GanttColumnNotSupportedException, SqlColMetadataException {
    return getGanttColumnList(seriesType,
                              dStore,
                              tableInfo.getTableName(),
                              firstGrpBy,
                              cProfile,
                              cProfileFilter,
                              seriesColorMap.keySet().toArray(String[]::new),
                              CompareFunction.EQUAL,
                              begin,
                              end);
  }
}