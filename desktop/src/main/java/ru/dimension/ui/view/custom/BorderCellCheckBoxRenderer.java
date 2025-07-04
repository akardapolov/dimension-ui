package ru.dimension.ui.view.custom;

import static ru.dimension.ui.laf.LafColorGroup.BORDER;

import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import ru.dimension.ui.laf.LaF;

public class BorderCellCheckBoxRenderer extends JCheckBox implements TableCellRenderer {

  public BorderCellCheckBoxRenderer() {
    super();
    setOpaque(true);
    setHorizontalAlignment(JLabel.CENTER);
    setBorderPainted(true);

    setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, LaF.getColorBorder(BORDER)));
  }

  @Override
  public Component getTableCellRendererComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus,
                                                 int row,
                                                 int column) {

    if (value instanceof Boolean) {
      setSelected((boolean) value);
    }

    return this;
  }
}

