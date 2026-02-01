package ru.dimension.ui.view.table.icon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import javax.swing.Icon;

public class VectorIcons {

  private static final int SIZE = 16;

  private static Graphics2D config(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    return g2;
  }

  public static class Profile implements Icon {
    private final Color color;
    public Profile(Color color) { this.color = color; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics2D g2 = config(g);
      g2.setColor(color);
      g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      Path2D hex = new Path2D.Double();
      hex.moveTo(x + 8, y + 2);
      hex.lineTo(x + 14, y + 5);
      hex.lineTo(x + 14, y + 11);
      hex.lineTo(x + 8, y + 14);
      hex.lineTo(x + 2, y + 11);
      hex.lineTo(x + 2, y + 5);
      hex.closePath();
      g2.draw(hex);

      Path2D inner = new Path2D.Double();
      inner.moveTo(x + 8, y + 8);
      inner.lineTo(x + 8, y + 14);
      inner.moveTo(x + 8, y + 8);
      inner.lineTo(x + 2, y + 5);
      inner.moveTo(x + 8, y + 8);
      inner.lineTo(x + 14, y + 5);
      g2.draw(inner);

      g2.dispose();
    }
    @Override public int getIconWidth() { return SIZE; }
    @Override public int getIconHeight() { return SIZE; }
  }

  public static class Task implements Icon {
    private final Color color;
    public Task(Color color) { this.color = color; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics2D g2 = config(g);
      g2.setColor(color);
      g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      Path2D path = new Path2D.Double();
      path.moveTo(x + 2, y + 13);
      path.lineTo(x + 6, y + 9);
      path.lineTo(x + 9, y + 12);
      path.lineTo(x + 13, y + 4);
      g2.draw(path);

      Path2D arrow = new Path2D.Double();
      arrow.moveTo(x + 9, y + 4);
      arrow.lineTo(x + 13.5, y + 3.5);
      arrow.lineTo(x + 13.5, y + 8);
      g2.draw(arrow);

      g2.dispose();
    }
    @Override public int getIconWidth() { return SIZE; }
    @Override public int getIconHeight() { return SIZE; }
  }

  public static class Query implements Icon {
    private final Color color;
    public Query(Color color) { this.color = color; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics2D g2 = config(g);
      g2.setColor(color);

      g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      Path2D prism = new Path2D.Double();
      prism.moveTo(x + 2, y + 8);
      prism.lineTo(x + 6, y + 4);
      prism.lineTo(x + 6, y + 12);
      prism.closePath();
      g2.draw(prism);

      g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
      g2.draw(new Line2D.Double(x + 8, y + 5, x + 13, y + 2));
      g2.draw(new Line2D.Double(x + 8.5, y + 8, x + 13.5, y + 8));
      g2.draw(new Line2D.Double(x + 8, y + 11, x + 13, y + 14));

      g2.dispose();
    }
    @Override public int getIconWidth() { return SIZE; }
    @Override public int getIconHeight() { return SIZE; }
  }

  public static class Design implements Icon {
    private final Color color;
    public Design(Color color) { this.color = color; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics2D g2 = config(g);
      g2.setColor(color);
      g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      g2.draw(new Rectangle2D.Double(x + 3, y + 2, 10, 12));

      g2.draw(new Line2D.Double(x + 5, y + 5, x + 11, y + 5));

      g2.fill(new Rectangle2D.Double(x + 5, y + 7, 2, 2));
      g2.draw(new Line2D.Double(x + 8, y + 8, x + 11, y + 8));

      g2.fill(new Rectangle2D.Double(x + 5, y + 10, 2, 2));
      g2.draw(new Line2D.Double(x + 8, y + 11, x + 11, y + 11));

      g2.dispose();
    }
    @Override public int getIconWidth() { return SIZE; }
    @Override public int getIconHeight() { return SIZE; }
  }

  public static class Metric implements Icon {
    private final Color color;
    public Metric(Color color) { this.color = color; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      Graphics2D g2 = config(g);
      g2.setColor(color);
      g2.setStroke(new BasicStroke(1.5f));

      // Axis
      g2.draw(new Line2D.Double(x + 2, y + 2, x + 2, y + 13));
      g2.draw(new Line2D.Double(x + 2, y + 13, x + 14, y + 13));

      // Bars
      g2.fillRect(x + 4, y + 8, 2, 5);
      g2.fillRect(x + 7, y + 5, 2, 8);
      g2.fillRect(x + 10, y + 9, 2, 4);

      g2.dispose();
    }
    @Override public int getIconWidth() { return SIZE; }
    @Override public int getIconHeight() { return SIZE; }
  }
}