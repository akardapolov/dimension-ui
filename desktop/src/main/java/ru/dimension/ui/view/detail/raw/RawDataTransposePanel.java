package ru.dimension.ui.view.detail.raw;

import static ru.dimension.ui.laf.LafColorGroup.TABLE_BACKGROUND;
import static ru.dimension.ui.laf.LafColorGroup.TABLE_FONT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import org.jdesktop.swingx.JXTable;
import ru.dimension.ui.laf.LaF;

public class RawDataTransposePanel extends JPanel {

  private static final int TRIANGLE_SIZE = 8;
  private static final int MAX_COLUMN_NAME_CHARS = 50;

  private final JXTable transposeTable;
  private final DefaultTableModel transposeTableModel;
  private final JScrollPane scrollPane;

  private String highlightColumnName;
  private Color highlightColor;
  private Map<String, Color> seriesColorMap;
  private int profileRowIndex = -1;

  public RawDataTransposePanel() {
    super(new BorderLayout());
    setBorder(BorderFactory.createTitledBorder("Row Details"));
    setMinimumSize(new Dimension(200, 0));

    transposeTableModel = new DefaultTableModel(new String[]{"Column", "Value"}, 0) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    transposeTable = new JXTable(transposeTableModel);
    transposeTable.setEditable(false);
    transposeTable.setShowVerticalLines(true);
    transposeTable.setShowHorizontalLines(true);
    transposeTable.setGridColor(java.awt.Color.GRAY);
    transposeTable.setIntercellSpacing(new java.awt.Dimension(1, 1));
    transposeTable.setBackground(LaF.getBackgroundColor(TABLE_BACKGROUND, LaF.getLafType()));
    transposeTable.setForeground(LaF.getBackgroundColor(TABLE_FONT, LaF.getLafType()));

    transposeTable.setDefaultRenderer(Object.class, new TransposeCellRenderer());
    transposeTable.getColumnModel().getColumn(1).setCellRenderer(new TextAreaCellRenderer());

    scrollPane = new JScrollPane(transposeTable,
                                 ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                 ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    add(scrollPane, BorderLayout.CENTER);
  }

  public void setSeriesColorMap(Map<String, Color> seriesColorMap) {
    this.seriesColorMap = seriesColorMap;
  }

  public void setHighlightColumnName(String columnName) {
    this.highlightColumnName = columnName;
  }

  public void updateFromRow(String[] columnNames, Object[] rowValues) {
    transposeTableModel.setRowCount(0);
    profileRowIndex = -1;

    if (columnNames == null || rowValues == null) {
      return;
    }

    int count = Math.min(columnNames.length, rowValues.length);
    for (int i = 0; i < count; i++) {
      String value = rowValues[i] != null ? rowValues[i].toString() : "";
      transposeTableModel.addRow(new Object[]{columnNames[i], value});

      if (highlightColumnName != null && columnNames[i].equals(highlightColumnName)) {
        profileRowIndex = i;
        resolveHighlightColor(value);
      }
    }

    adjustColumnWidth(columnNames);
    adjustRowHeights();

    if (profileRowIndex >= 0) {
      scrollToProfileRow();
    }
  }

  public void clear() {
    transposeTableModel.setRowCount(0);
    profileRowIndex = -1;
    highlightColor = null;
  }

  private void adjustColumnWidth(String[] columnNames) {
    FontMetrics fm = transposeTable.getFontMetrics(transposeTable.getFont());

    int maxWidth = fm.stringWidth("Column") + 20;
    for (String name : columnNames) {
      if (name == null) continue;
      String displayed = name.length() > MAX_COLUMN_NAME_CHARS
          ? name.substring(0, MAX_COLUMN_NAME_CHARS)
          : name;
      int w = fm.stringWidth(displayed) + 20;
      if (w > maxWidth) {
        maxWidth = w;
      }
    }

    TableColumn col = transposeTable.getColumnModel().getColumn(0);
    col.setPreferredWidth(maxWidth);
    col.setMinWidth(maxWidth);
    col.setMaxWidth(maxWidth);
  }

  private void adjustRowHeights() {
    SwingUtilities.invokeLater(() -> {
      int columnIndex = 1;
      int tableWidth = transposeTable.getColumnModel().getColumn(columnIndex).getWidth();
      if (tableWidth <= 0) {
        tableWidth = transposeTable.getWidth() - transposeTable.getColumnModel().getColumn(0).getWidth();
      }
      if (tableWidth <= 0) {
        tableWidth = 200;
      }

      for (int row = 0; row < transposeTable.getRowCount(); row++) {
        Object val = transposeTable.getValueAt(row, columnIndex);
        String text = val != null ? val.toString() : "";

        JTextArea area = new JTextArea(text);
        area.setFont(transposeTable.getFont());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setSize(tableWidth, Short.MAX_VALUE);

        int preferredHeight = area.getPreferredSize().height + 4;
        int minHeight = transposeTable.getRowHeight();
        transposeTable.setRowHeight(row, Math.max(minHeight, preferredHeight));
      }
    });
  }

  private void resolveHighlightColor(String value) {
    highlightColor = null;
    if (seriesColorMap != null && value != null) {
      highlightColor = seriesColorMap.get(value);
    }
  }

  private void scrollToProfileRow() {
    if (profileRowIndex < 0 || profileRowIndex >= transposeTable.getRowCount()) {
      return;
    }

    SwingUtilities.invokeLater(() -> {
      java.awt.Rectangle cellRect = transposeTable.getCellRect(profileRowIndex, 0, true);

      int viewportHeight = scrollPane.getViewport().getExtentSize().height;
      int rowCenter = cellRect.y + cellRect.height / 2;
      int scrollY = Math.max(0, rowCenter - viewportHeight / 2);

      scrollPane.getViewport().setViewPosition(
          new java.awt.Point(scrollPane.getViewport().getViewPosition().x, scrollY));
    });
  }

  private class TransposeCellRenderer extends DefaultTableCellRenderer {

    private Color triangleColor = null;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      triangleColor = null;

      if (row == profileRowIndex && highlightColor != null) {
        triangleColor = highlightColor;
      }

      return c;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);

      if (triangleColor != null) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(triangleColor);

        int[] xPoints = {getWidth() - TRIANGLE_SIZE, getWidth(), getWidth()};
        int[] yPoints = {0, 0, TRIANGLE_SIZE};
        g2.fillPolygon(xPoints, yPoints, 3);

        g2.dispose();
      }
    }
  }

  private class TextAreaCellRenderer extends JTextArea implements javax.swing.table.TableCellRenderer {

    private Color triangleColor = null;

    public TextAreaCellRenderer() {
      setLineWrap(true);
      setWrapStyleWord(true);
      setOpaque(true);
      setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
      if (isSelected) {
        setBackground(table.getSelectionBackground());
        setForeground(table.getSelectionForeground());
      } else {
        setBackground(table.getBackground());
        setForeground(table.getForeground());
      }

      setFont(table.getFont());
      setText(value != null ? value.toString() : "");

      triangleColor = null;
      if (row == profileRowIndex && highlightColor != null) {
        triangleColor = highlightColor;
      }

      int columnWidth = table.getColumnModel().getColumn(column).getWidth();
      setSize(columnWidth, Short.MAX_VALUE);
      int preferredHeight = getPreferredSize().height + 4;
      int currentHeight = table.getRowHeight(row);
      if (preferredHeight > currentHeight) {
        table.setRowHeight(row, preferredHeight);
      }

      return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);

      if (triangleColor != null) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(triangleColor);

        int[] xPoints = {getWidth() - TRIANGLE_SIZE, getWidth(), getWidth()};
        int[] yPoints = {0, 0, TRIANGLE_SIZE};
        g2.fillPolygon(xPoints, yPoints, 3);

        g2.dispose();
      }
    }
  }
}