package ru.dimension.ui.component.module.adhoc.model;

import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableRowSorter;
import lombok.Data;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTitledSeparator;
import ru.dimension.tt.api.TT;
import ru.dimension.tt.api.TTRegistry;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.tt.swingx.JXTableTables;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.EntityRow;
import ru.dimension.ui.view.table.row.Rows.TimestampRow;

@Data
@Log4j2
public class AdHocModelView extends JPanel {

  private JTabbedPane tableViewPane;
  private JTabbedPane timeStampColumnMetricPane;

  private final TTRegistry registry;
  private final TTTable<ConnectionRow, JXTable> connectionTable;
  private final TTTable<EntityRow, JXTable> tableTable;
  private final TTTable<EntityRow, JXTable> viewTable;
  private final TTTable<TimestampRow, JXTable> timestampTable;
  private final TTTable<ColumnRow, JXTable> columnTable;

  private final JComboBox<String> schemaCatalogCBox;
  private final JLabel statusLabel;

  private final JTextField tableSearchField;
  private final JTextField viewSearchField;
  private final JTextField columnSearchField;

  private boolean blockConnectionAction = false;
  private boolean blockTableAction = false;
  private boolean blockViewAction = false;
  private boolean blockTimestampAction = false;
  private boolean blockColumnAction = false;

  private int tablePickColumnIndex = -1;
  private int viewPickColumnIndex = -1;
  private int columnPickColumnIndex = -1;

  @Setter
  private boolean ignoreTableCheckboxEvents = false;
  @Setter
  private boolean ignoreViewCheckboxEvents = false;
  @Setter
  private boolean ignoreColumnCheckboxEvents = false;

  public interface EntityCheckboxChangeListener {
    void onTableCheckboxChanged(EntityRow row, boolean selected, int modelRow);
    void onViewCheckboxChanged(EntityRow row, boolean selected, int modelRow);
    void onColumnCheckboxChanged(ColumnRow row, boolean selected, int modelRow);
  }

  @Setter
  private EntityCheckboxChangeListener checkboxChangeListener;

  public AdHocModelView() {
    this.registry = TT.builder()
        .scanPackages("ru.dimension.ui.view.table.row")
        .build();

    this.connectionTable = createConnectionTable();
    this.schemaCatalogCBox = createSchemaCatalogComboBox();
    this.tableTable = createTableEntityTable();
    this.viewTable = createViewEntityTable();
    this.timestampTable = createTimestampTable();
    this.columnTable = createColumnTable();
    this.statusLabel = new JLabel();

    this.tableSearchField = new JTextField();
    this.viewSearchField = new JTextField();
    this.columnSearchField = new JTextField();

    tablePickColumnIndex = tableTable.model().schema().modelIndexOf("pick");
    viewPickColumnIndex = viewTable.model().schema().modelIndexOf("pick");
    columnPickColumnIndex = columnTable.model().schema().modelIndexOf("pick");

    setupLayout();
    setupTableFilter();
    setupCheckboxListeners();
  }

  private TTTable<ConnectionRow, JXTable> createConnectionTable() {
    TTTable<ConnectionRow, JXTable> tt = JXTableTables.create(
        registry,
        ConnectionRow.class,
        TableUi.<ConnectionRow>builder()
            .rowIcon(ModelIconProviders.forConnectionRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));
    table.setEditable(false);

    if (table.getColumnExt("ID") != null) {
      table.getColumnExt("ID").setVisible(false);
    }

    return tt;
  }

  private TTTable<EntityRow, JXTable> createTableEntityTable() {
    TTTable<EntityRow, JXTable> tt = JXTableTables.create(
        registry,
        EntityRow.class,
        TableUi.<EntityRow>builder()
            .rowIcon(ModelIconProviders.forTableRow())
            .rowIconInColumn("name")
            .build()
    );

    configureEntityTable(tt.table());
    return tt;
  }

  private TTTable<EntityRow, JXTable> createViewEntityTable() {
    TTTable<EntityRow, JXTable> tt = JXTableTables.create(
        registry,
        EntityRow.class,
        TableUi.<EntityRow>builder()
            .rowIcon(ModelIconProviders.forViewRow())
            .rowIconInColumn("name")
            .build()
    );

    configureEntityTable(tt.table());
    return tt;
  }

  private void configureEntityTable(JXTable table) {
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
  }

  private TTTable<TimestampRow, JXTable> createTimestampTable() {
    TTTable<TimestampRow, JXTable> tt = JXTableTables.create(
        registry,
        TimestampRow.class,
        TableUi.<TimestampRow>builder()
            .rowIcon(ModelIconProviders.forTimestampRow())
            .rowIconInColumn("name")
            .build()
    );

    JXTable table = tt.table();
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new java.awt.Dimension(1, 1));
    table.setEditable(false);
    table.getTableHeader().setVisible(true);

    return tt;
  }

  private TTTable<ColumnRow, JXTable> createColumnTable() {
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

    return tt;
  }

  private void setupCheckboxListeners() {
    if (tablePickColumnIndex >= 0) {
      tableTable.model().addTableModelListener(e -> {
        if (ignoreTableCheckboxEvents || e.getType() != TableModelEvent.UPDATE) return;
        if (e.getColumn() == tablePickColumnIndex) {
          int row = e.getFirstRow();
          if (row >= 0 && row < tableTable.model().getRowCount()) {
            EntityRow item = tableTable.model().itemAt(row);
            if (item != null && checkboxChangeListener != null) {
              checkboxChangeListener.onTableCheckboxChanged(item, item.isPick(), row);
            }
          }
        }
      });
    }

    if (viewPickColumnIndex >= 0) {
      viewTable.model().addTableModelListener(e -> {
        if (ignoreViewCheckboxEvents || e.getType() != TableModelEvent.UPDATE) return;
        if (e.getColumn() == viewPickColumnIndex) {
          int row = e.getFirstRow();
          if (row >= 0 && row < viewTable.model().getRowCount()) {
            EntityRow item = viewTable.model().itemAt(row);
            if (item != null && checkboxChangeListener != null) {
              checkboxChangeListener.onViewCheckboxChanged(item, item.isPick(), row);
            }
          }
        }
      });
    }

    if (columnPickColumnIndex >= 0) {
      columnTable.model().addTableModelListener(e -> {
        if (ignoreColumnCheckboxEvents || e.getType() != TableModelEvent.UPDATE) return;
        if (e.getColumn() == columnPickColumnIndex) {
          int row = e.getFirstRow();
          if (row >= 0 && row < columnTable.model().getRowCount()) {
            ColumnRow item = columnTable.model().itemAt(row);
            if (item != null && checkboxChangeListener != null) {
              checkboxChangeListener.onColumnCheckboxChanged(item, item.isPick(), row);
            }
          }
        }
      });
    }
  }

  private JComboBox<String> createSchemaCatalogComboBox() {
    return new JComboBox<>();
  }

  private void setupLayout() {
    LaF.setBackgroundConfigPanel(CONFIG_PANEL, this);
    this.setBorder(new EtchedBorder());

    initTabbedPanes();

    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    Dimension zeroSize = new Dimension(0, 0);
    int gridY = 0;

    gbc.gridx = 0;
    gbc.weightx = 1.0;

    gbc.gridy = gridY++;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JXTitledSeparator("Connection"), gbc);

    gbc.gridy = gridY++;
    gbc.weighty = 0.2;
    gbc.fill = GridBagConstraints.BOTH;
    JScrollPane connectionScrollPane = connectionTable.scrollPane();
    connectionScrollPane.setPreferredSize(zeroSize);
    add(connectionScrollPane, gbc);

    gbc.gridy = gridY++;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JXTitledSeparator("Schema/Catalog"), gbc);

    gbc.gridy = gridY++;
    add(schemaCatalogCBox, gbc);

    gbc.gridy = gridY++;
    gbc.weighty = 0.3;
    gbc.fill = GridBagConstraints.BOTH;
    tableViewPane.setPreferredSize(zeroSize);
    add(tableViewPane, gbc);

    gbc.gridy = gridY++;
    gbc.weighty = 0.5;
    gbc.fill = GridBagConstraints.BOTH;
    timeStampColumnMetricPane.setPreferredSize(zeroSize);
    add(timeStampColumnMetricPane, gbc);
  }

  private void initTabbedPanes() {
    this.tableViewPane = new JTabbedPane();

    JPanel tablePanel = new JPanel(new BorderLayout());
    tablePanel.add(tableSearchField, BorderLayout.NORTH);
    tablePanel.add(tableTable.scrollPane(), BorderLayout.CENTER);

    JPanel viewPanel = new JPanel(new BorderLayout());
    viewPanel.add(viewSearchField, BorderLayout.NORTH);
    viewPanel.add(viewTable.scrollPane(), BorderLayout.CENTER);

    this.tableViewPane.addTab("Table", tablePanel);
    this.tableViewPane.addTab("View", viewPanel);

    tableViewPane.addChangeListener(e -> {
      blockTableAction = true;
      blockViewAction = true;

      tableTable.table().clearSelection();
      viewTable.table().clearSelection();

      blockTableAction = false;
      blockViewAction = false;
    });

    JPanel columnPanel = new JPanel(new BorderLayout());
    columnPanel.add(columnSearchField, BorderLayout.NORTH);
    columnPanel.add(columnTable.scrollPane(), BorderLayout.CENTER);

    this.timeStampColumnMetricPane = new JTabbedPane();
    this.timeStampColumnMetricPane.addTab("Timestamp", timestampTable.scrollPane());
    this.timeStampColumnMetricPane.addTab("Column", columnPanel);
  }

  private void setupTableFilter() {
    addSearchListener(tableSearchField, tableTable);
    addSearchListener(viewSearchField, viewTable);
    addSearchListener(columnSearchField, columnTable);
  }

  private <T> void addSearchListener(JTextField textField, TTTable<T, JXTable> ttTable) {
    textField.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        filterTable(ttTable, textField.getText());
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        filterTable(ttTable, textField.getText());
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        filterTable(ttTable, textField.getText());
      }
    });
  }

  private <T> void filterTable(TTTable<T, JXTable> ttTable, String pattern) {
    JXTable table = ttTable.table();
    if (table.isEditing()) {
      table.getCellEditor().cancelCellEditing();
    }

    int nameColumnIndex = ttTable.model().schema().modelIndexOf("name");
    if (nameColumnIndex < 0) {
      nameColumnIndex = 0;
    }

    TableRowSorter<?> sorter = new TableRowSorter<>(ttTable.model());
    table.setRowSorter(sorter);

    if (pattern == null || pattern.isEmpty()) {
      sorter.setRowFilter(null);
    } else {
      try {
        final int colIdx = nameColumnIndex;
        sorter.setRowFilter(RowFilter.regexFilter("(?iu)" + pattern, colIdx));
      } catch (PatternSyntaxException e) {
        sorter.setRowFilter(RowFilter.regexFilter("$^", 0));
      }
    }

    table.clearSelection();
  }

  public void addConnectionRow(ConnectionRow row) {
    connectionTable.model().addItem(row);
  }

  public void removeConnectionRowById(int connectionId) {
    List<ConnectionRow> filtered = new ArrayList<>();
    for (int i = 0; i < connectionTable.model().getRowCount(); i++) {
      ConnectionRow row = connectionTable.model().itemAt(i);
      if (row != null) {
        filtered.add(row);
      }
    }
    filtered.removeIf(row -> row.getId() == connectionId);
    connectionTable.setItems(filtered);
  }

  public void clearConnectionTable() {
    connectionTable.setItems(Collections.emptyList());
  }

  public void addTableRow(EntityRow row) {
    tableTable.model().addItem(row);
  }

  public void setTableRows(List<EntityRow> rows) {
    tableTable.setItems(rows);
  }

  public void clearTableTable() {
    tableTable.setItems(Collections.emptyList());
  }

  public void addViewRow(EntityRow row) {
    viewTable.model().addItem(row);
  }

  public void setViewRows(List<EntityRow> rows) {
    viewTable.setItems(rows);
  }

  public void clearViewTable() {
    viewTable.setItems(Collections.emptyList());
  }

  public void addTimestampRow(TimestampRow row) {
    timestampTable.model().addItem(row);
  }

  public void setTimestampRows(List<TimestampRow> rows) {
    timestampTable.setItems(rows);
  }

  public void clearTimestampTable() {
    timestampTable.setItems(Collections.emptyList());
  }

  public void addColumnRow(ColumnRow row) {
    columnTable.model().addItem(row);
  }

  public void setColumnRows(List<ColumnRow> rows) {
    columnTable.setItems(rows);
  }

  public void clearColumnTable() {
    columnTable.setItems(Collections.emptyList());
  }

  public ConnectionRow getSelectedConnectionRow() {
    int row = connectionTable.table().getSelectedRow();
    if (row < 0) return null;
    int modelRow = connectionTable.table().convertRowIndexToModel(row);
    return connectionTable.model().itemAt(modelRow);
  }

  public EntityRow getSelectedTableRow() {
    int row = tableTable.table().getSelectedRow();
    if (row < 0) return null;
    int modelRow = tableTable.table().convertRowIndexToModel(row);
    return tableTable.model().itemAt(modelRow);
  }

  public EntityRow getSelectedViewRow() {
    int row = viewTable.table().getSelectedRow();
    if (row < 0) return null;
    int modelRow = viewTable.table().convertRowIndexToModel(row);
    return viewTable.model().itemAt(modelRow);
  }

  public TimestampRow getSelectedTimestampRow() {
    int row = timestampTable.table().getSelectedRow();
    if (row < 0) return null;
    int modelRow = timestampTable.table().convertRowIndexToModel(row);
    return timestampTable.model().itemAt(modelRow);
  }

  public ColumnRow getSelectedColumnRow() {
    int row = columnTable.table().getSelectedRow();
    if (row < 0) return null;
    int modelRow = columnTable.table().convertRowIndexToModel(row);
    return columnTable.model().itemAt(modelRow);
  }

  public void setTablePickValue(int modelRow, boolean value) {
    ignoreTableCheckboxEvents = true;
    try {
      tableTable.model().setValueAt(value, modelRow, tablePickColumnIndex);
    } finally {
      ignoreTableCheckboxEvents = false;
    }
  }

  public void setViewPickValue(int modelRow, boolean value) {
    ignoreViewCheckboxEvents = true;
    try {
      viewTable.model().setValueAt(value, modelRow, viewPickColumnIndex);
    } finally {
      ignoreViewCheckboxEvents = false;
    }
  }

  public void setColumnPickValue(int modelRow, boolean value) {
    ignoreColumnCheckboxEvents = true;
    try {
      columnTable.model().setValueAt(value, modelRow, columnPickColumnIndex);
    } finally {
      ignoreColumnCheckboxEvents = false;
    }
  }

  public void updateEntityCheckbox(TTTable<EntityRow, JXTable> targetTable, String baseName, boolean selected) {
    ignoreTableCheckboxEvents = (targetTable == tableTable);
    ignoreViewCheckboxEvents = (targetTable == viewTable);
    try {
      int pickIdx = targetTable.model().schema().modelIndexOf("pick");
      for (int i = 0; i < targetTable.model().getRowCount(); i++) {
        EntityRow row = targetTable.model().itemAt(i);
        if (row != null && baseName.equals(row.getName())) {
          targetTable.model().setValueAt(selected, i, pickIdx);
          break;
        }
      }
    } finally {
      ignoreTableCheckboxEvents = false;
      ignoreViewCheckboxEvents = false;
    }
  }

  public void clearAllColumnCheckboxes() {
    ignoreColumnCheckboxEvents = true;
    try {
      for (int i = 0; i < columnTable.model().getRowCount(); i++) {
        columnTable.model().setValueAt(false, i, columnPickColumnIndex);
      }
    } finally {
      ignoreColumnCheckboxEvents = false;
    }
  }
}