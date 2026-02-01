package ru.dimension.ui.view.icon;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Base class for custom painted icons with dark theme optimization.
 */
public abstract class AbstractPaintedIcon implements Icon {

  protected final int width;
  protected final int height;

  // Main colors
  protected static final Color SOFT_CYAN = new Color(130, 210, 230);   // Config icon
  protected static final Color SOFT_AMBER = new Color(245, 200, 110);  // Template icon

  // Additional soft colors for dark theme
  protected static final Color SOFT_BLUE = new Color(100, 160, 220);
  protected static final Color SOFT_PURPLE = new Color(160, 140, 200);
  protected static final Color SOFT_STEEL = new Color(120, 150, 180);
  protected static final Color SOFT_SLATE = new Color(140, 150, 165);

  protected AbstractPaintedIcon(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public int getIconWidth() {
    return width;
  }

  @Override
  public int getIconHeight() {
    return height;
  }

  protected Graphics2D prepareGraphics(Graphics g) {
    Graphics2D g2d = (Graphics2D) g.create();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    return g2d;
  }

  protected Color brighter(Color c, float factor) {
    int r = Math.min(255, (int) (c.getRed() + 255 * factor));
    int g = Math.min(255, (int) (c.getGreen() + 255 * factor));
    int b = Math.min(255, (int) (c.getBlue() + 255 * factor));
    return new Color(r, g, b, c.getAlpha());
  }

  protected Color darker(Color c, float factor) {
    int r = Math.max(0, (int) (c.getRed() * (1 - factor)));
    int g = Math.max(0, (int) (c.getGreen() * (1 - factor)));
    int b = Math.max(0, (int) (c.getBlue() * (1 - factor)));
    return new Color(r, g, b, c.getAlpha());
  }
}