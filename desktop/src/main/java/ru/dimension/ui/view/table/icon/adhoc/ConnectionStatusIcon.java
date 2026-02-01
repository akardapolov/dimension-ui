package ru.dimension.ui.view.table.icon.adhoc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import javax.swing.Icon;
import ru.dimension.ui.model.type.ConnectionStatus;

public class ConnectionStatusIcon implements Icon {

  private static final int SIZE = 14;

  private static final Color READY_COLOR = new Color(0x4CAF50);
  private static final Color NOT_CONNECTED_COLOR = new Color(0xF44336);
  private static final Color CONNECTING_COLOR = new Color(0xFF9800);

  private final ConnectionStatus status;
  private final Color color;

  public ConnectionStatusIcon(ConnectionStatus status) {
    this.status = status != null ? status : ConnectionStatus.NOT_CONNECTED;
    this.color = getColorForStatus(this.status);
  }

  private static Color getColorForStatus(ConnectionStatus status) {
    return switch (status) {
      case READY -> READY_COLOR;
      case NOT_CONNECTED -> NOT_CONNECTED_COLOR;
      case CONNECTING -> CONNECTING_COLOR;
    };
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    g2.setColor(color);

    switch (status) {
      case READY -> drawCheckmark(g2, x, y);
      case NOT_CONNECTED -> drawCross(g2, x, y);
      case CONNECTING -> drawHourglass(g2, x, y);
    }

    g2.dispose();
  }

  private void drawCheckmark(Graphics2D g2, int x, int y) {
    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    g2.draw(new Ellipse2D.Double(x + 1, y + 1, SIZE - 2, SIZE - 2));

    Path2D check = new Path2D.Double();
    check.moveTo(x + 4, y + 7);
    check.lineTo(x + 6, y + 10);
    check.lineTo(x + 10, y + 4);
    g2.draw(check);
  }

  private void drawCross(Graphics2D g2, int x, int y) {
    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    g2.draw(new Ellipse2D.Double(x + 1, y + 1, SIZE - 2, SIZE - 2));

    g2.draw(new Line2D.Double(x + 4, y + 4, x + 10, y + 10));
    g2.draw(new Line2D.Double(x + 10, y + 4, x + 4, y + 10));
  }

  private void drawHourglass(Graphics2D g2, int x, int y) {
    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    g2.draw(new Line2D.Double(x + 2, y + 2, x + 12, y + 2));
    g2.draw(new Line2D.Double(x + 2, y + 12, x + 12, y + 12));

    Path2D hourglass = new Path2D.Double();
    hourglass.moveTo(x + 3, y + 2);
    hourglass.lineTo(x + 7, y + 7);
    hourglass.lineTo(x + 3, y + 12);
    hourglass.moveTo(x + 11, y + 2);
    hourglass.lineTo(x + 7, y + 7);
    hourglass.lineTo(x + 11, y + 12);
    g2.draw(hourglass);

    g2.setColor(color.brighter());
    g2.fill(new Arc2D.Double(x + 5, y + 8, 4, 3, 0, 180, Arc2D.PIE));
  }

  @Override
  public int getIconWidth() {
    return SIZE;
  }

  @Override
  public int getIconHeight() {
    return SIZE;
  }

  public ConnectionStatus getStatus() {
    return status;
  }

  public Color getColor() {
    return color;
  }

  public static String getTooltip(ConnectionStatus status) {
    if (status == null) return "Unknown";
    return switch (status) {
      case READY -> "Connected";
      case NOT_CONNECTED -> "Not Connected";
      case CONNECTING -> "Connecting...";
    };
  }
}