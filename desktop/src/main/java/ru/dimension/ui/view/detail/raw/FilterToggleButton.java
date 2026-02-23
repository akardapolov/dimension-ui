package ru.dimension.ui.view.detail.raw;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.Setter;

public class FilterToggleButton extends JToggleButton {

  private static final int ARC = 8;
  private static final int BTN_HEIGHT = 22;
  private static final int PADDING_H = 6;
  private static final int CHECKMARK_WIDTH = 14;
  private static final int RESERVE_CHARS = 3;

  private static final float ALPHA_SELECTED = 1.0f;
  private static final float ALPHA_DESELECTED = 0.45f;
  private static final Color SELECTED_BORDER_COLOR = new Color(40, 40, 40);
  private static final Color DESELECTED_BORDER_COLOR = new Color(160, 160, 160);

  @Getter
  private final String seriesKey;

  @Getter
  @Setter
  private Color seriesColor;

  @Getter
  @Setter
  private double percent;

  @Getter
  @Setter
  private boolean showLabel;

  private boolean hovering = false;

  public FilterToggleButton(String seriesKey, Color seriesColor, double percent, boolean showLabel) {
    super();
    this.seriesKey = seriesKey;
    this.seriesColor = seriesColor != null ? seriesColor : Color.GRAY;
    this.percent = percent;
    this.showLabel = showLabel;

    setToolTipText(buildTooltip());

    setContentAreaFilled(false);
    setFocusPainted(false);
    setBorderPainted(false);
    setOpaque(false);
    setBorder(new EmptyBorder(2, PADDING_H, 2, PADDING_H));
    setMargin(new Insets(1, 4, 1, 4));

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        hovering = true;
        repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        hovering = false;
        repaint();
      }
    });
  }

  private String buildTooltip() {
    StringBuilder sb = new StringBuilder("<html><b>");
    sb.append(seriesKey);
    sb.append("</b><br/>");
    sb.append(formatPercent(percent)).append("%");
    sb.append("</html>");
    return sb.toString();
  }

  public void updateTooltip() {
    setToolTipText(buildTooltip());
  }

  private String formatPercent(double p) {
    if (p == (long) p) {
      return String.valueOf((long) p);
    }
    return String.format("%.1f", p);
  }

  public String getPercentText() {
    return formatPercent(percent) + "%";
  }

  public int computeMinWidth(FontMetrics fm) {
    String pctText = getPercentText();
    int pctWidth = fm.stringWidth(pctText);
    return pctWidth + PADDING_H * 2 + CHECKMARK_WIDTH + 4;
  }

  public int computeFullWidth(FontMetrics fm) {
    String pctText = getPercentText();
    int pctWidth = fm.stringWidth(pctText);
    String label = seriesKey;
    int labelWidth = fm.stringWidth(label);
    int reserveWidth = fm.charWidth('M') * RESERVE_CHARS;
    return pctWidth + labelWidth + PADDING_H * 2 + CHECKMARK_WIDTH + reserveWidth + 8;
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    int w = getWidth();
    int h = getHeight();

    float alpha = isSelected() ? ALPHA_SELECTED : ALPHA_DESELECTED;
    if (hovering && !isSelected()) {
      alpha = 0.7f;
    }

    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    g2.setColor(seriesColor);
    g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, ARC, ARC));

    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    if (isSelected()) {
      g2.setColor(SELECTED_BORDER_COLOR);
      g2.setStroke(new BasicStroke(2f));
    } else {
      g2.setColor(DESELECTED_BORDER_COLOR);
      g2.setStroke(new BasicStroke(1f));
    }
    g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, ARC, ARC));

    Color textColor = getContrastTextColor(seriesColor);
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

    int checkOffset = 0;
    if (isSelected()) {
      g2.setColor(textColor);
      g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
      g2.drawString("✓", 4, h / 2 + 4);
      checkOffset = CHECKMARK_WIDTH;
    }

    g2.setFont(getFont().deriveFont(11f));
    FontMetrics fm = g2.getFontMetrics();

    String pctText = getPercentText();
    int pctWidth = fm.stringWidth(pctText);

    int contentArea = w - PADDING_H * 2 - checkOffset;

    if (showLabel) {
      String label = seriesKey;
      int labelWidth = fm.stringWidth(label);

      int totalNeeded = labelWidth + 4 + pctWidth;
      if (totalNeeded > contentArea) {
        int availForLabel = contentArea - pctWidth - 4;
        if (availForLabel > fm.stringWidth("…")) {
          label = truncateToFit(label, fm, availForLabel);
        } else {
          label = "";
        }
        labelWidth = fm.stringWidth(label);
      }

      int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
      g2.setColor(textColor);

      if (!label.isEmpty()) {
        int labelX = PADDING_H + checkOffset;
        g2.drawString(label, labelX, textY);

        int pctX = w - PADDING_H - pctWidth;
        g2.drawString(pctText, pctX, textY);
      } else {
        int pctX = PADDING_H + checkOffset + (contentArea - pctWidth) / 2;
        g2.drawString(pctText, pctX, textY);
      }
    } else {
      int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
      g2.setColor(textColor);
      int pctX = PADDING_H + checkOffset + (contentArea - pctWidth) / 2;
      g2.drawString(pctText, pctX, textY);
    }

    g2.dispose();
  }

  private String truncateToFit(String text, FontMetrics fm, int maxWidth) {
    String ellipsis = "…";
    int ellipsisWidth = fm.stringWidth(ellipsis);
    if (fm.stringWidth(text) <= maxWidth) {
      return text;
    }
    for (int i = text.length() - 1; i > 0; i--) {
      if (fm.stringWidth(text.substring(0, i)) + ellipsisWidth <= maxWidth) {
        return text.substring(0, i) + ellipsis;
      }
    }
    return ellipsis;
  }

  private static Color getContrastTextColor(Color bg) {
    if (bg == null) return Color.BLACK;
    double luminance = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
    return luminance > 140 ? Color.BLACK : Color.WHITE;
  }
}