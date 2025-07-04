package com.egantt.swing.cell.renderer.adapter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CustomCellRenderer extends DefaultTableCellRenderer {
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        setFont(new Font("Arial", Font.BOLD, 12));
        ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);

        return c;
    }
}
