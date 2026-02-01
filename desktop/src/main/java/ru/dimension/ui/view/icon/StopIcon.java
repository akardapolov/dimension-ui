package ru.dimension.ui.view.icon;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/**
 * Stop icon - Rounded square.
 */
public class StopIcon implements Icon {

  private static final Color SOFT_SLATE = new Color(150, 160, 180);

  private final int size;
  private final Color primaryColor;

  public StopIcon(int size) {
    this.size = size;
    this.primaryColor = SOFT_SLATE;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2d = (Graphics2D) g.create();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.translate(x, y);

    float margin = 3f;
    float squareSize = size - margin * 2;
    float arcSize = 2f;

    RoundRectangle2D square = new RoundRectangle2D.Float(
        margin, margin, squareSize, squareSize, arcSize, arcSize
    );

    // Draw shadow
    g2d.translate(0.5, 0.5);
    g2d.setColor(new Color(0, 0, 0, 50));
    g2d.fill(square);
    g2d.translate(-0.5, -0.5);

    // Fill with gradient
    GradientPaint gradient = new GradientPaint(
        margin, margin, brighter(primaryColor, 0.15f),
        margin + squareSize, margin + squareSize, darker(primaryColor, 0.1f)
    );
    g2d.setPaint(gradient);
    g2d.fill(square);

    // Draw outline
    g2d.setColor(brighter(primaryColor, 0.2f));
    g2d.setStroke(new BasicStroke(1.0f));
    g2d.draw(square);

    g2d.dispose();
  }

  @Override
  public int getIconWidth() { return size; }

  @Override
  public int getIconHeight() { return size; }

  private Color brighter(Color c, float factor) {
    return new Color(
        Math.min(255, (int) (c.getRed() + 255 * factor)),
        Math.min(255, (int) (c.getGreen() + 255 * factor)),
        Math.min(255, (int) (c.getBlue() + 255 * factor))
    );
  }

  private Color darker(Color c, float factor) {
    return new Color(
        Math.max(0, (int) (c.getRed() * (1 - factor))),
        Math.max(0, (int) (c.getGreen() * (1 - factor))),
        Math.max(0, (int) (c.getBlue() * (1 - factor)))
    );
  }
}