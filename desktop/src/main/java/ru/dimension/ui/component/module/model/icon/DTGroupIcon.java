package ru.dimension.ui.component.module.model.icon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import ru.dimension.db.metadata.DTGroup;

/**
 * Icon representing a data type group.
 * Different DTGroups are marked with different colors.
 */
public class DTGroupIcon implements Icon {

  private static final int SIZE = 14;
  private final Color color;
  private final DTGroup group;

  public DTGroupIcon(DTGroup group) {
    this.group = group;
    this.color = getColorForGroup(group);
  }

  private static Color getColorForGroup(DTGroup group) {
    if (group == null) {
      return Color.GRAY;
    }
    return switch (group) {
      case INTEGER  -> new Color(0x4CAF50);  // Green
      case FLOAT    -> new Color(0x2196F3);  // Blue
      case STRING   -> new Color(0xFF9800);  // Orange
      case DATETIME -> new Color(0x9C27B0);  // Purple
      case BINARY   -> new Color(0x607D8B);  // Blue Grey
      case ARRAY    -> new Color(0xE91E63);  // Pink
      case MAP      -> new Color(0x00BCD4);  // Cyan
      case SET      -> new Color(0xCDDC39);  // Lime
      case BOOLEAN  -> new Color(0xF44336);  // Red
      case NETWORK  -> new Color(0x3F51B5);  // Indigo
      case JSON     -> new Color(0xFFC107);  // Amber
      case SPATIAL  -> new Color(0x009688);  // Teal
      case INTERVAL -> new Color(0x795548);  // Brown
    };
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

  public DTGroup getGroup() {
    return group;
  }

  public Color getColor() {
    return color;
  }
}