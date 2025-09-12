package ru.dimension.ui.component.module.preview;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.regex.PatternSyntaxException;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
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
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.block.PreviewConfigBlock;
import ru.dimension.ui.component.module.PreviewChartModule;
import ru.dimension.ui.component.panel.CollapseCardPanel;
import ru.dimension.ui.component.panel.ConfigShowHidePanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.range.RealTimeRangePanel;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;

@Getter
public class PreviewView extends JDialog {
  private final PreviewModel model;
  private final JPanel mainPanel;
  private final JSplitPane splitPane;
  private final JXTableCase columnTableCase;

  private JTextField columnSearch;
  private TableRowSorter<?> columnSorter;

  private boolean ignoreCheckboxEvents = false;

  public interface CheckboxChangeListener {
    void onCheckboxChanged(String columnName, boolean selected);
  }

  private CheckboxChangeListener checkboxChangeListener;

  private final PreviewConfigBlock previewConfigBlock;
  private final RealTimeRangePanel realTimeRangePanel;
  private final LegendPanel realTimeLegendPanel;
  private final ConfigShowHidePanel configShowHidePanel;
  private final CollapseCardPanel collapseCardPanel;

  private final JXTaskPaneContainer cardContainer;
  private final JScrollPane cardScrollPane;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public PreviewView(PreviewModel model) {
    this.model = model;

    this.mainPanel = new JPanel();
    this.mainPanel.setBorder(new EtchedBorder());

    this.columnTableCase = createCheckboxTableCase();
    setupColumnTableListener();

    // Create search panel for columns
    JPanel columnSearchPanel = new JPanel(new BorderLayout());
    columnSearch = new JTextField();
    columnSearch.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        updateColumnFilter();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        updateColumnFilter();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        updateColumnFilter();
      }
    });
    columnSearch.setToolTipText("Search columns...");
    columnSearchPanel.add(columnSearch, BorderLayout.NORTH);
    columnSearchPanel.add(columnTableCase.getJScrollPane(), BorderLayout.CENTER);
    columnSearchPanel.setBorder(BorderFactory.createTitledBorder("Columns"));

    this.realTimeRangePanel = new RealTimeRangePanel(getLabel("Range: "));
    this.realTimeLegendPanel = new LegendPanel(getLabel("Legend: "));
    this.configShowHidePanel = new ConfigShowHidePanel(getLabel("Config: "));
    this.collapseCardPanel = new CollapseCardPanel(getLabel("Dashboard"));
    this.previewConfigBlock = new PreviewConfigBlock(realTimeRangePanel,
                                                     realTimeLegendPanel,
                                                     configShowHidePanel,
                                                     collapseCardPanel);

    // Initialize chart container
    this.cardContainer = new JXTaskPaneContainer();
    LaF.setBackgroundColor(CHART_PANEL, cardContainer);
    cardContainer.setBackgroundPainter(null);

    this.cardScrollPane = new JScrollPane();
    GUIHelper.setScrolling(cardScrollPane);
    cardScrollPane.setViewportView(cardContainer);

    LaF.setBackgroundColor(CHART_PANEL, mainPanel);

    PainlessGridBag gbl = new PainlessGridBag(mainPanel, PGHelper.getPGConfig(1), false);

    JPanel rightPanel = new JPanel(new BorderLayout());
    rightPanel.add(previewConfigBlock, BorderLayout.NORTH);
    rightPanel.add(cardScrollPane, BorderLayout.CENTER);

    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                          columnSearchPanel,
                                          rightPanel);
    splitPane.setDividerLocation(300);

    gbl.row().cell(splitPane).fillXY();
    gbl.done();

    this.add(mainPanel);

    ProfileInfo profileInfo = model.getProfileManager().getProfileInfoById(model.getKey().getProfileId());
    TaskInfo taskInfo = model.getProfileManager().getTaskInfoById(model.getKey().getTaskId());
    QueryInfo queryInfo = model.getProfileManager().getQueryInfoById(model.getKey().getQueryId());

    this.setTitle("Preview realtime -> Profile: " + profileInfo.getName() + " >>> Task: " + taskInfo.getName() + " >>> Query: " + queryInfo.getName());
    this.packConfig(false);
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

  public void setCheckboxChangeListener(CheckboxChangeListener listener) {
    this.checkboxChangeListener = listener;
  }

  public void addChartCard(PreviewChartModule taskPane,
                           BiConsumer<PreviewChartModule, Exception> onComplete) {
    addTaskPane(taskPane);

    SwingTaskRunner.runWithProgress(
        taskPane,
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

  private void addTaskPane(PreviewChartModule taskPane) {
    cardContainer.add(taskPane);
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  public void removeTaskPane(PreviewChartModule taskPane) {
    setColumnSelected(taskPane.getTitle(), false);
    cardContainer.remove(taskPane);
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  public void removeChartCard(PreviewChartModule taskPane) {
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

  public void packConfig(boolean visible) {
    this.splitPane.setDividerLocation(300);

    this.setVisible(visible);
    this.setModal(true);
    this.setResizable(true);
    this.pack();

    this.setSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width - 400,
                               Toolkit.getDefaultToolkit().getScreenSize().height - 100));
    this.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - getWidth() / 2,
                     (Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - getHeight() / 2);
  }
}