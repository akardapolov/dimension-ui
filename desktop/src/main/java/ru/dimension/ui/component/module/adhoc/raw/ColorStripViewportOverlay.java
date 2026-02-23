package ru.dimension.ui.component.module.adhoc.raw;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ToolTipManager;

public class ColorStripViewportOverlay extends JComponent {

  private static final int STRIP_WIDTH = 36;
  private static final int INNER_MARGIN = 3;
  private static final int BORDER_RADIUS = 4;

  private volatile List<RowColorEntry> rowEntries = Collections.emptyList();
  private volatile boolean active = false;

  private JTable linkedTable;
  private JScrollPane linkedScrollPane;

  public record RowColorEntry(Color color, String label) {}

  public ColorStripViewportOverlay() {
    setOpaque(false);
    setPreferredSize(new Dimension(STRIP_WIDTH, 0));
    setMinimumSize(new Dimension(STRIP_WIDTH, 0));

    ToolTipManager.sharedInstance().registerComponent(this);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        handleClick(e);
      }
    });

    addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        repaint();
      }
    });
  }

  public void linkTable(JTable table, JScrollPane scrollPane) {
    this.linkedTable = table;
    this.linkedScrollPane = scrollPane;
  }

  public void setRowEntries(List<RowColorEntry> entries) {
    this.rowEntries = entries != null ? new ArrayList<>(entries) : Collections.emptyList();
    repaint();
  }

  public void setActive(boolean active) {
    this.active = active;
    setVisible(active);
    repaint();
  }

  public boolean isActive() {
    return active;
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    if (!active || rowEntries.isEmpty()) return null;

    int rowIndex = getRowIndexAtY(event.getY());
    if (rowIndex >= 0 && rowIndex < rowEntries.size()) {
      RowColorEntry entry = rowEntries.get(rowIndex);
      if (entry != null && entry.label() != null) {
        return "<html><b>Row " + (rowIndex + 1) + ":</b> " + entry.label() + "</html>";
      }
    }
    return null;
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (!active || rowEntries.isEmpty()) return;

    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int w = getWidth();
    int h = getHeight();

    g2.setColor(new Color(30, 30, 35));
    g2.fillRoundRect(0, 0, w, h, BORDER_RADIUS, BORDER_RADIUS);

    g2.setColor(new Color(80, 80, 90));
    g2.drawRoundRect(0, 0, w - 1, h - 1, BORDER_RADIUS, BORDER_RADIUS);

    g2.setColor(new Color(180, 180, 180));
    g2.setFont(g2.getFont().deriveFont(java.awt.Font.BOLD, 8f));
    String header = "MAP";
    int headerWidth = g2.getFontMetrics().stringWidth(header);
    g2.drawString(header, (w - headerWidth) / 2, 10);

    int stripTop = 14;
    int stripBottom = h - 4;
    int stripHeight = stripBottom - stripTop;
    int stripLeft = INNER_MARGIN;
    int stripRight = w - INNER_MARGIN;
    int stripWidth = stripRight - stripLeft;

    if (stripHeight <= 0 || stripWidth <= 0) {
      g2.dispose();
      return;
    }

    int rowCount = rowEntries.size();

    if (rowCount <= stripHeight) {
      double pixelsPerRow = (double) stripHeight / rowCount;

      for (int i = 0; i < rowCount; i++) {
        RowColorEntry entry = rowEntries.get(i);
        if (entry == null || entry.color() == null) continue;

        int y1 = stripTop + (int) (i * pixelsPerRow);
        int y2 = stripTop + (int) ((i + 1) * pixelsPerRow);
        int rowH = Math.max(y2 - y1, 1);

        g2.setColor(entry.color());
        g2.fillRect(stripLeft, y1, stripWidth, rowH);
      }
    } else {
      for (int py = 0; py < stripHeight; py++) {
        int rowStart = (int) ((long) py * rowCount / stripHeight);
        int rowEnd = Math.min((int) ((long) (py + 1) * rowCount / stripHeight), rowCount);

        Color dominant = findDominantColor(rowStart, rowEnd);
        if (dominant != null) {
          g2.setColor(dominant);
          g2.fillRect(stripLeft, stripTop + py, stripWidth, 1);
        }
      }
    }

    paintViewportIndicator(g2, stripTop, stripHeight, stripLeft, stripWidth);

    g2.dispose();
  }

  private void paintViewportIndicator(Graphics2D g2, int stripTop, int stripHeight,
                                      int stripLeft, int stripWidth) {
    if (linkedTable == null || linkedScrollPane == null) return;

    int totalRows = linkedTable.getRowCount();
    if (totalRows <= 0) return;

    java.awt.Rectangle visibleRect = linkedTable.getVisibleRect();
    int firstVisibleRow = linkedTable.rowAtPoint(new java.awt.Point(0, visibleRect.y));
    int lastVisibleRow = linkedTable.rowAtPoint(
        new java.awt.Point(0, visibleRect.y + visibleRect.height - 1));

    if (firstVisibleRow < 0) firstVisibleRow = 0;
    if (lastVisibleRow < 0) lastVisibleRow = totalRows - 1;

    double topFraction = (double) firstVisibleRow / totalRows;
    double bottomFraction = (double) (lastVisibleRow + 1) / totalRows;

    int indicatorTop = stripTop + (int) (topFraction * stripHeight);
    int indicatorBottom = stripTop + (int) (bottomFraction * stripHeight);
    int indicatorHeight = Math.max(indicatorBottom - indicatorTop, 4);

    g2.setColor(new Color(255, 255, 255, 60));
    g2.fillRect(stripLeft - 1, indicatorTop, stripWidth + 2, indicatorHeight);

    g2.setColor(new Color(255, 255, 255, 150));
    g2.drawRect(stripLeft - 1, indicatorTop, stripWidth + 2, indicatorHeight);
  }

  private void handleClick(MouseEvent e) {
    if (!active || rowEntries.isEmpty() || linkedTable == null) return;

    int rowIndex = getRowIndexAtY(e.getY());
    if (rowIndex >= 0 && rowIndex < linkedTable.getRowCount()) {
      linkedTable.scrollRectToVisible(
          linkedTable.getCellRect(rowIndex, 0, true));
      linkedTable.setRowSelectionInterval(rowIndex, rowIndex);
    }
  }

  private int getRowIndexAtY(int y) {
    int stripTop = 14;
    int stripBottom = getHeight() - 4;
    int stripHeight = stripBottom - stripTop;

    if (stripHeight <= 0 || rowEntries.isEmpty()) return -1;

    int relativeY = y - stripTop;
    if (relativeY < 0 || relativeY >= stripHeight) return -1;

    return (int) ((long) relativeY * rowEntries.size() / stripHeight);
  }

  private Color findDominantColor(int fromRow, int toRow) {
    if (fromRow >= toRow || fromRow >= rowEntries.size()) return null;

    Map<Color, Integer> freq = new LinkedHashMap<>();
    for (int i = fromRow; i < Math.min(toRow, rowEntries.size()); i++) {
      RowColorEntry entry = rowEntries.get(i);
      if (entry != null && entry.color() != null) {
        freq.merge(entry.color(), 1, Integer::sum);
      }
    }

    return freq.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(null);
  }
}