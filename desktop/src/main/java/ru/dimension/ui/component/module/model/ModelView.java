package ru.dimension.ui.component.module.model;

import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.PatternSyntaxException;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableRowSorter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTitledSeparator;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swing.icon.RowIconProvider;
import ru.dimension.tt.swingx.JXTableTables;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;
import ru.dimension.ui.view.table.row.Rows.MetricRow;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
public class ModelView extends JPanel {

  private final TTTable<ProfileRow, JXTable> profileTable;
  private final TTTable<TaskRow, JXTable> taskTable;
  private final TTTable<QueryRow, JXTable> queryTable;
  private final TTTable<ColumnRow, JXTable> columnTable;
  private final TTTable<MetricRow, JXTable> metricTable;

  private JTextField columnSearch;
  private JTextField metricSearch;

  private JCheckBox columnPickAllCheckbox;
  private JCheckBox metricPickAllCheckbox;

  private TableRowSorter<?> columnSorter;
  private TableRowSorter<?> metricSorter;

  private int columnNameColumnIndex = -1;
  private int metricNameColumnIndex = -1;

  private int columnPickColumnIndex = -1;
  private int metricPickColumnIndex = -1;

  private boolean ignoreToggleEvents = false;

  private final ExecutorService pickAllExecutor = Executors.newSingleThreadExecutor();

  public ModelView() {
    TTRegistry registry = TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();

    this.profileTable = createBasicTableWithIcon(
        registry, ProfileRow.class, ModelIconProviders.forProfileRow());
    this.taskTable = createBasicTableWithIcon(
        registry, TaskRow.class, ModelIconProviders.forTaskRow());
    this.queryTable = createBasicTableWithIcon(
        registry, QueryRow.class, ModelIconProviders.forQueryRow());
    this.columnTable = createCheckBoxTableWithIcon(
        registry, ColumnRow.class, ModelIconProviders.forColumnRow());
    this.metricTable = createCheckBoxTableWithIcon(
        registry, MetricRow.class, ModelIconProviders.forMetricRow());

    columnPickColumnIndex = columnTable.model().schema().modelIndexOf("pick");
    metricPickColumnIndex = metricTable.model().schema().modelIndexOf("pick");

    initColumnFiltering();
    initMetricFiltering();

    setupLayout();
  }

  public TTTable<ProfileRow, JXTable> getProfileTable() { return profileTable; }
  public TTTable<TaskRow, JXTable> getTaskTable() { return taskTable; }
  public TTTable<QueryRow, JXTable> getQueryTable() { return queryTable; }
  public TTTable<ColumnRow, JXTable> getColumnTable() { return columnTable; }
  public TTTable<MetricRow, JXTable> getMetricTable() { return metricTable; }

  public void setColumnToggleListener(ModelHandler<ColumnRow> handler) {
    setupTableListener(columnTable, "pick", handler, this::updateColumnPickAllState);
  }

  public void setMetricToggleListener(ModelHandler<MetricRow> handler) {
    setupTableListener(metricTable, "pick", handler, this::updateMetricPickAllState);
  }

  @SuppressWarnings("unchecked")
  private <T> void setupTableListener(TTTable<T, ?> tt, String colName, ModelHandler<T> handler, Runnable pickAllUpdater) {
    int colIdx = tt.model().schema().modelIndexOf(colName);
    if (colIdx < 0) {
      log.warn("Column '{}' not found in table schema", colName);
      return;
    }

    tt.model().addTableModelListener(e -> {
      if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == colIdx) {
        if (ignoreToggleEvents) return;

        int rowIdx = e.getFirstRow();
        if (rowIdx >= 0 && rowIdx < tt.model().getRowCount()) {
          T item = tt.model().itemAt(rowIdx);
          if (item == null) return;

          boolean isPicked = false;
          if (item instanceof ColumnRow) isPicked = ((ColumnRow) item).isPick();
          else if (item instanceof MetricRow) isPicked = ((MetricRow) item).isPick();

          handler.handle(item, isPicked);
          pickAllUpdater.run();
        }
      }
    });
  }

  private void updateColumnPickAllState() {
    if (columnPickAllCheckbox == null || columnPickColumnIndex < 0) return;

    boolean allPicked = columnTable.model().getRowCount() > 0;
    for (int i = 0; i < columnTable.model().getRowCount(); i++) {
      ColumnRow row = columnTable.model().itemAt(i);
      if (row != null && !row.isPick()) {
        allPicked = false;
        break;
      }
    }
    columnPickAllCheckbox.setSelected(allPicked);
  }

  private void updateMetricPickAllState() {
    if (metricPickAllCheckbox == null || metricPickColumnIndex < 0) return;

    boolean allPicked = metricTable.model().getRowCount() > 0;
    for (int i = 0; i < metricTable.model().getRowCount(); i++) {
      MetricRow row = metricTable.model().itemAt(i);
      if (row != null && !row.isPick()) {
        allPicked = false;
        break;
      }
    }
    metricPickAllCheckbox.setSelected(allPicked);
  }

  public void selectFirstProfileRow() {
    if (profileTable.model().getRowCount() > 0) {
      profileTable.table().setRowSelectionInterval(0, 0);
    }
  }

  public void selectFirstTaskRow() {
    if (taskTable.model().getRowCount() > 0) {
      taskTable.table().setRowSelectionInterval(0, 0);
    } else {
      clearQueryAndDetailsTables();
    }
  }

  public void selectFirstQueryRow() {
    if (queryTable.model().getRowCount() > 0) {
      queryTable.table().setRowSelectionInterval(0, 0);
    } else {
      clearQueryAndDetailsTables();
    }
  }

  public void selectFirstDetailsRows() {
    if (columnTable.model().getRowCount() > 0) {
      columnTable.table().setRowSelectionInterval(0, 0);
    }
    if (metricTable.model().getRowCount() > 0) {
      metricTable.table().setRowSelectionInterval(0, 0);
    }
    updateColumnPickAllState();
    updateMetricPickAllState();
  }

  public void clearAllSelections() {
    profileTable.table().clearSelection();
    taskTable.setItems(Collections.emptyList());
    clearQueryAndDetailsTables();
  }

  public void clearQueryAndDetailsTables() {
    queryTable.setItems(Collections.emptyList());
    ignoreToggleEvents = true;
    try {
      columnTable.setItems(Collections.emptyList());
      metricTable.setItems(Collections.emptyList());
    } finally {
      ignoreToggleEvents = false;
    }
    if (columnPickAllCheckbox != null) {
      columnPickAllCheckbox.setSelected(false);
    }
    if (metricPickAllCheckbox != null) {
      metricPickAllCheckbox.setSelected(false);
    }
  }

  public void setColumnItems(java.util.List<ColumnRow> items) {
    ignoreToggleEvents = true;
    try {
      columnTable.setItems(items);
    } finally {
      ignoreToggleEvents = false;
    }
    updateColumnPickAllState();
  }

  public void setMetricItems(java.util.List<MetricRow> items) {
    ignoreToggleEvents = true;
    try {
      metricTable.setItems(items);
    } finally {
      ignoreToggleEvents = false;
    }
    updateMetricPickAllState();
  }

  public void uncheckColumn(CProfile cProfile) {
    if (columnPickColumnIndex < 0) return;
    for (int i = 0; i < columnTable.model().getRowCount(); i++) {
      ColumnRow row = columnTable.model().itemAt(i);
      if (row != null && row.hasOrigin()
          && row.getOrigin().getColId() == cProfile.getColId()
          && row.isPick()) {
        ignoreToggleEvents = true;
        try {
          columnTable.model().setValueAt(false, i, columnPickColumnIndex);
        } finally {
          ignoreToggleEvents = false;
        }
        break;
      }
    }
    updateColumnPickAllState();
  }

  public void uncheckMetric(CProfile cProfile) {
    if (metricPickColumnIndex < 0) return;
    for (int i = 0; i < metricTable.model().getRowCount(); i++) {
      MetricRow row = metricTable.model().itemAt(i);
      if (row != null && row.hasOrigin()
          && row.getOrigin().getYAxis() != null
          && row.getOrigin().getYAxis().getColId() == cProfile.getColId()
          && row.isPick()) {
        ignoreToggleEvents = true;
        try {
          metricTable.model().setValueAt(false, i, metricPickColumnIndex);
        } finally {
          ignoreToggleEvents = false;
        }
        break;
      }
    }
    updateMetricPickAllState();
  }

  public void showNotRunningMessage(String profileName) {
    log.warn("Profile: {} not started", profileName);
  }

  private void handleColumnPickAllAction(boolean selected) {
    handlePickAllAction(selected, columnTable, columnPickColumnIndex, columnPickAllCheckbox,
                        columnSearch, "columns");
  }

  private void handleMetricPickAllAction(boolean selected) {
    handlePickAllAction(selected, metricTable, metricPickColumnIndex, metricPickAllCheckbox,
                        metricSearch, "metrics");
  }

  private <T> void handlePickAllAction(boolean selected,
                                       TTTable<T, JXTable> table,
                                       int pickColIndex,
                                       JCheckBox pickAllCb,
                                       JTextField searchField,
                                       String entityName) {
    if (pickColIndex < 0) return;

    int count = 0;
    for (int i = 0; i < table.model().getRowCount(); i++) {
      T row = table.model().itemAt(i);
      if (row == null) continue;

      boolean currentPick = false;
      if (row instanceof ColumnRow) currentPick = ((ColumnRow) row).isPick();
      else if (row instanceof MetricRow) currentPick = ((MetricRow) row).isPick();

      if (currentPick != selected) {
        count++;
      }
    }

    if (count == 0) return;

    String action = selected ? "select" : "deselect";
    int result = JOptionPane.showConfirmDialog(
        SwingUtilities.getWindowAncestor(this),
        "Are you sure you want to " + action + " all " + count + " " + entityName + "?",
        "Confirm " + action + " all",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (result != JOptionPane.YES_OPTION) {
      pickAllCb.setSelected(!selected);
      return;
    }

    pickAllCb.setEnabled(false);
    searchField.setEnabled(false);
    table.table().setEnabled(false);

    pickAllExecutor.submit(() -> {
      try {
        for (int i = 0; i < table.model().getRowCount(); i++) {
          final int rowIndex = i;
          T row = table.model().itemAt(rowIndex);
          if (row == null) continue;

          boolean currentPick = false;
          if (row instanceof ColumnRow) currentPick = ((ColumnRow) row).isPick();
          else if (row instanceof MetricRow) currentPick = ((MetricRow) row).isPick();

          if (currentPick != selected) {
            SwingUtilities.invokeAndWait(() ->
                                             table.model().setValueAt(selected, rowIndex, pickColIndex)
            );
          }
        }
      } catch (Exception ex) {
        log.error("Error during pick all operation", ex);
        SwingUtilities.invokeLater(() ->
                                       JOptionPane.showMessageDialog(
                                           SwingUtilities.getWindowAncestor(this),
                                           "Error during pick all operation: " + ex.getMessage(),
                                           "Error",
                                           JOptionPane.ERROR_MESSAGE
                                       )
        );
      } finally {
        SwingUtilities.invokeLater(() -> {
          pickAllCb.setEnabled(true);
          searchField.setEnabled(true);
          table.table().setEnabled(true);
        });
      }
    });
  }

  private <T> TTTable<T, JXTable> createBasicTableWithIcon(
      TTRegistry registry,
      Class<T> type,
      RowIconProvider<T> iconProvider) {

    TTTable<T, JXTable> tt = JXTableTables.create(
        registry,
        type,
        TableUi.<T>builder()
            .rowIcon(iconProvider)
            .rowIconInColumn("name")
            .build()
    );
    configureCommon(tt);
    tt.table().setSortable(false);
    return tt;
  }

  private <T> TTTable<T, JXTable> createCheckBoxTableWithIcon(
      TTRegistry registry,
      Class<T> type,
      RowIconProvider<T> iconProvider) {

    TTTable<T, JXTable> tt = JXTableTables.create(
        registry,
        type,
        TableUi.<T>builder()
            .rowIcon(iconProvider)
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    configureCommon(tt);
    table.setEditable(true);

    if (table.getColumnExt("Name") != null) {
      table.getColumnExt("Name").setMinWidth(30);
      table.getColumnExt("Name").setMaxWidth(40);
      table.getColumnExt("Name").setEditable(false);
    }
    return tt;
  }

  private void configureCommon(TTTable<?, JXTable> tt) {
    JXTable table = tt.table();

    if (table.getColumnExt("id") != null) {
      table.getColumnExt("id").setVisible(false);
    }

    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));
  }

  private void initColumnFiltering() {
    columnNameColumnIndex = columnTable.model().schema().modelIndexOf("name");
    columnSorter = new TableRowSorter<>(columnTable.model());
    for (int i = 0; i < columnTable.model().getColumnCount(); i++) {
      columnSorter.setSortable(i, false);
    }
    columnTable.table().setRowSorter(columnSorter);
  }

  private void initMetricFiltering() {
    metricNameColumnIndex = metricTable.model().schema().modelIndexOf("name");
    metricSorter = new TableRowSorter<>(metricTable.model());
    for (int i = 0; i < metricTable.model().getColumnCount(); i++) {
      metricSorter.setSortable(i, false);
    }
    metricTable.table().setRowSorter(metricSorter);
  }

  private void updateColumnFilter() {
    if (columnSearch == null || columnSorter == null) return;

    String searchText = columnSearch.getText();
    if (searchText == null || searchText.isEmpty()) {
      columnSorter.setRowFilter(null);
      return;
    }

    try {
      if (columnNameColumnIndex >= 0) {
        columnSorter.setRowFilter(RowFilter.regexFilter("(?iu)" + searchText, columnNameColumnIndex));
      }
    } catch (PatternSyntaxException e) {
      columnSorter.setRowFilter(RowFilter.regexFilter("$^", 0));
    }
  }

  private void updateMetricFilter() {
    if (metricSearch == null || metricSorter == null) return;

    String searchText = metricSearch.getText();
    if (searchText == null || searchText.isEmpty()) {
      metricSorter.setRowFilter(null);
      return;
    }

    try {
      if (metricNameColumnIndex >= 0) {
        metricSorter.setRowFilter(RowFilter.regexFilter("(?iu)" + searchText, metricNameColumnIndex));
      }
    } catch (PatternSyntaxException e) {
      metricSorter.setRowFilter(RowFilter.regexFilter("$^", 0));
    }
  }

  private void setupLayout() {
    LaF.setBackgroundConfigPanel(CONFIG_PANEL, this);
    this.setBorder(new EtchedBorder());
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    Dimension zeroSize = new Dimension(0, 0);
    int gridY = 0;

    gbc.gridx = 0;
    gbc.weightx = 1.0;

    gbc.gridy = gridY++;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JXTitledSeparator("Profile"), gbc);

    gbc.gridy = gridY++;
    gbc.weighty = 0.15;
    gbc.fill = GridBagConstraints.BOTH;
    JScrollPane profileSP = profileTable.scrollPane();
    profileSP.setPreferredSize(zeroSize);
    add(profileSP, gbc);

    gbc.gridy = gridY++;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JXTitledSeparator("Task"), gbc);

    gbc.gridy = gridY++;
    gbc.weighty = 0.15;
    gbc.fill = GridBagConstraints.BOTH;
    JScrollPane taskSP = taskTable.scrollPane();
    taskSP.setPreferredSize(zeroSize);
    add(taskSP, gbc);

    gbc.gridy = gridY++;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JXTitledSeparator("Query"), gbc);

    gbc.gridy = gridY++;
    gbc.weighty = 0.15;
    gbc.fill = GridBagConstraints.BOTH;
    JScrollPane querySP = queryTable.scrollPane();
    querySP.setPreferredSize(zeroSize);
    add(querySP, gbc);

    JPanel columnsPanel = new JPanel(new BorderLayout());

    JPanel columnSearchRowPanel = new JPanel();
    columnSearchRowPanel.setLayout(new BoxLayout(columnSearchRowPanel, BoxLayout.X_AXIS));

    columnSearch = new JTextField();
    columnSearch.setToolTipText("Search columns...");
    columnSearch.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) { updateColumnFilter(); }
      @Override
      public void removeUpdate(DocumentEvent e) { updateColumnFilter(); }
      @Override
      public void changedUpdate(DocumentEvent e) { updateColumnFilter(); }
    });

    columnPickAllCheckbox = new JCheckBox();
    columnPickAllCheckbox.setToolTipText("Pick it all");
    columnPickAllCheckbox.setPreferredSize(new Dimension(30, columnPickAllCheckbox.getPreferredSize().height));
    columnPickAllCheckbox.setMinimumSize(new Dimension(30, columnPickAllCheckbox.getMinimumSize().height));
    columnPickAllCheckbox.setMaximumSize(new Dimension(30, Short.MAX_VALUE));
    columnPickAllCheckbox.setHorizontalAlignment(JCheckBox.CENTER);
    columnPickAllCheckbox.addActionListener(e -> handleColumnPickAllAction(columnPickAllCheckbox.isSelected()));

    columnSearchRowPanel.add(columnSearch);
    columnSearchRowPanel.add(Box.createHorizontalStrut(1));
    columnSearchRowPanel.add(columnPickAllCheckbox);

    columnsPanel.add(columnSearchRowPanel, BorderLayout.NORTH);
    columnsPanel.add(columnTable.scrollPane(), BorderLayout.CENTER);

    JPanel metricsPanel = new JPanel(new BorderLayout());

    JPanel metricSearchRowPanel = new JPanel();
    metricSearchRowPanel.setLayout(new BoxLayout(metricSearchRowPanel, BoxLayout.X_AXIS));

    metricSearch = new JTextField();
    metricSearch.setToolTipText("Search metrics...");
    metricSearch.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) { updateMetricFilter(); }
      @Override
      public void removeUpdate(DocumentEvent e) { updateMetricFilter(); }
      @Override
      public void changedUpdate(DocumentEvent e) { updateMetricFilter(); }
    });

    metricPickAllCheckbox = new JCheckBox();
    metricPickAllCheckbox.setToolTipText("Pick it all");
    metricPickAllCheckbox.setPreferredSize(new Dimension(30, metricPickAllCheckbox.getPreferredSize().height));
    metricPickAllCheckbox.setMinimumSize(new Dimension(30, metricPickAllCheckbox.getMinimumSize().height));
    metricPickAllCheckbox.setMaximumSize(new Dimension(30, Short.MAX_VALUE));
    metricPickAllCheckbox.setHorizontalAlignment(JCheckBox.CENTER);
    metricPickAllCheckbox.addActionListener(e -> handleMetricPickAllAction(metricPickAllCheckbox.isSelected()));

    metricSearchRowPanel.add(metricSearch);
    metricSearchRowPanel.add(Box.createHorizontalStrut(1));
    metricSearchRowPanel.add(metricPickAllCheckbox);

    metricsPanel.add(metricSearchRowPanel, BorderLayout.NORTH);
    metricsPanel.add(metricTable.scrollPane(), BorderLayout.CENTER);

    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Columns", columnsPanel);
    tabbedPane.addTab("Metrics", metricsPanel);
    tabbedPane.setPreferredSize(zeroSize);

    gbc.gridy = gridY++;
    gbc.weighty = 0.55;
    gbc.fill = GridBagConstraints.BOTH;
    add(tabbedPane, gbc);
  }
}