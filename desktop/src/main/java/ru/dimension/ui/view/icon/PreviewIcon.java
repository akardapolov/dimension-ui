package ru.dimension.ui.view.icon;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/**
 * Preview/Eye icon - Stylized eye shape.
 */
public class PreviewIcon implements Icon {

  private static final Color SOFT_AMBER = new Color(205, 140, 210);

  private final int size;
  private final Color primaryColor;

  public PreviewIcon(int size) {
    this.size = size;
    this.primaryColor = SOFT_AMBER;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2d = (Graphics2D) g.create();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.translate(x, y);

    float centerX = size / 2f;
    float centerY = size / 2f;

    float eyeWidth = size - 2f;
    float eyeHeight = size * 0.65f;

    Path2D eye = new Path2D.Float();

    // Top arc
    Arc2D topArc = new Arc2D.Float(
        centerX - eyeWidth / 2, centerY - eyeHeight / 2,
        eyeWidth, eyeHeight,
        0, 180, Arc2D.OPEN
    );

    // Bottom arc
    Arc2D bottomArc = new Arc2D.Float(
        centerX - eyeWidth / 2, centerY - eyeHeight / 2,
        eyeWidth, eyeHeight,
        180, 180, Arc2D.OPEN
    );

    eye.append(topArc, false);
    eye.append(bottomArc, true);
    eye.closePath();

    // Draw shadow
    g2d.translate(0.5, 0.5);
    g2d.setColor(new Color(0, 0, 0, 40));
    g2d.fill(eye);
    g2d.translate(-0.5, -0.5);

    // Fill eye with gradient
    GradientPaint gradient = new GradientPaint(
        0, centerY - eyeHeight / 2, brighter(primaryColor, 0.15f),
        0, centerY + eyeHeight / 2, darker(primaryColor, 0.1f)
    );
    g2d.setPaint(gradient);
    g2d.fill(eye);

    // Draw eye outline
    g2d.setColor(brighter(primaryColor, 0.2f));
    g2d.setStroke(new BasicStroke(1.0f));
    g2d.draw(eye);

    // Larger pupil (inner circle)
    float pupilRadius = size * 0.22f;
    Ellipse2D pupil = new Ellipse2D.Float(
        centerX - pupilRadius,
        centerY - pupilRadius,
        pupilRadius * 2,
        pupilRadius * 2
    );
    g2d.setColor(new Color(50, 55, 65));
    g2d.fill(pupil);

    // Larger iris ring
    float irisRadius = size * 0.32f;
    Ellipse2D iris = new Ellipse2D.Float(
        centerX - irisRadius,
        centerY - irisRadius,
        irisRadius * 2,
        irisRadius * 2
    );
    g2d.setColor(darker(primaryColor, 0.3f));
    g2d.setStroke(new BasicStroke(1.5f));
    g2d.draw(iris);

    // Draw highlight
    float highlightSize = size * 0.10f;
    Ellipse2D highlight = new Ellipse2D.Float(
        centerX - pupilRadius + 1,
        centerY - pupilRadius + 1,
        highlightSize,
        highlightSize
    );
    g2d.setColor(new Color(255, 255, 255, 180));
    g2d.fill(highlight);

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