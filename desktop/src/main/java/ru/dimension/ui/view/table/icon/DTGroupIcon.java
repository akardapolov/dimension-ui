package ru.dimension.ui.view.table.icon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import javax.swing.Icon;
import ru.dimension.db.metadata.DTGroup;

public class DTGroupIcon implements Icon {

  private static final int SIZE = 14;
  private final Color color;
  private final DTGroup group;

  private static final Font ICON_FONT = new Font("SansSerif", Font.BOLD, 9);
  private static final Font TINY_FONT = new Font("SansSerif", Font.BOLD, 8);

  public DTGroupIcon(DTGroup group) {
    this.group = group;
    this.color = getColorForGroup(group);
  }

  private static Color getColorForGroup(DTGroup group) {
    if (group == null) {
      return Color.GRAY;
    }
    return switch (group) {
      case INTEGER  -> new Color(0x4CAF50);
      case FLOAT    -> new Color(0x2196F3);
      case STRING   -> new Color(0xFF9800);
      case DATETIME -> new Color(0xBB86FC);
      case BINARY   -> new Color(0x607D8B);
      case ARRAY    -> new Color(0xE91E63);
      case MAP      -> new Color(0x00BCD4);
      case SET      -> new Color(0xCDDC39);
      case BOOLEAN  -> new Color(0xF44336);
      case NETWORK  -> new Color(0x3F51B5);
      case JSON     -> new Color(0xFFC107);
      case SPATIAL  -> new Color(0x009688);
      case INTERVAL -> new Color(0x795548);
    };
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    g2.setColor(color);

    g2.setStroke(new BasicStroke(1.2f));

    if (group == null) {
      g2.drawOval(x + 2, y + 2, SIZE - 5, SIZE - 5);
      g2.dispose();
      return;
    }

    switch (group) {
      case INTEGER -> drawCenteredText(g2, "123", x, y, ICON_FONT);

      case FLOAT -> drawCenteredText(g2, "1.0", x, y, ICON_FONT);

      case STRING -> drawCenteredText(g2, "abc", x, y, ICON_FONT);

      case DATETIME -> {
        g2.draw(new Ellipse2D.Double(x + 1, y + 1, SIZE - 3, SIZE - 3));
        g2.draw(new Line2D.Double(x + 7, y + 7, x + 7, y + 3.5));
        g2.draw(new Line2D.Double(x + 7, y + 7, x + 10, y + 7));
      }

      case BOOLEAN -> {
        g2.draw(new Rectangle2D.Double(x + 2, y + 2, SIZE - 4, SIZE - 4));
        Path2D check = new Path2D.Double();
        check.moveTo(x + 4, y + 7);
        check.lineTo(x + 6, y + 10);
        check.lineTo(x + 10, y + 4);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(check);
      }

      case BINARY -> drawCenteredText(g2, "01", x, y, ICON_FONT);

      case ARRAY -> drawCenteredText(g2, "[ ]", x, y, ICON_FONT);

      case MAP -> drawCenteredText(g2, "{ }", x, y, ICON_FONT);

      case JSON -> drawCenteredText(g2, "JS", x, y, TINY_FONT);

      case SET -> {
        g2.draw(new Ellipse2D.Double(x + 1, y + 3, 8, 8));
        g2.draw(new Ellipse2D.Double(x + 5, y + 3, 8, 8));
      }

      case SPATIAL -> {
        Path2D path = new Path2D.Double();
        path.moveTo(x + 7, y + 13);
        path.lineTo(x + 2, y + 4);
        path.lineTo(x + 12, y + 4);
        path.closePath();
        g2.fill(path);
      }

      case NETWORK -> {
        g2.fillOval(x + 6, y + 1, 3, 3);
        g2.fillOval(x + 2, y + 9, 3, 3);
        g2.fillOval(x + 10, y + 9, 3, 3);
        g2.drawLine(x + 7, y + 3, x + 4, y + 10);
        g2.drawLine(x + 8, y + 3, x + 11, y + 10);
        g2.drawLine(x + 4, y + 10, x + 11, y + 10);
      }

      case INTERVAL -> {
        Path2D path = new Path2D.Double();
        path.moveTo(x + 3, y + 2);
        path.lineTo(x + 11, y + 2);
        path.lineTo(x + 7, y + 7);
        path.lineTo(x + 11, y + 12);
        path.lineTo(x + 3, y + 12);
        path.lineTo(x + 7, y + 7);
        path.closePath();
        g2.draw(path);
      }
    }

    g2.dispose();
  }

  private void drawCenteredText(Graphics2D g2, String text, int x, int y, Font font) {
    g2.setFont(font);
    FontMetrics fm = g2.getFontMetrics();

    double textWidth = fm.getStringBounds(text, g2).getWidth();
    double textHeight = fm.getAscent() - fm.getDescent();

    double cx = x + (SIZE - textWidth) / 2.0;
    double cy = y + (SIZE + textHeight) / 2.0 - 1;

    g2.drawString(text, (float) cx, (float) cy);
  }

  @Override
  public int getIconWidth() {
    return SIZE;
  }

  @Override
  public int getIconHeight() {
    return SIZE;
  }

  public DTGroup getGroup() {
    return group;
  }

  public Color getColor() {
    return color;
  }
}