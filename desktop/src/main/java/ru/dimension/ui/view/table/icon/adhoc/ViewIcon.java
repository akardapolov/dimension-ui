package ru.dimension.ui.view.table.icon.adhoc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.Icon;

public class ViewIcon implements Icon {

  private static final int SIZE = 14;
  private final Color color;
  private final Color irisColor;

  private static final Color DEFAULT_COLOR = new Color(0x2196F3);
  private static final Color DEFAULT_IRIS_COLOR = new Color(0x1565C0);

  public ViewIcon() {
    this(DEFAULT_COLOR, DEFAULT_IRIS_COLOR);
  }

  public ViewIcon(Color color) {
    this(color, color.darker());
  }

  public ViewIcon(Color color, Color irisColor) {
    this.color = color;
    this.irisColor = irisColor;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    g2.setColor(color);
    g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    Path2D eye = new Path2D.Double();
    eye.moveTo(x + 1, y + 7);
    eye.curveTo(x + 2, y + 3, x + 5, y + 2, x + 7, y + 2);
    eye.curveTo(x + 9, y + 2, x + 12, y + 3, x + 13, y + 7);
    eye.curveTo(x + 12, y + 11, x + 9, y + 12, x + 7, y + 12);
    eye.curveTo(x + 5, y + 12, x + 2, y + 11, x + 1, y + 7);
    eye.closePath();
    g2.draw(eye);

    g2.setColor(irisColor);
    g2.draw(new Ellipse2D.Double(x + 4, y + 4, 6, 6));

    g2.fill(new Ellipse2D.Double(x + 5.5, y + 5.5, 3, 3));

    g2.setColor(Color.WHITE);
    g2.fill(new Ellipse2D.Double(x + 5, y + 5, 1.5, 1.5));

    g2.dispose();
  }

  @Override
  public int getIconWidth() { return SIZE; }

  @Override
  public int getIconHeight() { return SIZE; }

  public Color getColor() { return color; }
}