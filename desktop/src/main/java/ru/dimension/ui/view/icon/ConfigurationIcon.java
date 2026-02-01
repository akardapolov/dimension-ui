package ru.dimension.ui.view.icon;

import java.awt.*;
import java.awt.geom.*;

/**
 * Gear/cog icon for configuration button.
 */
public class ConfigurationIcon extends AbstractPaintedIcon {

  private final Color primaryColor;

  public ConfigurationIcon() {
    this(20, 20);
  }

  public ConfigurationIcon(int size) {
    this(size, size);
  }

  public ConfigurationIcon(int width, int height) {
    super(width, height);
    this.primaryColor = SOFT_CYAN;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2d = prepareGraphics(g);
    g2d.translate(x, y);

    float centerX = width / 2f;
    float centerY = height / 2f;
    float outerRadius = Math.min(width, height) / 2f - 1f;
    float innerRadius = outerRadius * 0.55f;
    float holeRadius = outerRadius * 0.25f;
    int teeth = 8;

    // Create gear shape
    Path2D gear = createGearPath(centerX, centerY, outerRadius, innerRadius, teeth);

    // Draw shadow/depth
    g2d.translate(0.5, 0.5);
    g2d.setColor(new Color(0, 0, 0, 60));
    g2d.fill(gear);
    g2d.translate(-0.5, -0.5);

    // Fill gear with gradient (using SOFT_CYAN as base)
    GradientPaint gradient = new GradientPaint(
        0, 0, brighter(primaryColor, 0.1f),
        width, height, darker(primaryColor, 0.15f)
    );
    g2d.setPaint(gradient);
    g2d.fill(gear);

    // Draw gear outline
    g2d.setColor(brighter(primaryColor, 0.2f));
    g2d.setStroke(new BasicStroke(1.0f));
    g2d.draw(gear);

    // Draw center hole
    Ellipse2D hole = new Ellipse2D.Float(
        centerX - holeRadius,
        centerY - holeRadius,
        holeRadius * 2,
        holeRadius * 2
    );
    g2d.setColor(new Color(40, 44, 52)); // Dark background
    g2d.fill(hole);
    g2d.setColor(darker(primaryColor, 0.3f));
    g2d.draw(hole);

    g2d.dispose();
  }

  private Path2D createGearPath(float cx, float cy, float outerR, float innerR, int teeth) {
    Path2D path = new Path2D.Float();
    int steps = teeth * 4;
    double angleStep = 2 * Math.PI / steps;

    for (int i = 0; i < steps; i++) {
      double angle = i * angleStep - Math.PI / 2;
      float radius = (i % 4 < 2) ? outerR : innerR;
      float px = cx + (float) (radius * Math.cos(angle));
      float py = cy + (float) (radius * Math.sin(angle));

      if (i == 0) {
        path.moveTo(px, py);
      } else {
        path.lineTo(px, py);
      }
    }
    path.closePath();
    return path;
  }
}