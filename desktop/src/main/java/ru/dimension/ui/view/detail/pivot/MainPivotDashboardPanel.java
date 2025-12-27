package ru.dimension.ui.view.detail.pivot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.db.core.DStore;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.model.column.TaskColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.TableInfo;

@Log4j2
public class MainPivotDashboardPanel extends GanttPivotPanel implements ListSelectionListener {

  private enum PivotAgg {
    COUNT,
    SUM,
    SUM_TOTAL
  }

  private final DStore dStore;
  private final java.util.concurrent.ScheduledExecutorService executorService;
  private final Metric metric;

  private final JPanel rightPanel;
  private final JPanel rightCenterPanel;

  private final JRadioButton rbCount;
  private final JRadioButton rbSum;
  private final JRadioButton rbSumTotal;

  private volatile CProfile lastSelectedFirstLevelGroupBy;

  public MainPivotDashboardPanel(DStore dStore,
                                 java.util.concurrent.ScheduledExecutorService executorService,
                                 TableInfo tableInfo,
                                 Metric metric,
                                 CProfile cProfile,
                                 long begin,
                                 long end,
                                 Map<String, Color> seriesColorMap) {
    super(tableInfo, cProfile, begin, end, seriesColorMap);

    this.dStore = dStore;
    this.executorService = executorService;
    this.metric = metric;

    this.jxTableCase.getJxTable().getSelectionModel().addListSelectionListener(this);

    this.rightPanel = new JPanel(new BorderLayout());
    this.rightCenterPanel = new JPanel(new BorderLayout());

    JPanel aggPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    aggPanel.add(new JLabel("Function:"));

    this.rbCount = new JRadioButton("Count by Row", true);
    this.rbSum = new JRadioButton("Sum by Row", false);
    this.rbSumTotal = new JRadioButton("Sum Total", false);

    this.rbCount.setToolTipText("Count the number of records in each category");
    this.rbSum.setToolTipText("Sum values with breakdown by each unique value");
    this.rbSumTotal.setToolTipText("Aggregated sum per category (single row)");

    this.rbSum.setEnabled(false);
    this.rbSumTotal.setEnabled(false);

    ButtonGroup bg = new ButtonGroup();
    bg.add(rbCount);
    bg.add(rbSum);
    bg.add(rbSumTotal);

    aggPanel.add(rbCount);
    aggPanel.add(rbSum);
    aggPanel.add(rbSumTotal);

    rbCount.addActionListener(e -> reloadPivotForLastSelection());
    rbSum.addActionListener(e -> reloadPivotForLastSelection());
    rbSumTotal.addActionListener(e -> reloadPivotForLastSelection());

    rightPanel.add(aggPanel, BorderLayout.NORTH);
    rightPanel.add(rightCenterPanel, BorderLayout.CENTER);

    this.jSplitPane.setLeftComponent(this.jxTableCase.getJScrollPane());
    this.jSplitPane.setRightComponent(rightPanel);

    this.setLayout(new BorderLayout());
    this.add(this.jSplitPane, BorderLayout.CENTER);

    setRightCenter(new JPanel());
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

    if (e.getValueIsAdjusting()) {
      return;
    }

    if (listSelectionModel.isSelectionEmpty()) {
      lastSelectedFirstLevelGroupBy = null;
      setRightCenter(new JPanel());
      return;
    }

    int columnId = GUIHelper.getIdByColumnName(
        jxTableCase.getJxTable(),
        this.jxTableCase.getDefaultTableModel(),
        listSelectionModel,
        TaskColumnNames.ID.getColName()
    );

    executorService.submit(() -> {
      try {
        SwingUtilities.invokeLater(() -> setRightCenter(createProgressBar("Loading, please wait...")));

        CProfile firstLevelGroupBy = tableInfo.getCProfiles().stream()
            .filter(f -> f.getColId() == columnId)
            .findFirst()
            .orElseThrow();

        SwingUtilities.invokeLater(() -> updateAggButtons(firstLevelGroupBy));

        lastSelectedFirstLevelGroupBy = firstLevelGroupBy;
        buildAndShowPivot(firstLevelGroupBy);

      } catch (Exception ex) {
        log.catching(ex);
        SwingUtilities.invokeLater(() -> setRightCenter(new JLabel("Error loading pivot. See logs.")));
      }
    });
  }

  private void reloadPivotForLastSelection() {
    CProfile selected = lastSelectedFirstLevelGroupBy;
    if (selected == null) {
      return;
    }
    executorService.submit(() -> {
      try {
        SwingUtilities.invokeLater(() -> setRightCenter(createProgressBar("Loading, please wait...")));
        buildAndShowPivot(selected);
      } catch (Exception ex) {
        log.catching(ex);
        SwingUtilities.invokeLater(() -> setRightCenter(new JLabel("Error loading pivot. See logs.")));
      }
    });
  }

  private void buildAndShowPivot(CProfile firstLevelGroupBy) throws Exception {
    PivotAgg agg = resolveSelectedAgg();

    // We still use 2D counts as base data (SUM derived from count * numeric-rowKey)
    List<GanttColumnCount> ganttColumnList1 =
        dStore.getGanttCount(tableInfo.getTableName(), firstLevelGroupBy, cProfile, null, begin, end);

    List<GanttColumnCount> ganttColumnList2 =
        dStore.getGanttCount(tableInfo.getTableName(), cProfile, cProfile, null, begin, end);

    if (agg == PivotAgg.SUM_TOTAL && isNumeric(firstLevelGroupBy)) {
      combinedTable = loadPivotGanttSumTotal(
          firstLevelGroupBy, cProfile,
          ganttColumnList1, ganttColumnList2,
          seriesColorMap, 100, 23, false
      );
    } else if (agg == PivotAgg.SUM && isNumeric(firstLevelGroupBy)) {
      combinedTable = loadPivotGanttSum(
          firstLevelGroupBy, cProfile,
          ganttColumnList1, ganttColumnList2,
          seriesColorMap, 100, 23, false
      );
    } else {
      combinedTable = loadPivotGantt(
          firstLevelGroupBy, cProfile,
          ganttColumnList1, ganttColumnList2,
          seriesColorMap, 100, 23, false
      );
    }

    applyCenterBoldRenderer(combinedTable);
    SwingUtilities.invokeLater(() -> setRightCenter(getJScrollPane(combinedTable)));
  }

  private void updateAggButtons(CProfile firstLevelGroupBy) {
    boolean numeric = isNumeric(firstLevelGroupBy);

    rbCount.setEnabled(true);
    rbSum.setEnabled(numeric);
    rbSumTotal.setEnabled(numeric);

    if (!numeric && (rbSum.isSelected() || rbSumTotal.isSelected())) {
      rbCount.setSelected(true);
    }
  }

  private PivotAgg resolveSelectedAgg() {
    if (rbSumTotal.isSelected()) {
      return PivotAgg.SUM_TOTAL;
    }
    if (rbSum.isSelected()) {
      return PivotAgg.SUM;
    }
    return PivotAgg.COUNT;
  }

  private boolean isNumeric(CProfile profile) {
    if (profile == null || profile.getCsType() == null) {
      return false;
    }
    CType cType = profile.getCsType().getCType();
    if (cType == null) {
      return false;
    }
    return cType != CType.STRING;
  }

  private void applyCenterBoldRenderer(JXTable table) {
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

    table.setDefaultRenderer(Object.class, centerRenderer);
  }

  private void setRightCenter(Component component) {
    rightCenterPanel.removeAll();
    rightCenterPanel.add(component, BorderLayout.CENTER);
    rightCenterPanel.revalidate();
    rightCenterPanel.repaint();
  }

  private JPanel createProgressBar(String message) {
    JPanel panel = new JPanel();
    panel.add(new JLabel(message));
    return panel;
  }
}