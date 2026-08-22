package ru.dimension.ui.view.panel.config.ui;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;
import static ru.dimension.ui.laf.LafColorGroup.REPORT;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jdesktop.swingx.VerticalLayout;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;

public class ChartSettingsPanel extends JPanel {

  private final JCheckBox chkHideBuiltInLegend;
  private final JCheckBox chkHideYAxis;
  private final JCheckBox chkHideXAxis;
  private final JCheckBox chkHidePlotInsets;
  private final JCheckBox chkHideChartPadding;

  private final ChartPreviewPanel previewPanel;

  public ChartSettingsPanel() {
    setBorder(new EtchedBorder());
    setLayout(new BorderLayout());

    chkHideBuiltInLegend = new JCheckBox("Built-in legend");
    chkHideYAxis         = new JCheckBox("Y-axis");
    chkHideXAxis         = new JCheckBox("X-axis");
    chkHidePlotInsets    = new JCheckBox("Plot insets");
    chkHideChartPadding  = new JCheckBox("Chart padding");

    previewPanel = new ChartPreviewPanel(this);

    JXTaskPaneContainer container = new JXTaskPaneContainer();
    LaF.setBackgroundColor(REPORT, container);
    container.setBackgroundPainter(null);

    JPanel cardPanel = new JPanel(new VerticalLayout());
    LaF.setBackgroundColor(REPORT, cardPanel);
    cardPanel.add(container);

    JScrollPane scrollPane = new JScrollPane();
    GUIHelper.setScrolling(scrollPane);
    scrollPane.setViewportView(cardPanel);

    JXTaskPane zonesPane = new JXTaskPane();
    zonesPane.setTitle("Hide / Show Zones");
    zonesPane.setCollapsed(false);
    zonesPane.add(buildZonesContent());

    container.add(zonesPane);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(scrollPane).fillXY();
    gbl.done();

    ActionListener repaint = e -> previewPanel.repaint();
    chkHideBuiltInLegend.addActionListener(repaint);
    chkHideYAxis        .addActionListener(repaint);
    chkHideXAxis        .addActionListener(repaint);
    chkHidePlotInsets   .addActionListener(repaint);
    chkHideChartPadding .addActionListener(repaint);
  }

  private JPanel buildZonesContent() {
    JPanel root = new JPanel(new BorderLayout(8, 0));
    root.setOpaque(false);

    JPanel left = new JPanel();
    left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
    left.setOpaque(false);

    JXTitledSeparator separator = new JXTitledSeparator("Hide:");
    separator.setAlignmentX(LEFT_ALIGNMENT);
    left.add(separator);
    left.add(Box.createVerticalStrut(4));

    for (JCheckBox chk : new JCheckBox[]{
        chkHideBuiltInLegend,
        chkHideYAxis,
        chkHideXAxis,
        chkHidePlotInsets,
        chkHideChartPadding}) {
      chk.setAlignmentX(LEFT_ALIGNMENT);
      left.add(chk);
    }

    JPanel previewWrapper = new JPanel(new BorderLayout());
    previewWrapper.setOpaque(false);
    previewWrapper.add(previewPanel, BorderLayout.NORTH);

    root.add(left,           BorderLayout.WEST);
    root.add(previewWrapper, BorderLayout.CENTER);

    return root;
  }

  public boolean isHideCustomLegend()  { return false; }
  public boolean isHideBuiltInLegend() { return chkHideBuiltInLegend.isSelected(); }
  public boolean isHideYAxis()         { return chkHideYAxis.isSelected(); }
  public boolean isHideXAxis()         { return chkHideXAxis.isSelected(); }
  public boolean isHidePlotInsets()    { return chkHidePlotInsets.isSelected(); }
  public boolean isHideChartPadding()  { return chkHideChartPadding.isSelected(); }

  public void setHideCustomLegend(boolean v)  {}
  public void setHideBuiltInLegend(boolean v) { chkHideBuiltInLegend.setSelected(v); previewPanel.repaint(); }
  public void setHideYAxis(boolean v)         { chkHideYAxis.setSelected(v);         previewPanel.repaint(); }
  public void setHideXAxis(boolean v)         { chkHideXAxis.setSelected(v);         previewPanel.repaint(); }
  public void setHidePlotInsets(boolean v)    { chkHidePlotInsets.setSelected(v);    previewPanel.repaint(); }
  public void setHideChartPadding(boolean v)  { chkHideChartPadding.setSelected(v);  previewPanel.repaint(); }

  public void addChangeListener(ActionListener listener) {
    chkHideBuiltInLegend.addActionListener(listener);
    chkHideYAxis        .addActionListener(listener);
    chkHideXAxis        .addActionListener(listener);
    chkHidePlotInsets   .addActionListener(listener);
    chkHideChartPadding .addActionListener(listener);
  }

  private static final class ChartPreviewPanel extends JPanel {

    private static final int W = 360;
    private static final int H = 128;

    private static final Color S1 = new Color( 91, 155, 213);
    private static final Color S2 = new Color(237, 125,  49);
    private static final Color S3 = new Color(112, 173,  71);

    private static final String[] LABELS = {"series_1", "series_2", "series_3"};
    private static final Color[]  COLORS = {S1, S2, S3};

    private static final float[][] FRACS = {
        {.12f,.18f,.22f,.30f,.20f,.26f,.34f,.24f,.28f,.18f,.22f,.16f},
        {.08f,.16f,.19f,.10f,.14f,.18f,.12f,.19f,.31f,.33f,.09f,.10f},
        {.05f,.07f,.09f,.31f,.08f,.20f,.07f,.09f,.06f,.09f,.07f,.06f}
    };

    private final ChartSettingsPanel cfg;

    ChartPreviewPanel(ChartSettingsPanel cfg) {
      this.cfg = cfg;
      LaF.setBackgroundConfigPanel(CHART_PANEL, this);
      setOpaque(true);
      setPreferredSize(new Dimension(W, H));
      setMinimumSize  (new Dimension(W, H));
      setMaximumSize  (new Dimension(W, H));
    }

    @Override
    protected void paintComponent(Graphics g0) {
      super.paintComponent(g0);
      if (getWidth() <= 0 || getHeight() <= 0) return;

      Graphics2D g = (Graphics2D) g0.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      try {
        Rectangle card = new Rectangle(6, 6, getWidth() - 12, getHeight() - 12);
        paintCard(g, card);
        paintContent(g, card);
      } finally {
        g.dispose();
      }
    }

    private void paintCard(Graphics2D g, Rectangle card) {
      g.setColor(cardColor());
      g.fillRoundRect(card.x, card.y, card.width, card.height, 16, 16);
      g.setColor(borderColor());
      g.setStroke(new BasicStroke(1f));
      g.drawRoundRect(card.x, card.y, card.width, card.height, 16, 16);
    }

    private void paintContent(Graphics2D g, Rectangle card) {
      boolean hideLegend       = cfg.isHideBuiltInLegend();
      boolean hideYAxis        = cfg.isHideYAxis();
      boolean hideXAxis        = cfg.isHideXAxis();
      boolean hidePlotInsets   = cfg.isHidePlotInsets();
      boolean hideChartPadding = cfg.isHideChartPadding();

      g.setFont(getFont().deriveFont(Font.BOLD, 14f));
      g.setColor(primaryText());
      g.drawString("Chart preview", card.x + 12, card.y + 15);

      int cp = hideChartPadding ? 0 : 4;

      int legendW = hideLegend ? 0 : 76;

      int boxX = card.x + 10 + cp;
      int boxY = card.y + 20 + cp;
      int boxW = card.width  - 20 - cp * 2;
      int boxH = card.height - 30 - cp * 2;
      if (boxW < 8) boxW = 8;
      if (boxH < 8) boxH = 8;

      g.setColor(chartBoxColor());
      g.fill(new RoundRectangle2D.Float(boxX, boxY, boxW, boxH, 10, 10));
      g.setColor(borderColor());
      g.setStroke(new BasicStroke(0.8f));
      g.draw(new RoundRectangle2D.Float(boxX, boxY, boxW, boxH, 10, 10));

      if (!hideLegend) {
        drawLegend(g, boxX + boxW - legendW - 2, boxY + 4, legendW, boxH - 8);
      }

      int pi     = hidePlotInsets ? 0 : 4;
      int yAxisW = hideYAxis ? 0 : 26;
      int xAxisH = hideXAxis ? 0 : 16;

      int plotX = boxX + pi + yAxisW;
      int plotY = boxY + pi;
      int plotW = boxW - pi * 2 - yAxisW - legendW - (hideLegend ? 0 : 4);
      int plotH = boxH - pi * 2 - xAxisH;
      if (plotW < 4) plotW = 4;
      if (plotH < 4) plotH = 4;

      g.setColor(plotBg());
      g.fillRect(plotX, plotY, plotW, plotH);

      drawGrid(g, plotX, plotY, plotW, plotH);
      drawStackedAreas(g, plotX, plotY, plotW, plotH);

      if (!hideYAxis) drawYAxis(g, boxX + pi, plotY, yAxisW, plotH);
      if (!hideXAxis) drawXAxis(g, plotX, plotY + plotH, plotW, xAxisH);

      g.setColor(borderColor());
      g.setStroke(new BasicStroke(0.7f));
      g.drawRect(plotX, plotY, plotW, plotH);
    }

    private void drawLegend(Graphics2D g, int lx, int ly, int lw, int lh) {
      g.setColor(withAlpha(borderColor(), 80));
      g.fill(new RoundRectangle2D.Float(lx, ly, lw, lh, 8, 8));
      g.setColor(withAlpha(borderColor(), 140));
      g.setStroke(new BasicStroke(0.8f));
      g.draw(new RoundRectangle2D.Float(lx, ly, lw, lh, 8, 8));

      g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
      FontMetrics fm = g.getFontMetrics();

      int itemH = 22;

      int totalHeight = COLORS.length * itemH;
      int startY = ly + (lh - totalHeight) / 2;

      for (int i = 0; i < COLORS.length; i++) {
        int iy = startY + i * itemH;

        g.setColor(COLORS[i]);
        g.fillRoundRect(lx + 6, iy, 10, 8, 3, 3);

        g.setColor(COLORS[i].darker());
        g.setStroke(new BasicStroke(0.5f));
        g.drawRoundRect(lx + 6, iy, 10, 8, 3, 3);

        g.setColor(primaryText());
        g.drawString(LABELS[i], lx + 20, iy + fm.getAscent() - 1);
      }
    }

    private void drawStackedAreas(Graphics2D g, int px, int py, int pw, int ph) {
      int pts  = FRACS[0].length;
      int[] base = new int[pts];
      Arrays.fill(base, py + ph);

      for (int s = FRACS.length - 1; s >= 0; s--) {
        int[] topX = new int[pts];
        int[] topY = new int[pts];
        int[] botX = new int[pts];
        int[] botY = new int[pts];

        for (int i = 0; i < pts; i++) {
          topX[i] = px + Math.round((float) pw * i / (pts - 1));

          float spike = (float)Math.sin(i * 0.9) * 0.015f;
          float value = FRACS[s][i] + spike;
          topY[i] = base[i] - Math.round(value * ph);

          botX[i] = topX[i];
          botY[i] = base[i];
        }

        Path2D poly = new Path2D.Double();
        poly.moveTo(botX[0], botY[0]);
        for (int i = 0; i < pts; i++) poly.lineTo(topX[i], topY[i]);
        for (int i = pts - 1; i >= 0; i--) poly.lineTo(botX[i], botY[i]);
        poly.closePath();

        g.setColor(withAlpha(COLORS[s], 160));
        g.fill(poly);

        g.setColor(COLORS[s]);
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D edge = new Path2D.Double();
        edge.moveTo(topX[0], topY[0]);
        for (int i = 1; i < pts; i++) edge.lineTo(topX[i], topY[i]);
        g.draw(edge);

        System.arraycopy(topY, 0, base, 0, pts);
      }
    }

    private void drawYAxis(Graphics2D g, int ax, int py, int aw, int ph) {
      g.setColor(borderColor());
      g.setStroke(new BasicStroke(0.8f));
      g.drawLine(ax + aw, py, ax + aw, py + ph);

      g.setFont(getFont().deriveFont(Font.PLAIN, 8f));
      FontMetrics fm = g.getFontMetrics();
      String[] lbls = {"0", "25", "50", "75"};

      for (int i = 0; i < lbls.length; i++) {
        int ty = py + ph - Math.round((float) ph * i / (lbls.length - 1));

        int textY;
        if (i == 0) {
          textY = ty - 2;
        } else if (i == lbls.length - 1) {
          textY = ty + fm.getAscent();
        } else {
          textY = ty + fm.getAscent() / 2 - 1;
        }

        g.setColor(withAlpha(borderColor(), 180));
        g.setStroke(new BasicStroke(0.7f));
        g.drawLine(ax + aw - 3, ty, ax + aw, ty);
        g.setColor(secondaryText());
        int sw = fm.stringWidth(lbls[i]);
        g.drawString(lbls[i], ax + aw - 4 - sw, textY);
      }
    }

    private void drawXAxis(Graphics2D g, int px, int ay, int pw, int ah) {
      g.setColor(borderColor());
      g.setStroke(new BasicStroke(0.8f));
      g.drawLine(px, ay, px + pw, ay);

      g.setFont(getFont().deriveFont(Font.PLAIN, 8f));
      FontMetrics fm = g.getFontMetrics();
      String[] labels = {"10:00", "11:00", "12:00", "13:00"};

      for (int i = 0; i < labels.length; i++) {
        int tx = px + Math.round((float) pw * i / (labels.length - 1));
        int sw = fm.stringWidth(labels[i]);

        int textX;
        if (i == 0) {
          textX = tx;
        } else if (i == labels.length - 1) {
          textX = tx - sw;
        } else {
          textX = tx - sw / 2;
        }

        g.setColor(withAlpha(borderColor(), 180));
        g.setStroke(new BasicStroke(0.7f));
        g.drawLine(tx, ay, tx, ay + 3);
        g.setColor(secondaryText());
        g.drawString(labels[i], textX, ay + fm.getAscent() + 2);
      }
    }

    private void drawGrid(Graphics2D g, int px, int py, int pw, int ph) {
      g.setColor(withAlpha(borderColor(), 80));
      g.setStroke(new BasicStroke(0.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                                  0, new float[]{3f, 3f}, 0f));
      for (int i = 1; i <= 3; i++) {
        int gy = py + Math.round((float) ph * i / 4);
        g.drawLine(px, gy, px + pw, gy);
      }
    }

    private Color plotBg() {
      Color bg = getBackground();
      return isDark(bg) ? mix(bg, Color.WHITE, 0.08f) : mix(bg, Color.WHITE, 0.75f);
    }

    private Color cardColor() {
      Color bg = getBackground();
      return isDark(bg) ? mix(bg, Color.WHITE, 0.05f) : mix(bg, Color.WHITE, 0.38f);
    }

    private Color chartBoxColor() {
      Color bg = getBackground();
      return isDark(bg) ? mix(bg, Color.WHITE, 0.10f) : mix(bg, Color.WHITE, 0.70f);
    }

    private Color borderColor() {
      Color bg = getBackground();
      return isDark(bg) ? mix(bg, Color.WHITE, 0.22f) : mix(bg, Color.BLACK, 0.14f);
    }

    private Color primaryText() {
      return isDark(getBackground()) ? new Color(235, 240, 245) : new Color(55, 60, 66);
    }

    private Color secondaryText() {
      return isDark(getBackground()) ? new Color(170, 178, 188) : new Color(110, 118, 128);
    }

    private static Color withAlpha(Color c, int alpha) {
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static boolean isDark(Color c) {
      return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue() < 140;
    }

    private static Color mix(Color a, Color b, float r) {
      r = Math.max(0f, Math.min(1f, r));
      return new Color(
          (int)(a.getRed()   * (1 - r) + b.getRed()   * r),
          (int)(a.getGreen() * (1 - r) + b.getGreen() * r),
          (int)(a.getBlue()  * (1 - r) + b.getBlue()  * r));
    }
  }
}