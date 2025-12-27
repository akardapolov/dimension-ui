package ru.dimension.ui.component.module.model.icon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 * Simple colored circle icon for rows without DTGroup.
 */
public class SimpleColorIcon implements Icon {

  private static final int SIZE = 14;
  private final Color color;

  public SimpleColorIcon(Color color) {
    this.color = color != null ? color : Color.GRAY;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    g.setColor(color);
    g.fillOval(x + 1, y + 1, SIZE - 2, SIZE - 2);
    g.setColor(color.darker());
    g.drawOval(x + 1, y + 1, SIZE - 2, SIZE - 2);
  }

  @Override
  public int getIconWidth() {
    return SIZE;
  }

  @Override
  public int getIconHeight() {
    return SIZE;
  }

  public Color getColor() {
    return color;
  }
}