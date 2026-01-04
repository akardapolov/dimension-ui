package ru.dimension.ui.view.table.icon.adhoc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import javax.swing.Icon;

/**
 * Icon representing a database table.
 * Renders a grid/spreadsheet symbol in DBeaver style.
 */
public class TableIcon implements Icon {

  private static final int SIZE = 14;
  private final Color color;
  private final Color headerColor;

  private static final Color DEFAULT_COLOR = new Color(0x4CAF50);        // Green
  private static final Color DEFAULT_HEADER_COLOR = new Color(0x2E7D32); // Darker green

  public TableIcon() {
    this(DEFAULT_COLOR, DEFAULT_HEADER_COLOR);
  }

  public TableIcon(Color color) {
    this(color, color.darker());
  }

  public TableIcon(Color color, Color headerColor) {
    this.color = color;
    this.headerColor = headerColor;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    // Outer frame
    g2.setColor(color);
    g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(new Rectangle2D.Double(x + 1, y + 1, SIZE - 2, SIZE - 2));

    // Header row (filled with darker color)
    g2.setColor(headerColor);
    g2.fill(new Rectangle2D.Double(x + 2, y + 2, SIZE - 4, 3));

    // Grid lines
    g2.setColor(color);
    g2.setStroke(new BasicStroke(1.0f));

    // Horizontal row lines
    g2.draw(new Line2D.Double(x + 1, y + 5, x + SIZE - 1, y + 5));
    g2.draw(new Line2D.Double(x + 2, y + 8, x + SIZE - 2, y + 8));
    g2.draw(new Line2D.Double(x + 2, y + 11, x + SIZE - 2, y + 11));

    // Vertical column lines
    g2.draw(new Line2D.Double(x + 5, y + 5, x + 5, y + SIZE - 2));
    g2.draw(new Line2D.Double(x + 9, y + 5, x + 9, y + SIZE - 2));

    g2.dispose();
  }

  @Override
  public int getIconWidth() { return SIZE; }

  @Override
  public int getIconHeight() { return SIZE; }

  public Color getColor() { return color; }
}