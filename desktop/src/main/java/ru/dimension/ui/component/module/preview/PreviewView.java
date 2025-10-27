package ru.dimension.ui.component.module.preview;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.BorderLayout;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.regex.PatternSyntaxException;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import lombok.Getter;
import lombok.Setter;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.block.PreviewConfigBlock;
import ru.dimension.ui.component.module.preview.spi.IPreviewChart;
import ru.dimension.ui.component.module.preview.spi.PreviewMode;
import ru.dimension.ui.component.panel.CollapseCardPanel;
import ru.dimension.ui.component.panel.ConfigShowHidePanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.component.panel.range.RealTimeRangePanel;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.table.JXTableCase;

@Getter
public class PreviewView extends JPanel {
  private final PreviewMode mode;

  private final PreviewModel model;
  private final JSplitPane splitPane;
  private final JXTableCase columnTableCase;

  private JTextField columnSearch;
  private TableRowSorter<?> columnSorter;

  private boolean ignoreCheckboxEvents = false;

  public interface CheckboxChangeListener {
    void onCheckboxChanged(String columnName, boolean selected);
  }

  @Setter
  private CheckboxChangeListener checkboxChangeListener;

  private final PreviewConfigBlock previewConfigBlock;
  private final RealTimeRangePanel realTimeRangePanel;
  private final HistoryRangePanel historyRangePanel;
  private final LegendPanel realTimeLegendPanel;
  private final ConfigShowHidePanel configShowHidePanel;
  private final CollapseCardPanel collapseCardPanel;

  private final JXTaskPaneContainer cardContainer;
  private final JScrollPane cardScrollPane;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public PreviewView(PreviewMode mode,
                     PreviewModel model) {
    this.mode = mode;
    this.model = model;

    this.setBorder(new EtchedBorder());

    this.columnTableCase = createCheckboxTableCase();
    setupColumnTableListener();

    JPanel columnSearchPanel = new JPanel(new BorderLayout());
    columnSearch = new JTextField();
    columnSearch.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) { updateColumnFilter(); }
      @Override
      public void removeUpdate(DocumentEvent e) { updateColumnFilter(); }
      @Override
      public void changedUpdate(DocumentEvent e) { updateColumnFilter(); }
    });
    columnSearch.setToolTipText("Search columns...");
    columnSearchPanel.add(columnSearch, BorderLayout.NORTH);
    columnSearchPanel.add(columnTableCase.getJScrollPane(), BorderLayout.CENTER);
    columnSearchPanel.setBorder(BorderFactory.createTitledBorder("Columns"));

    this.realTimeRangePanel = new RealTimeRangePanel(getLabel("Range: "));
    this.historyRangePanel = new HistoryRangePanel(getLabel("Range: "));
    this.realTimeLegendPanel = new LegendPanel(getLabel("Legend: "));
    this.configShowHidePanel = new ConfigShowHidePanel(getLabel("Config: "));
    this.collapseCardPanel = new CollapseCardPanel(getLabel("Dashboard"));

    JPanel rangePanel;
    if (PreviewMode.PREVIEW.equals(mode)) {
      rangePanel = realTimeRangePanel;
    } else {
      rangePanel = historyRangePanel;
    }

    this.previewConfigBlock = new PreviewConfigBlock(rangePanel,
                                                     realTimeLegendPanel,
                                                     configShowHidePanel,
                                                     collapseCardPanel);

    this.cardContainer = new JXTaskPaneContainer();
    LaF.setBackgroundColor(CHART_PANEL, cardContainer);
    cardContainer.setBackgroundPainter(null);

    this.cardScrollPane = new JScrollPane();
    GUIHelper.setScrolling(cardScrollPane);
    cardScrollPane.setViewportView(cardContainer);

    LaF.setBackgroundColor(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    JPanel rightPanel = new JPanel(new BorderLayout());
    rightPanel.add(previewConfigBlock, BorderLayout.NORTH);
    rightPanel.add(cardScrollPane, BorderLayout.CENTER);

    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                               columnSearchPanel,
                               rightPanel);

    if (PreviewMode.PREVIEW.equals(mode)) {
      splitPane.setDividerLocation(300);
    } else if (PreviewMode.DETAIL.equals(mode)) {
      splitPane.setDividerLocation(200);
    }

    gbl.row().cell(splitPane).fillXY();
    gbl.done();
  }

  public void setColumnSelected(String columnName, boolean selected) {
    ignoreCheckboxEvents = true;
    for (int i = 0; i < columnTableCase.getDefaultTableModel().getRowCount(); i++) {
      String name = (String) columnTableCase.getDefaultTableModel().getValueAt(i, ColumnNames.NAME.ordinal());
      if (name.equals(columnName)) {
        columnTableCase.getDefaultTableModel().setValueAt(selected, i, ColumnNames.PICK.ordinal());
        break;
      }
    }
    ignoreCheckboxEvents = false;
  }

  private void setupColumnTableListener() {
    columnTableCase.getDefaultTableModel().addTableModelListener(e -> {
      if (ignoreCheckboxEvents || e.getType() != TableModelEvent.UPDATE)
        return;

      if (e.getColumn() == ColumnNames.PICK.ordinal()) {
        int row = e.getFirstRow();
        Boolean selected = (Boolean) columnTableCase.getDefaultTableModel()
            .getValueAt(row, ColumnNames.PICK.ordinal());
        String columnName = (String) columnTableCase.getDefaultTableModel()
            .getValueAt(row, ColumnNames.NAME.ordinal());

        if (checkboxChangeListener != null) {
          checkboxChangeListener.onCheckboxChanged(columnName, selected);
        }
      }
    });
  }

  public void addChartCard(IPreviewChart taskPane, BiConsumer<IPreviewChart, Exception> onComplete) {
    addTaskPane(taskPane);

    SwingTaskRunner.runWithProgress(
        taskPane.asTaskPane(),
        executor,
        taskPane::initializeUI,
        e -> {
          removeTaskPane(taskPane);
          onComplete.accept(null, e);
        },
        () -> createProgressBar("Loading preview, please wait..."),
        () -> {
          setColumnSelected(taskPane.getTitle(), true);
          onComplete.accept(taskPane, null);
        }
    );
  }

  public void clearAllCharts() {
    ignoreCheckboxEvents = true;
    for (int i = 0; i < columnTableCase.getDefaultTableModel().getRowCount(); i++) {
      columnTableCase.getDefaultTableModel().setValueAt(false, i, ColumnNames.PICK.ordinal());
    }

    cardContainer.removeAll();
    cardContainer.revalidate();
    cardContainer.repaint();
    ignoreCheckboxEvents = false;
  }

  private void addTaskPane(IPreviewChart taskPane) {
    cardContainer.add(taskPane.asTaskPane());
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  public void removeTaskPane(IPreviewChart taskPane) {
    setColumnSelected(taskPane.getTitle(), false);
    cardContainer.remove(taskPane.asTaskPane());
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  public void removeChartCard(IPreviewChart taskPane) {
    removeTaskPane(taskPane);
  }

  public void updateColumnTables(TableInfo tableInfo) {
    columnTableCase.clearTable();
    populateColumnTable(tableInfo);
  }

  private JXTableCase createCheckboxTableCase() {
    JXTableCase jxTableCase = GUIHelper.getJXTableCaseCheckBoxAdHoc(10,
                                                                    new String[]{
                                                                        ColumnNames.ID.getColName(),
                                                                        ColumnNames.NAME.getColName(),
                                                                        ColumnNames.PICK.getColName()
                                                                    }, ColumnNames.PICK.ordinal());

    jxTableCase.getJxTable().getColumnExt(ColumnNames.ID.ordinal()).setVisible(false);

    TableColumn nameColumn = jxTableCase.getJxTable().getColumnModel().getColumn(ColumnNames.NAME.ordinal());
    nameColumn.setMinWidth(30);
    nameColumn.setMaxWidth(40);

    columnSorter = new TableRowSorter<>(jxTableCase.getJxTable().getModel());
    jxTableCase.getJxTable().setRowSorter(columnSorter);

    return jxTableCase;
  }

  private void updateColumnFilter() {
    String searchText = columnSearch.getText();
    if (columnTableCase == null) return;
    if (columnSorter == null) {
      columnSorter = new TableRowSorter<>(columnTableCase.getJxTable().getModel());
      columnTableCase.getJxTable().setRowSorter(columnSorter);
    }
    if (searchText == null || searchText.isEmpty()) {
      columnSorter.setRowFilter(null);
    } else {
      try {
        columnSorter.setRowFilter(RowFilter.regexFilter("(?iu)" + searchText, ColumnNames.NAME.ordinal()));
      } catch (PatternSyntaxException e) {
        columnSorter.setRowFilter(RowFilter.regexFilter("$^", 0));
      }
    }
  }

  private void populateColumnTable(TableInfo tableInfo) {
    if (tableInfo.getCProfiles() != null) {
      tableInfo.getCProfiles().stream()
          .filter(cProfile -> !cProfile.getCsType().isTimeStamp())
          .forEach(cProfile -> columnTableCase.getDefaultTableModel()
              .addRow(new Object[]{cProfile.getColId(), cProfile.getColName(), false}));
    }
  }

  private JLabel getLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }
}