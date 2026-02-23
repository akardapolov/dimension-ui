package ru.dimension.ui.component.module.preview;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableRowSorter;
import lombok.Getter;
import lombok.Setter;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swingx.JXTableTables;
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
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;

@Getter
public class PreviewView extends JPanel {
  private final PreviewMode mode;

  private final PreviewModel model;
  private final JSplitPane splitPane;
  private final TTTable<ColumnRow, JXTable> columnTable;

  private JTextField columnSearch;
  private JCheckBox pickAllCheckbox;
  private TableRowSorter<?> columnSorter;
  private int nameColumnIndex = -1;
  private int pickColumnIndex = -1;

  private boolean ignoreCheckboxEvents = false;

  private final AtomicInteger pendingOperations = new AtomicInteger(0);

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
  private final ExecutorService pickAllExecutor = Executors.newSingleThreadExecutor();

  public PreviewView(PreviewMode mode,
                     PreviewModel model) {
    this.mode = mode;
    this.model = model;

    this.setBorder(new EtchedBorder());

    TTRegistry registry = TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();

    this.columnTable = createCheckboxTable(registry);
    setupColumnTableListener();

    JPanel columnSearchPanel = new JPanel(new BorderLayout());

    JPanel searchRowPanel = new JPanel();
    searchRowPanel.setLayout(new BoxLayout(searchRowPanel, BoxLayout.X_AXIS));

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

    pickAllCheckbox = new JCheckBox();
    pickAllCheckbox.setToolTipText("Pick it all");
    pickAllCheckbox.setPreferredSize(new Dimension(30, pickAllCheckbox.getPreferredSize().height));
    pickAllCheckbox.setMinimumSize(new Dimension(30, pickAllCheckbox.getMinimumSize().height));
    pickAllCheckbox.setMaximumSize(new Dimension(30, Short.MAX_VALUE));
    pickAllCheckbox.setHorizontalAlignment(JCheckBox.CENTER);
    pickAllCheckbox.addActionListener(e -> handlePickAllAction(pickAllCheckbox.isSelected()));

    searchRowPanel.add(columnSearch);
    searchRowPanel.add(Box.createHorizontalStrut(1));
    searchRowPanel.add(pickAllCheckbox);

    columnSearchPanel.add(searchRowPanel, BorderLayout.NORTH);
    columnSearchPanel.add(columnTable.scrollPane(), BorderLayout.CENTER);
    columnSearchPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Columns"));

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
    } else if (PreviewMode.ADHOC.equals(mode)) {
      splitPane.setDividerLocation(200);
    }

    gbl.row().cell(splitPane).fillXY();
    gbl.done();
  }

  private void handlePickAllAction(boolean selected) {
    if (pickColumnIndex < 0) return;

    int count = 0;
    for (int i = 0; i < columnTable.model().getRowCount(); i++) {
      ColumnRow row = columnTable.model().itemAt(i);
      if (row != null && row.isPick() != selected) {
        count++;
      }
    }

    if (count == 0) return;

    String action = selected ? "select" : "deselect";
    int result = JOptionPane.showConfirmDialog(
        SwingUtilities.getWindowAncestor(this),
        "Are you sure you want to " + action + " all " + count + " columns?",
        "Confirm " + action + " all",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (result != JOptionPane.YES_OPTION) {
      pickAllCheckbox.setSelected(!selected);
      return;
    }

    setPickAllUIEnabled(false);

    pickAllExecutor.submit(() -> {
      try {
        for (int i = 0; i < columnTable.model().getRowCount(); i++) {
          final int rowIndex = i;
          ColumnRow row = columnTable.model().itemAt(rowIndex);
          if (row != null && row.isPick() != selected) {
            SwingUtilities.invokeAndWait(() ->
                                             columnTable.model().setValueAt(selected, rowIndex, pickColumnIndex)
            );
          }
        }
      } catch (Exception ex) {
        SwingUtilities.invokeLater(() ->
                                       JOptionPane.showMessageDialog(
                                           SwingUtilities.getWindowAncestor(this),
                                           "Error during pick all operation: " + ex.getMessage(),
                                           "Error",
                                           JOptionPane.ERROR_MESSAGE
                                       )
        );
      } finally {
        SwingUtilities.invokeLater(() -> setPickAllUIEnabled(true));
      }
    });
  }

  private void setPickAllUIEnabled(boolean enabled) {
    pickAllCheckbox.setEnabled(enabled);
    columnSearch.setEnabled(enabled);
    columnTable.table().setEnabled(enabled);
  }

  public void setColumnSelected(String columnName, boolean selected) {
    ignoreCheckboxEvents = true;
    try {
      for (int i = 0; i < columnTable.model().getRowCount(); i++) {
        ColumnRow row = columnTable.model().itemAt(i);
        if (row != null && row.getName().equals(columnName)) {
          columnTable.model().setValueAt(selected, i, pickColumnIndex);
          break;
        }
      }
    } finally {
      ignoreCheckboxEvents = false;
    }
  }

  private void setupColumnTableListener() {
    pickColumnIndex = columnTable.model().schema().modelIndexOf("pick");
    if (pickColumnIndex < 0) {
      return;
    }

    columnTable.model().addTableModelListener(e -> {
      if (ignoreCheckboxEvents || e.getType() != TableModelEvent.UPDATE)
        return;

      if (e.getColumn() == pickColumnIndex) {
        int row = e.getFirstRow();
        if (row >= 0 && row < columnTable.model().getRowCount()) {
          ColumnRow item = columnTable.model().itemAt(row);
          if (item != null && checkboxChangeListener != null) {
            checkboxChangeListener.onCheckboxChanged(item.getName(), item.isPick());
          }
        }
      }
    });
  }

  public void addChartCard(IPreviewChart taskPane, BiConsumer<IPreviewChart, Exception> onComplete) {
    pendingOperations.incrementAndGet();
    addTaskPane(taskPane);

    SwingTaskRunner.runWithProgress(
        taskPane.asTaskPane(),
        executor,
        taskPane::initializeUI,
        e -> {
          removeTaskPane(taskPane);
          pendingOperations.decrementAndGet();
          onComplete.accept(null, e);
        },
        () -> createProgressBar("Loading preview, please wait..."),
        () -> {
          setColumnSelected(taskPane.getTitle(), true);
          pendingOperations.decrementAndGet();
          onComplete.accept(taskPane, null);
        }
    );
  }

  public void clearAllCharts() {
    ignoreCheckboxEvents = true;
    try {
      for (int i = 0; i < columnTable.model().getRowCount(); i++) {
        columnTable.model().setValueAt(false, i, pickColumnIndex);
      }

      cardContainer.removeAll();
      cardContainer.revalidate();
      cardContainer.repaint();
    } finally {
      ignoreCheckboxEvents = false;
    }
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
    columnTable.setItems(Collections.emptyList());
    populateColumnTable(tableInfo);
    if (pickAllCheckbox != null) {
      pickAllCheckbox.setSelected(false);
    }
  }

  private TTTable<ColumnRow, JXTable> createCheckboxTable(TTRegistry registry) {
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
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));
    table.setEditable(true);

    if (table.getColumnExt("ID") != null) {
      table.getColumnExt("ID").setVisible(false);
    }

    if (table.getColumnExt("Name") != null) {
      table.getColumnExt("Name").setEditable(false);
    }

    columnSorter = new TableRowSorter<>(tt.model());
    table.setRowSorter(columnSorter);

    nameColumnIndex = tt.model().schema().modelIndexOf("name");
    pickColumnIndex = tt.model().schema().modelIndexOf("pick");

    return tt;
  }

  private void updateColumnFilter() {
    String searchText = columnSearch.getText();
    if (columnTable == null) return;

    if (columnSorter == null) {
      columnSorter = new TableRowSorter<>(columnTable.model());
      columnTable.table().setRowSorter(columnSorter);
    }

    if (searchText == null || searchText.isEmpty()) {
      columnSorter.setRowFilter(null);
    } else {
      try {
        if (nameColumnIndex >= 0) {
          columnSorter.setRowFilter(RowFilter.regexFilter("(?iu)" + searchText, nameColumnIndex));
        }
      } catch (PatternSyntaxException e) {
        columnSorter.setRowFilter(RowFilter.regexFilter("$^", 0));
      }
    }
  }

  private void populateColumnTable(TableInfo tableInfo) {
    if (tableInfo != null && tableInfo.getCProfiles() != null) {
      List<ColumnRow> rows = tableInfo.getCProfiles().stream()
          .filter(cProfile -> !cProfile.getCsType().isTimeStamp())
          .map(cProfile -> new ColumnRow(cProfile, false))
          .collect(Collectors.toList());
      columnTable.setItems(rows);
    }
  }

  private JLabel getLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }
}