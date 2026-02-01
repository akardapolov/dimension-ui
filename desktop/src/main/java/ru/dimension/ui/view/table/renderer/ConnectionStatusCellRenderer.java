package ru.dimension.ui.view.table.renderer;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import ru.dimension.ui.model.type.ConnectionStatus;
import ru.dimension.ui.view.table.icon.adhoc.ConnectionStatusIcon;

public class ConnectionStatusCellRenderer extends DefaultTableCellRenderer {

  @Override
  public Component getTableCellRendererComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus,
                                                 int row,
                                                 int column) {

    JLabel label = (JLabel) super.getTableCellRendererComponent(
        table, "", isSelected, hasFocus, row, column);

    label.setHorizontalAlignment(SwingConstants.CENTER);

    if (value instanceof ConnectionStatus status) {
      label.setIcon(new ConnectionStatusIcon(status));
      label.setToolTipText(ConnectionStatusIcon.getTooltip(status));
    } else {
      label.setIcon(new ConnectionStatusIcon(ConnectionStatus.NOT_CONNECTED));
      label.setToolTipText("Unknown");
    }

    return label;
  }
}