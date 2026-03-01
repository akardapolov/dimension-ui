package ru.dimension.ui.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.tt.swing.TableUi;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.view.table.icon.ModelIconProviders;
import ru.dimension.ui.view.table.row.Rows.TimestampColumnRow;

@Log4j2
public class TimestampColumnChooser extends JDialog {

  private final TTTable<TimestampColumnRow, JXTable> ttTable;
  private final List<CProfile> timestampColumns;
  private final JCheckBox compressionCheckbox;
  private CProfile selectedColumn;
  private boolean compression = true;
  private boolean confirmed = false;

  public TimestampColumnChooser(Frame owner, List<CProfile> timestampColumns) {
    super(owner, "Select Timestamp Column", true);
    this.timestampColumns = timestampColumns;

    List<TimestampColumnRow> rows = timestampColumns.stream()
        .map(TimestampColumnRow::new)
        .collect(Collectors.toList());

    ttTable = ru.dimension.tt.swingx.JXTableTables.create(
        GUIHelper.getRegistry(),
        TimestampColumnRow.class,
        TableUi.<TimestampColumnRow>builder()
            .rowIcon(ModelIconProviders.forTimestampColumnRow())
            .rowIconInColumn("colName")
            .build()
    );

    JXTable table = ttTable.table();
    table.setSortable(false);
    table.setShowVerticalLines(true);
    table.setShowHorizontalLines(true);
    table.setGridColor(java.awt.Color.GRAY);
    table.setIntercellSpacing(new Dimension(1, 1));
    GUIHelper.configureStandardTableProperties(table);

    ttTable.setItems(rows);

    if (!rows.isEmpty()) {
      table.setRowSelectionInterval(0, 0);
    }

    ttTable.scrollPane().setPreferredSize(new Dimension(450, 120));

    JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
    contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    JLabel label = new JLabel("Multiple timestamp columns found. Please choose one:");
    contentPanel.add(label, BorderLayout.NORTH);
    contentPanel.add(ttTable.scrollPane(), BorderLayout.CENTER);

    JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

    compressionCheckbox = new JCheckBox("Enable compression", true);
    compressionCheckbox.setToolTipText("Enable data compression for this table");
    bottomPanel.add(compressionCheckbox, BorderLayout.WEST);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

    JButton okButton = new JButton("OK");
    okButton.addActionListener(e -> {
      int selectedRow = table.getSelectedRow();
      if (selectedRow >= 0) {
        int modelRow = table.convertRowIndexToModel(selectedRow);
        List<TimestampColumnRow> items = ttTable.model().items();
        if (modelRow >= 0 && modelRow < items.size()) {
          TimestampColumnRow row = items.get(modelRow);
          if (row.hasOrigin()) {
            selectedColumn = row.getOrigin();
          }
          compression = compressionCheckbox.isSelected();
          confirmed = true;
        }
      }
      dispose();
    });

    JButton cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(e -> {
      confirmed = false;
      dispose();
    });

    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    bottomPanel.add(buttonPanel, BorderLayout.EAST);

    contentPanel.add(bottomPanel, BorderLayout.SOUTH);

    setContentPane(contentPanel);
    pack();
    setMinimumSize(new Dimension(500, 250));
    setLocationRelativeTo(owner);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
  }

  public CProfile getSelectedColumn() {
    return selectedColumn;
  }

  public boolean isCompression() {
    return compression;
  }

  public boolean isConfirmed() {
    return confirmed;
  }

  public static TimestampResult choose(List<CProfile> timestampColumns) {
    if (timestampColumns == null || timestampColumns.isEmpty()) {
      return null;
    }

    if (timestampColumns.size() == 1) {
      log.info("Auto-selected single timestamp column: {}", timestampColumns.get(0).getColName());
      return new TimestampResult(timestampColumns.get(0), true);
    }

    final TimestampResult[] result = {null};

    if (SwingUtilities.isEventDispatchThread()) {
      result[0] = showDialog(timestampColumns);
    } else {
      try {
        SwingUtilities.invokeAndWait(() -> result[0] = showDialog(timestampColumns));
      } catch (Exception e) {
        log.error("Error showing timestamp chooser dialog", e);
      }
    }

    return result[0];
  }

  private static TimestampResult showDialog(List<CProfile> timestampColumns) {
    TimestampColumnChooser chooser = new TimestampColumnChooser(null, timestampColumns);
    chooser.setVisible(true);
    if (chooser.isConfirmed() && chooser.getSelectedColumn() != null) {
      log.info("User selected timestamp column: {}, compression: {}",
               chooser.getSelectedColumn().getColName(), chooser.isCompression());
      return new TimestampResult(chooser.getSelectedColumn(), chooser.isCompression());
    }
    return null;
  }

  public record TimestampResult(CProfile column, boolean compression) {}
}