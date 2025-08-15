package ru.dimension.ui.component.module.adhoc.model;

import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;

import java.awt.BorderLayout;
import java.util.regex.PatternSyntaxException;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.column.ConnectionColumnNames;
import ru.dimension.ui.model.table.JXTableCase;

@Data
@Log4j2
public class AdHocView extends JPanel {
  private static final int DEFAULT_TABLE_ROW_COUNT = 10;
  private static final int NAME_COLUMN_MIN_WIDTH = 30;
  private static final int NAME_COLUMN_MAX_WIDTH = 40;

  private JTabbedPane tableViewPane;
  private JTabbedPane timeStampColumnMetricPane;

  private final JXTableCase connectionCase;
  private final JComboBox<String> schemaCatalogCBox;
  private final JXTableCase tableCase;
  private final JXTableCase viewCase;
  private final JXTableCase timestampCase;
  private final JXTableCase columnCase;
  private final JLabel statusLabel;

  private final JTextField tableSearchField;
  private final JTextField viewSearchField;
  private final JTextField columnSearchField;

  public AdHocView() {
    this.connectionCase = createConnectionCase();
    this.schemaCatalogCBox = createSchemaCatalogComboBox();
    this.tableCase = createTableCase();
    this.viewCase = createViewCase();
    this.timestampCase = createTimestampCase();
    this.columnCase = createColumnCase();
    this.statusLabel = new JLabel();

    this.tableSearchField = new JTextField();
    this.viewSearchField = new JTextField();
    this.columnSearchField = new JTextField();

    setupLayout();
    setupTableFilter();
  }

  private void setupLayout() {
    LaF.setBackgroundConfigPanel(CONFIG_PANEL, this);
    this.setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    initTabbedPanes();

    gbl.row().cell(new JXTitledSeparator("Connection")).fillX();
    gbl.row().cell(connectionCase.getJScrollPane()).fillX();
    gbl.row().cell(new JXTitledSeparator("Schema/Catalog")).fillX();
    gbl.row().cell(schemaCatalogCBox).fillX();
    gbl.row().cell(tableViewPane).fillX();
    gbl.row().cell(timeStampColumnMetricPane).fillXY();

    gbl.done();
  }

  private void initTabbedPanes() {
    this.tableViewPane = new JTabbedPane();

    JPanel tablePanel = new JPanel(new BorderLayout());
    tablePanel.add(tableSearchField, BorderLayout.NORTH);
    tablePanel.add(tableCase.getJScrollPane(), BorderLayout.CENTER);

    JPanel viewPanel = new JPanel(new BorderLayout());
    viewPanel.add(viewSearchField, BorderLayout.NORTH);
    viewPanel.add(viewCase.getJScrollPane(), BorderLayout.CENTER);

    this.tableViewPane.addTab("Table", tablePanel);
    this.tableViewPane.addTab("View", viewPanel);

    tableViewPane.addChangeListener(e -> {
      tableCase.setBlockRunAction(true);
      viewCase.setBlockRunAction(true);

      tableCase.getJxTable().clearSelection();
      viewCase.getJxTable().clearSelection();

      tableCase.setBlockRunAction(false);
      viewCase.setBlockRunAction(false);
    });

    JPanel columnPanel = new JPanel(new BorderLayout());
    columnPanel.add(columnSearchField, BorderLayout.NORTH);
    columnPanel.add(columnCase.getJScrollPane(), BorderLayout.CENTER);

    this.timeStampColumnMetricPane = new JTabbedPane();
    this.timeStampColumnMetricPane.addTab("Timestamp", timestampCase.getJScrollPane());
    this.timeStampColumnMetricPane.addTab("Column", columnPanel);
  }

  private void setupTableFilter() {
    addSearchListener(tableSearchField, tableCase);
    addSearchListener(viewSearchField, viewCase);
    addSearchListener(columnSearchField, columnCase);
  }

  private void addSearchListener(JTextField textField, JXTableCase tableCase) {
    textField.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        filterTable(tableCase, textField.getText());
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        filterTable(tableCase, textField.getText());
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        filterTable(tableCase, textField.getText());
      }
    });
  }

  private void filterTable(JXTableCase tableCase, String pattern) {
    if (tableCase.getJxTable().isEditing()) {
      tableCase.getJxTable().getCellEditor().cancelCellEditing();
    }

    tableCase.setBlockRunAction(true);
    TableRowSorter<?> sorter = new TableRowSorter<>(tableCase.getDefaultTableModel());
    tableCase.getJxTable().setRowSorter(sorter);

    if (pattern == null || pattern.isEmpty()) {
      sorter.setRowFilter(null);
    } else {
      try {
        sorter.setRowFilter(RowFilter.regexFilter("(?iu)" + pattern, ColumnNames.NAME.ordinal()));
      } catch (PatternSyntaxException e) {
        sorter.setRowFilter(RowFilter.regexFilter("$^", 1));
      }
    }

    tableCase.getJxTable().clearSelection();
    tableCase.setBlockRunAction(false);
  }

  private JXTableCase createConnectionCase() {
    JXTableCase jxTableCase = GUIHelper.getJXTableCase(
        7,
        new String[]{
            ConnectionColumnNames.ID.getColName(),
            ConnectionColumnNames.NAME.getColName()
        }
    );

    jxTableCase.getJxTable().getColumnExt(0).setVisible(false);
    jxTableCase.getJxTable().getColumnModel().getColumn(0)
        .setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());

    return jxTableCase;
  }

  private JComboBox<String> createSchemaCatalogComboBox() {
    return new JComboBox<>();
  }

  private JXTableCase createTableCase() {
    return createCheckBoxTableCase();
  }

  private JXTableCase createViewCase() {
    return createCheckBoxTableCase();
  }

  private JXTableCase createTimestampCase() {
    JXTableCase jxTableCase = GUIHelper.getJXTableCase(
        DEFAULT_TABLE_ROW_COUNT,
        new String[]{ColumnNames.NAME.getColName()}
    );
    jxTableCase.getJxTable().getTableHeader().setVisible(true);
    return jxTableCase;
  }

  private JXTableCase createColumnCase() {
    return createCheckBoxTableCase();
  }

  private JXTableCase createCheckBoxTableCase() {
    JXTableCase jxTableCase = GUIHelper.getJXTableCaseCheckBoxAdHoc(
        DEFAULT_TABLE_ROW_COUNT,
        new String[]{
            ColumnNames.ID.getColName(),
            ColumnNames.NAME.getColName(),
            ColumnNames.PICK.getColName()
        },
        ColumnNames.PICK.ordinal()
    );

    configureCommonTableColumns(jxTableCase);
    return jxTableCase;
  }

  private void configureCommonTableColumns(JXTableCase tableCase) {
    tableCase.getJxTable().getColumnExt(ColumnNames.ID.ordinal()).setVisible(false);

    TableColumn nameColumn = tableCase.getJxTable()
        .getColumnModel()
        .getColumn(ColumnNames.NAME.ordinal());

    nameColumn.setMinWidth(NAME_COLUMN_MIN_WIDTH);
    nameColumn.setMaxWidth(NAME_COLUMN_MAX_WIDTH);
  }
}