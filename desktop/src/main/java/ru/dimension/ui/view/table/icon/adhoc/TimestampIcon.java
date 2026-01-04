package ru.dimension.ui.view.table.icon.adhoc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import javax.swing.Icon;

/**
 * Icon representing a timestamp column.
 * Uses the same style and color as DATETIME from DTGroupIcon.
 */
public class TimestampIcon implements Icon {

  private static final int SIZE = 14;

  // Same color as DATETIME in DTGroupIcon
  private static final Color TIMESTAMP_COLOR = new Color(0xBB86FC); // Purple

  private final Color color;

  public TimestampIcon() {
    this(TIMESTAMP_COLOR);
  }

  public TimestampIcon(Color color) {
    this.color = color;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    g2.setColor(color);
    g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Clock face (circle)
    g2.draw(new Ellipse2D.Double(x + 1, y + 1, SIZE - 2, SIZE - 2));

    // Clock center point
    int cx = x + 7;
    int cy = y + 7;

    // Hour markers (small dots at 12, 3, 6, 9)
    g2.setStroke(new BasicStroke(1.5f));
    g2.fill(new Ellipse2D.Double(cx - 0.5, y + 2, 1, 1));      // 12 o'clock
    g2.fill(new Ellipse2D.Double(x + SIZE - 3, cy - 0.5, 1, 1)); // 3 o'clock
    g2.fill(new Ellipse2D.Double(cx - 0.5, y + SIZE - 3, 1, 1)); // 6 o'clock
    g2.fill(new Ellipse2D.Double(x + 2, cy - 0.5, 1, 1));        // 9 o'clock

    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    // Minute hand (pointing to 12)
    g2.draw(new Line2D.Double(cx, cy, cx, y + 3));

    // Hour hand (pointing to 3)
    g2.draw(new Line2D.Double(cx, cy, x + SIZE - 3.5, cy));

    // Center dot
    g2.fill(new Ellipse2D.Double(cx - 1, cy - 1, 2, 2));

    g2.dispose();
  }

  @Override
  public int getIconWidth() { return SIZE; }

  @Override
  public int getIconHeight() { return SIZE; }

  public Color getColor() { return color; }

  public static Color getTimestampColor() { return TIMESTAMP_COLOR; }
}