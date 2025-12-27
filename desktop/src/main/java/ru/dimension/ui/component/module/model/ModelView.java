package ru.dimension.ui.component.module.model;

import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collections;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelEvent;

import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTitledSeparator;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swing.icon.RowIconProvider;
import ru.dimension.tt.swingx.JXTableTables;
import ru.dimension.ui.component.module.model.icon.ModelIconProviders;
import ru.dimension.ui.component.module.model.row.Rows.*;
import ru.dimension.ui.laf.LaF;

@Log4j2
public class ModelView extends JPanel {

  private final TTTable<ProfileRow, JXTable> profileTable;
  private final TTTable<TaskRow, JXTable> taskTable;
  private final TTTable<QueryRow, JXTable> queryTable;
  private final TTTable<ColumnRow, JXTable> columnTable;
  private final TTTable<MetricRow, JXTable> metricTable;

  public ModelView() {
    TTRegistry registry = TT.builder()
        .scanPackages("ru.dimension.ui.component.module.model.row")
        .build();

    // Create tables with icons
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

    setupLayout();
  }

  // --- Getters ---
  public TTTable<ProfileRow, JXTable> getProfileTable() { return profileTable; }
  public TTTable<TaskRow, JXTable>    getTaskTable()    { return taskTable; }
  public TTTable<QueryRow, JXTable>   getQueryTable()   { return queryTable; }
  public TTTable<ColumnRow, JXTable>  getColumnTable()  { return columnTable; }
  public TTTable<MetricRow, JXTable>  getMetricTable()  { return metricTable; }

  // --- Listener Registration ---

  public void setColumnToggleListener(ModelHandler<ColumnRow> handler) {
    setupTableListener(columnTable, "pick", handler);
  }

  public void setMetricToggleListener(ModelHandler<MetricRow> handler) {
    setupTableListener(metricTable, "pick", handler);
  }

  @SuppressWarnings("unchecked")
  private <T> void setupTableListener(TTTable<T, ?> tt, String colName, ModelHandler<T> handler) {
    int colIdx = tt.model().schema().modelIndexOf(colName);
    if (colIdx < 0) {
      log.warn("Column '{}' not found in table schema", colName);
      return;
    }

    tt.model().addTableModelListener(e -> {
      if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == colIdx) {
        int rowIdx = e.getFirstRow();
        if (rowIdx >= 0 && rowIdx < tt.model().getRowCount()) {
          T item = tt.model().itemAt(rowIdx);
          if (item == null) return;

          boolean isPicked = false;
          if (item instanceof ColumnRow) isPicked = ((ColumnRow) item).isPick();
          else if (item instanceof MetricRow) isPicked = ((MetricRow) item).isPick();

          handler.handle(item, isPicked);
        }
      }
    });
  }

  // --- Selection Helpers ---
  public void selectFirstProfileRow() {
    if (profileTable.model().getRowCount() > 0)
      profileTable.table().setRowSelectionInterval(0, 0);
  }

  public void selectFirstTaskRow() {
    if (taskTable.model().getRowCount() > 0)
      taskTable.table().setRowSelectionInterval(0, 0);
    else
      clearQueryAndDetailsTables();
  }

  public void selectFirstQueryRow() {
    if (queryTable.model().getRowCount() > 0)
      queryTable.table().setRowSelectionInterval(0, 0);
    else
      clearQueryAndDetailsTables();
  }

  public void selectFirstDetailsRows() {
    if (columnTable.model().getRowCount() > 0)
      columnTable.table().setRowSelectionInterval(0, 0);
    if (metricTable.model().getRowCount() > 0)
      metricTable.table().setRowSelectionInterval(0, 0);
  }

  public void clearAllSelections() {
    profileTable.table().clearSelection();
    taskTable.setItems(Collections.emptyList());
    clearQueryAndDetailsTables();
  }

  public void clearQueryAndDetailsTables() {
    queryTable.setItems(Collections.emptyList());
    columnTable.setItems(Collections.emptyList());
    metricTable.setItems(Collections.emptyList());
  }

  public void showNotRunningMessage(String profileName) {
    log.warn("Profile: {} not started", profileName);
  }

  // --- Creation Helpers with Icons ---

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
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
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
    table.setSortable(false);
    if (table.getColumnExt("id") != null) {
      table.getColumnExt("id").setVisible(false);
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

    // Profile
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

    // Task
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

    // Query
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

    // Details Tabs
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Columns", columnTable.scrollPane());
    tabbedPane.addTab("Metrics", metricTable.scrollPane());
    tabbedPane.setPreferredSize(zeroSize);

    gbc.gridy = gridY++;
    gbc.weighty = 0.55;
    gbc.fill = GridBagConstraints.BOTH;
    add(tabbedPane, gbc);
  }
}