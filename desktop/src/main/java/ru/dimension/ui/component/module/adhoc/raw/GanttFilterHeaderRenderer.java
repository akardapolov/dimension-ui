package ru.dimension.ui.component.module.adhoc.raw;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

public class GanttFilterHeaderRenderer extends JPanel implements TableCellRenderer {

  private static final int BUTTON_WIDTH = 52;

  private final JLabel titleLabel;
  private final FilterButton filterButton;
  private final GanttFilterCallback callback;

  private boolean activeColumn = false;

  public interface GanttFilterCallback {
    void onFilterButtonClicked(int modelColumnIndex, int screenX, int screenY, int columnWidth);
    boolean isFilterActiveForColumn(int modelColumnIndex);
  }

  public GanttFilterHeaderRenderer(GanttFilterCallback callback) {
    this.callback = callback;

    setLayout(new BorderLayout());
    setOpaque(true);

    setBorder(UIManager.getBorder("TableHeader.cellBorder"));
    if (getBorder() == null) {
      setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY),
          BorderFactory.createEmptyBorder(2, 4, 2, 2)
      ));
    }

    titleLabel = new JLabel();
    Font headerFont = UIManager.getFont("TableHeader.font");
    if (headerFont != null) {
      titleLabel.setFont(headerFont);
    }
    titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

    filterButton = new FilterButton();
    filterButton.setPreferredSize(new Dimension(BUTTON_WIDTH, 0));

    add(titleLabel, BorderLayout.CENTER);
    add(filterButton, BorderLayout.EAST);
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
                                                 boolean isSelected, boolean hasFocus,
                                                 int row, int column) {
    int modelIdx = table.convertColumnIndexToModel(column);

    String text = value != null ? value.toString() : "";
    titleLabel.setText(text);

    activeColumn = callback != null && callback.isFilterActiveForColumn(modelIdx);
    filterButton.setFilterActive(activeColumn);

    Color bg = UIManager.getColor("TableHeader.background");
    Color fg = UIManager.getColor("TableHeader.foreground");

    if (bg == null) bg = new Color(238, 238, 238);
    if (fg == null) fg = Color.BLACK;

    setBackground(bg);
    titleLabel.setForeground(fg);
    filterButton.setParentBackground(bg);

    return this;
  }

  public void installMouseListener(JTable table) {
    table.getTableHeader().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int col = table.getTableHeader().columnAtPoint(e.getPoint());
        if (col < 0) return;

        java.awt.Rectangle headerRect = table.getTableHeader().getHeaderRect(col);
        int buttonX = headerRect.x + headerRect.width - BUTTON_WIDTH - 4;

        if (e.getX() >= buttonX && e.getX() <= headerRect.x + headerRect.width) {
          int modelIdx = table.convertColumnIndexToModel(col);
          java.awt.Point screenPoint = table.getTableHeader().getLocationOnScreen();
          int screenX = screenPoint.x + headerRect.x;
          int screenY = screenPoint.y;

          if (callback != null) {
            callback.onFilterButtonClicked(modelIdx, screenX, screenY, headerRect.width);
          }
        }
      }
    });

    table.getTableHeader().addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        int col = table.getTableHeader().columnAtPoint(e.getPoint());
        if (col >= 0) {
          java.awt.Rectangle headerRect = table.getTableHeader().getHeaderRect(col);
          int buttonX = headerRect.x + headerRect.width - BUTTON_WIDTH - 4;
          if (e.getX() >= buttonX) {
            table.getTableHeader().setCursor(
                java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
          } else {
            table.getTableHeader().setCursor(
                java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
          }
        }
      }
    });
  }

  private static class FilterButton extends JPanel {
    private boolean filterActive = false;
    private Color parentBackground = Color.LIGHT_GRAY;

    FilterButton() {
      setOpaque(false);
    }

    void setFilterActive(boolean active) {
      this.filterActive = active;
    }

    void setParentBackground(Color bg) {
      this.parentBackground = bg;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int w = getWidth();
      int h = getHeight();
      int margin = 3;
      int btnW = w - margin * 2;
      int btnH = h - margin * 2;
      int arc = 6;

      if (filterActive) {
        Color topColor = darken(parentBackground, 0.75);
        Color bottomColor = darken(parentBackground, 0.85);
        g2.setPaint(new GradientPaint(margin, margin, topColor, margin, margin + btnH, bottomColor));
        g2.fillRoundRect(margin, margin, btnW, btnH, arc, arc);

        g2.setColor(darken(parentBackground, 0.55));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(margin, margin, btnW, btnH, arc, arc);

        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawRoundRect(margin + 1, margin + 1, btnW - 2, btnH - 2, arc - 1, arc - 1);
      } else {
        Color topColor = brighten(parentBackground, 1.08);
        Color bottomColor = darken(parentBackground, 0.92);
        g2.setPaint(new GradientPaint(margin, margin, topColor, margin, margin + btnH, bottomColor));
        g2.fillRoundRect(margin, margin, btnW, btnH, arc, arc);

        g2.setColor(darken(parentBackground, 0.65));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(margin, margin, btnW, btnH, arc, arc);

        g2.setColor(new Color(255, 255, 255, 70));
        g2.drawLine(margin + 2, margin + 1, margin + btnW - 2, margin + 1);
      }

      double iconSizeRatio = 0.70;
      double iconW = btnW * iconSizeRatio;
      double iconH = btnH * iconSizeRatio;
      double startX = margin + (btnW - iconW) / 2.0;
      double startY = margin + (btnH - iconH) / 2.0;

      GradientPaint lineGradient;
      GradientPaint dotGradient;
      Color dotBorderColor;

      if (filterActive) {
        Color c1 = new Color(30, 144, 255);
        Color c2 = new Color(0, 191, 255);
        lineGradient = new GradientPaint((float) startX, 0, c1, (float) (startX + iconW), 0, c2);
        dotGradient = new GradientPaint(0, (float) startY, Color.WHITE, 0, (float) (startY + iconH), new Color(200, 230, 255));
        dotBorderColor = new Color(0, 100, 200);
      } else {
        Color c1 = new Color(120, 120, 120);
        Color c2 = new Color(160, 160, 160);
        lineGradient = new GradientPaint((float) startX, 0, c1, (float) (startX + iconW), 0, c2);
        dotGradient = new GradientPaint(0, (float) startY, new Color(220, 220, 220), 0, (float) (startY + iconH), new Color(180, 180, 180));
        dotBorderColor = new Color(100, 100, 100);
      }

      double rowHeight = iconH / 3.0;
      double lineHeight = Math.max(2.0, rowHeight * 0.25);
      double dotRadius = Math.max(3.0, rowHeight * 0.35);
      double[] dotPositions = {0.2, 0.8, 0.5};

      for (int i = 0; i < 3; i++) {
        double currentY = startY + (i * rowHeight) + (rowHeight / 2.0);

        RoundRectangle2D line = new RoundRectangle2D.Double(
            startX, currentY - (lineHeight / 2.0),
            iconW, lineHeight,
            lineHeight, lineHeight
        );

        g2.setPaint(lineGradient);
        g2.fill(line);

        double dotX = startX + (iconW * dotPositions[i]);
        Ellipse2D dot = new Ellipse2D.Double(
            dotX - dotRadius, currentY - dotRadius,
            dotRadius * 2, dotRadius * 2
        );

        g2.setPaint(dotGradient);
        g2.fill(dot);

        g2.setColor(dotBorderColor);
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(dot);
      }

      g2.dispose();
    }

    private Color darken(Color color, double factor) {
      if (color == null) return Color.GRAY;
      return new Color(
          Math.max((int) (color.getRed() * factor), 0),
          Math.max((int) (color.getGreen() * factor), 0),
          Math.max((int) (color.getBlue() * factor), 0),
          color.getAlpha()
      );
    }

    private Color brighten(Color color, double factor) {
      if (color == null) return Color.WHITE;
      return new Color(
          Math.min((int) (color.getRed() * factor), 255),
          Math.min((int) (color.getGreen() * factor), 255),
          Math.min((int) (color.getBlue() * factor), 255),
          color.getAlpha()
      );
    }
  }
}