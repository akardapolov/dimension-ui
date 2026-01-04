package ru.dimension.ui.view.detail.pivot;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.GanttColumnNotSupportedException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.view.SeriesType;
import ru.dimension.ui.view.detail.HelperGantt;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;

@Log4j2
public class ReportPivotPanel extends GanttPivotPanel implements ListSelectionListener, HelperGantt {

  private final DStore dStore;
  private final SeriesType seriesType;

  private final ScheduledExecutorService executorService;

  public ReportPivotPanel(DStore dStore,
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

    this.columnTable.table().getSelectionModel().addListSelectionListener(this);

    this.jSplitPane.add(this.columnTable.scrollPane(), JSplitPane.LEFT);
    this.jSplitPane.add(new JPanel(), JSplitPane.RIGHT);

    this.setLayout(new BorderLayout());
    this.add(this.jSplitPane, BorderLayout.CENTER);
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) {
      return;
    }

    var table = this.columnTable.table();

    if (table.getSelectionModel().isSelectionEmpty()) {
      log.info("Clearing query fields");
    } else {
      int viewRow = table.getSelectedRow();
      if (viewRow < 0) return;

      int modelRow = table.convertRowIndexToModel(viewRow);
      ColumnRow selectedItem = this.columnTable.model().itemAt(modelRow);

      if (selectedItem != null) {
        String selectedColumnName = selectedItem.getName();
        log.info("Selected column: {}", selectedColumnName);

        executorService.submit(() -> {
          GUIHelper.addToJSplitPane(jSplitPane, createProgressBar("Loading, please wait..."), JSplitPane.RIGHT, DIVIDER_LOCATION);

          try {
            CProfile firstGrpBy = tableInfo.getCProfiles().stream()
                .filter(f -> f.getColName().equalsIgnoreCase(selectedColumnName))
                .findFirst()
                .orElseThrow();

            List<GanttColumnCount> ganttColumnList1 = getGanttColumnList(firstGrpBy, cProfile);
            List<GanttColumnCount> ganttColumnList2 = getGanttColumnList(cProfile, cProfile);

            combinedTable = loadPivotGantt(firstGrpBy, cProfile, ganttColumnList1, ganttColumnList2,
                                           seriesColorMap, 100, 23, false);

            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
              @Override
              public Component getTableCellRendererComponent(JTable table,
                                                             Object value,
                                                             boolean isSelected,
                                                             boolean hasFocus,
                                                             int row,
                                                             int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(new Font("Arial", Font.BOLD, 12));
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
              }
            };

            combinedTable.setDefaultRenderer(Object.class, centerRenderer);

            GUIHelper.addToJSplitPane(jSplitPane, getJScrollPane(combinedTable), JSplitPane.RIGHT, 200);

          } catch (Exception exception) {
            log.catching(exception);
          }
        });
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