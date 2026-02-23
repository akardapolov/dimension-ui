package ru.dimension.ui.view.table.renderer;

import java.awt.Component;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import ru.dimension.ui.model.type.ConnectionStatus;
import ru.dimension.ui.view.table.icon.adhoc.ConnectionStatusIcon;

public class ConnectionStatusCellRenderer implements TableCellRenderer {

  private final JLabel iconLabel;
  private final JButton retryButton;

  public ConnectionStatusCellRenderer() {
    iconLabel = new JLabel();
    iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
    iconLabel.setOpaque(true);

    retryButton = new JButton();
    retryButton.setMargin(new Insets(1, 1, 1, 1));
    retryButton.setFocusPainted(false);
    retryButton.setHorizontalAlignment(SwingConstants.CENTER);
  }

  @Override
  public Component getTableCellRendererComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus,
                                                 int row,
                                                 int column) {
    ConnectionStatus status = ConnectionStatus.NOT_CONNECTED;
    if (value instanceof ConnectionStatus s) {
      status = s;
    }

    if (status == ConnectionStatus.NOT_CONNECTED) {
      retryButton.setIcon(new ConnectionStatusIcon(ConnectionStatus.NOT_CONNECTED));
      retryButton.setToolTipText("Not Connected \u2014 Click to retry");
      return retryButton;
    }

    iconLabel.setIcon(new ConnectionStatusIcon(status));
    iconLabel.setToolTipText(ConnectionStatusIcon.getTooltip(status));

    if (isSelected) {
      iconLabel.setBackground(table.getSelectionBackground());
      iconLabel.setForeground(table.getSelectionForeground());
    } else {
      iconLabel.setBackground(table.getBackground());
      iconLabel.setForeground(table.getForeground());
    }

    return iconLabel;
  }
}