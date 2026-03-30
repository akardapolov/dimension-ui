package ru.dimension.ui.component.panel;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.JPanel;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.function.NormFunction;
import ru.dimension.ui.model.function.TimeRangeFunction;

public class FunctionPreviewPanel extends JPanel {
  private enum PreviewType { IDLE, TIME_RANGE, NORM }

  private static final double[] TIME_RANGE_SAMPLE = {
      8, 12, 10, 14, 11, 17, 15, 13, 18, 16, 14, 12,
      19, 23, 20, 18, 17, 15, 13, 14, 12, 10, 9, 11
  };

  private static final double[] NORM_SAMPLE = {
      1200, 1800, 900, 2200, 1600, 2400, 1750
  };

  private PreviewType previewType = PreviewType.IDLE;

  private TimeRangeFunction currentTimeRangeFunction;
  private TimeRangeFunction timeRangeFunction; // hovered

  private NormFunction currentNormFunction;
  private NormFunction normFunction; // hovered

  public FunctionPreviewPanel() {
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);
    setOpaque(true);
    setPreferredSize(new Dimension(360, 128));
    setMinimumSize(new Dimension(220, 128));
    setToolTipText("Наведите курсор на вариант настройки ниже");
  }

  public void showTimeRangePreview(TimeRangeFunction current, TimeRangeFunction hovered) {
    this.currentTimeRangeFunction = current;
    this.timeRangeFunction = hovered;
    this.currentNormFunction = null;
    this.normFunction = null;
    this.previewType = hovered == null ? PreviewType.IDLE : PreviewType.TIME_RANGE;
    repaint();
  }

  public void showNormPreview(NormFunction current, NormFunction hovered) {
    this.currentNormFunction = current;
    this.normFunction = hovered;
    this.currentTimeRangeFunction = null;
    this.timeRangeFunction = null;
    this.previewType = hovered == null ? PreviewType.IDLE : PreviewType.NORM;
    repaint();
  }

  public void clearPreview() {
    this.currentTimeRangeFunction = null;
    this.timeRangeFunction = null;
    this.currentNormFunction = null;
    this.normFunction = null;
    this.previewType = PreviewType.IDLE;
    setToolTipText("Наведите курсор на вариант настройки ниже");
    repaint();
  }

  public void setText(String text) {
    setToolTipText(text);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (getWidth() <= 0 || getHeight() <= 0) {
      return;
    }

    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    Rectangle card = new Rectangle(6, 6, getWidth() - 12, getHeight() - 12);
    paintCard(g2, card);

    switch (previewType) {
      case TIME_RANGE -> paintTimeRangePreview(g2, card);
      case NORM -> paintNormPreview(g2, card);
      case IDLE -> paintIdlePreview(g2, card);
    }

    g2.dispose();
  }

  private void paintIdlePreview(Graphics2D g2, Rectangle card) {
    Color text = getPrimaryTextColor();
    Color subText = getSecondaryTextColor();
    Color accent = new Color(76, 132, 255);

    drawHeader(g2, card, "Preview", "Hover an option below");

    Rectangle box = new Rectangle(card.x + 10, card.y + 28, card.width - 20, card.height - 38);
    Rectangle plot = drawChartBox(g2, box, "Chart change preview", null);

    drawGrid(g2, plot);
    drawLineSeries(g2, plot, TIME_RANGE_SAMPLE, getRawSeriesColor(), withAlpha(accent, 28), 2.0f, true);
    drawCenterMessage(g2, plot, "Time range / Normalization preview", text, subText);
  }

  private void paintTimeRangePreview(Graphics2D g2, Rectangle card) {
    Color accent = new Color(76, 132, 255);

    drawHeader(g2, card, "Time range preview", timeRangeFunction.getName());

    Rectangle[] layout = buildTwoChartsLayout(card);

    // Draw Current selected time range
    drawTimeRangeChart(g2, layout[0], "Current", currentTimeRangeFunction, getRawSeriesColor(), true, accent);

    // Draw Hovered time range
    drawTimeRangeChart(g2, layout[2], "Will be", timeRangeFunction, accent, false, accent);

    boolean isAuto = TimeRangeFunction.AUTO.equals(timeRangeFunction);
    drawArrowArea(g2, layout[1], isAuto ? "auto" : "group by " + getShortTimeRangeLabel(timeRangeFunction), accent);
  }

  private void drawTimeRangeChart(Graphics2D g2, Rectangle box, String title, TimeRangeFunction trFunc, Color seriesColor, boolean isCurrent, Color accent) {
    boolean isAuto = TimeRangeFunction.AUTO.equals(trFunc);
    String meta = isAuto ? TIME_RANGE_SAMPLE.length + " points" : getBucketCount(trFunc) + " buckets";
    Rectangle plot = drawChartBox(g2, box, title, meta);

    drawGrid(g2, plot);

    if (isAuto) {
      Color fillColor = isCurrent ? withAlpha(seriesColor, 20) : withAlpha(accent, 20);
      drawLineSeries(g2, plot, TIME_RANGE_SAMPLE, seriesColor, fillColor, 1.8f, true);
    } else {
      int bucketCount = getBucketCount(trFunc);
      double[] grouped = aggregate(TIME_RANGE_SAMPLE, bucketCount);

      if (!isCurrent) {
        drawBucketBands(g2, plot, bucketCount, accent);
      }
      drawLineSeries(g2, plot, TIME_RANGE_SAMPLE, withAlpha(getRawSeriesColor(), 90), null, 1.0f, false);
      drawStepSeries(g2, plot, grouped, seriesColor);
    }
  }

  private void paintNormPreview(Graphics2D g2, Rectangle card) {
    Color accent = new Color(55, 176, 106);

    drawHeader(g2, card, "Normalization preview", normFunction.getName());

    Rectangle[] layout = buildTwoChartsLayout(card);

    // Factors scale height from Second(0.2) up to None(1.0)
    double currentFactor = getNormPreviewFactor(currentNormFunction);
    double hoverFactor = getNormPreviewFactor(normFunction);

    double[] currentNormalized = Arrays.stream(NORM_SAMPLE).map(v -> v * currentFactor).toArray();
    double[] hoverNormalized = Arrays.stream(NORM_SAMPLE).map(v -> v * hoverFactor).toArray();

    // Use absolute global max based on the original unscaled values (None = max height)
    double commonMax = maxOf(NORM_SAMPLE) * 1.15d;

    // Draw Current selected normalization
    Rectangle beforePlot = drawChartBox(g2, layout[0], "Current", getNormUnitLabel(currentNormFunction));
    drawGrid(g2, beforePlot);
    drawBarChart(g2, beforePlot, currentNormalized, getRawSeriesColor(), "max " + formatValue(maxOf(currentNormalized)), commonMax);

    // Draw Hovered normalization
    Rectangle afterPlot = drawChartBox(g2, layout[2], "Will be", getNormUnitLabel(normFunction));
    drawGrid(g2, afterPlot);
    drawBarChart(g2, afterPlot, hoverNormalized, accent, "max " + formatValue(maxOf(hoverNormalized)), commonMax);

    drawArrowArea(g2, layout[1], getNormOperationLabel(normFunction), accent);
  }

  private Rectangle[] buildTwoChartsLayout(Rectangle card) {
    int contentX = card.x + 10;
    int contentY = card.y + 28;
    int contentW = card.width - 20;
    int contentH = card.height - 38;

    int arrowW = 54;
    int gap = 10;
    int boxW = (contentW - arrowW - gap * 2) / 2;

    Rectangle before = new Rectangle(contentX, contentY, boxW, contentH);
    Rectangle arrow = new Rectangle(contentX + boxW + gap, contentY, arrowW, contentH);
    Rectangle after = new Rectangle(contentX + boxW + gap + arrowW + gap, contentY, boxW, contentH);

    return new Rectangle[]{before, arrow, after};
  }

  private void drawHeader(Graphics2D g2, Rectangle card, String title, String meta) {
    Color text = getPrimaryTextColor();
    Color subText = getSecondaryTextColor();

    g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
    g2.setColor(text);
    g2.drawString(title, card.x + 12, card.y + 17);

    if (meta != null && !meta.isBlank()) {
      g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
      FontMetrics fm = g2.getFontMetrics();
      int x = card.x + card.width - 12 - fm.stringWidth(meta);
      g2.setColor(subText);
      g2.drawString(meta, x, card.y + 17);
    }
  }

  private Rectangle drawChartBox(Graphics2D g2, Rectangle box, String title, String meta) {
    Color boxBg = getChartBoxColor();
    Color border = getBorderColor();
    Color text = getPrimaryTextColor();
    Color subText = getSecondaryTextColor();

    g2.setColor(boxBg);
    g2.fillRoundRect(box.x, box.y, box.width, box.height, 14, 14);

    g2.setColor(border);
    g2.drawRoundRect(box.x, box.y, box.width, box.height, 14, 14);

    g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
    g2.setColor(text);
    g2.drawString(title, box.x + 8, box.y + 14);

    if (meta != null && !meta.isBlank()) {
      g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
      FontMetrics fm = g2.getFontMetrics();
      g2.setColor(subText);
      g2.drawString(meta, box.x + box.width - 8 - fm.stringWidth(meta), box.y + 14);
    }

    return new Rectangle(box.x + 8, box.y + 20, box.width - 16, box.height - 30);
  }

  private void drawArrowArea(Graphics2D g2, Rectangle area, String label, Color accent) {
    Color subText = getSecondaryTextColor();

    g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
    g2.setColor(subText);

    FontMetrics fm = g2.getFontMetrics();
    int labelX = area.x + (area.width - fm.stringWidth(label)) / 2;
    g2.drawString(label, labelX, area.y + 16);

    int y = area.y + area.height / 2 + 4;
    int x1 = area.x + 8;
    int x2 = area.x + area.width - 10;

    g2.setColor(accent);
    g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.drawLine(x1, y, x2 - 10, y);

    Polygon arrow = new Polygon();
    arrow.addPoint(x2 - 10, y - 6);
    arrow.addPoint(x2, y);
    arrow.addPoint(x2 - 10, y + 6);
    g2.fillPolygon(arrow);
  }

  private void drawGrid(Graphics2D g2, Rectangle plot) {
    Color grid = withAlpha(getBorderColor(), 110);
    g2.setColor(grid);
    g2.setStroke(new BasicStroke(1f));

    for (int i = 0; i <= 3; i++) {
      int y = plot.y + (int) Math.round((plot.height - 1) * i / 3.0);
      g2.drawLine(plot.x, y, plot.x + plot.width, y);
    }
  }

  private void drawLineSeries(Graphics2D g2,
                              Rectangle plot,
                              double[] values,
                              Color lineColor,
                              Color fillColor,
                              float strokeWidth,
                              boolean drawPoints) {
    if (values == null || values.length == 0) {
      return;
    }

    double max = Math.max(1d, maxOf(values));

    Path2D line = new Path2D.Double();
    Path2D fill = new Path2D.Double();

    for (int i = 0; i < values.length; i++) {
      int x = plot.x + (int) Math.round(i * (plot.width - 1d) / Math.max(1, values.length - 1));
      int y = mapY(values[i], max, plot);

      if (i == 0) {
        line.moveTo(x, y);
        fill.moveTo(x, plot.y + plot.height);
        fill.lineTo(x, y);
      } else {
        line.lineTo(x, y);
        fill.lineTo(x, y);
      }
    }

    int lastX = plot.x + plot.width - 1;
    fill.lineTo(lastX, plot.y + plot.height);
    fill.closePath();

    if (fillColor != null) {
      g2.setColor(fillColor);
      g2.fill(fill);
    }

    g2.setColor(lineColor);
    g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(line);

    if (drawPoints) {
      for (int i = 0; i < values.length; i++) {
        int x = plot.x + (int) Math.round(i * (plot.width - 1d) / Math.max(1, values.length - 1));
        int y = mapY(values[i], max, plot);
        g2.fillOval(x - 2, y - 2, 4, 4);
      }
    }
  }

  private void drawStepSeries(Graphics2D g2, Rectangle plot, double[] values, Color color) {
    if (values == null || values.length == 0) {
      return;
    }

    double max = Math.max(1d, maxOf(values));
    double bucketWidth = plot.width / (double) values.length;

    Path2D line = new Path2D.Double();
    Path2D fill = new Path2D.Double();

    int baseY = plot.y + plot.height;

    for (int i = 0; i < values.length; i++) {
      int x1 = plot.x + (int) Math.round(i * bucketWidth);
      int x2 = plot.x + (int) Math.round((i + 1) * bucketWidth);
      int y = mapY(values[i], max, plot);

      if (i == 0) {
        line.moveTo(x1, y);
        fill.moveTo(x1, baseY);
        fill.lineTo(x1, y);
      } else {
        line.lineTo(x1, y);
        fill.lineTo(x1, y);
      }

      line.lineTo(x2, y);
      fill.lineTo(x2, y);
    }

    fill.lineTo(plot.x + plot.width, baseY);
    fill.closePath();

    g2.setColor(withAlpha(color, 55));
    g2.fill(fill);

    g2.setColor(color);
    g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g2.draw(line);
  }

  private void drawBucketBands(Graphics2D g2, Rectangle plot, int bucketCount, Color accent) {
    if (bucketCount <= 0) {
      return;
    }

    double bucketWidth = plot.width / (double) bucketCount;

    for (int i = 0; i < bucketCount; i++) {
      int x = plot.x + (int) Math.round(i * bucketWidth);
      int w = (int) Math.round((i + 1) * bucketWidth) - (int) Math.round(i * bucketWidth);

      if (i % 2 == 0) {
        g2.setColor(withAlpha(accent, 14));
        g2.fillRect(x, plot.y, w, plot.height);
      }

      g2.setColor(withAlpha(accent, 40));
      g2.drawLine(x, plot.y, x, plot.y + plot.height);
    }

    g2.setColor(withAlpha(accent, 40));
    g2.drawLine(plot.x + plot.width, plot.y, plot.x + plot.width, plot.y + plot.height);
  }

  private void drawBarChart(Graphics2D g2, Rectangle plot, double[] values, Color color, String metaText, double scaleMax) {
    if (values == null || values.length == 0) {
      return;
    }

    double max = Math.max(1d, scaleMax);
    int n = values.length;
    int gap = 6;

    // Толщина столбиков рассчитывается всегда одинаково т.к. length массивов всегда равно 7
    int barWidth = Math.max(6, (plot.width - gap * (n + 1)) / n);

    for (int i = 0; i < n; i++) {
      int x = plot.x + gap + i * (barWidth + gap);
      int y = mapY(values[i], max, plot);
      int h = plot.y + plot.height - y;

      g2.setColor(withAlpha(color, 70));
      g2.fillRoundRect(x, y, barWidth, h, 8, 8);

      g2.setColor(color);
      g2.drawRoundRect(x, y, barWidth, h, 8, 8);
    }

    if (metaText != null) {
      g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
      g2.setColor(getSecondaryTextColor());
      g2.drawString(metaText, plot.x + 2, plot.y + 11);
    }
  }

  private void drawCenterMessage(Graphics2D g2, Rectangle plot, String message, Color text, Color subText) {
    g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
    FontMetrics fm = g2.getFontMetrics();
    int x = plot.x + (plot.width - fm.stringWidth(message)) / 2;
    int y = plot.y + plot.height / 2 - 4;
    g2.setColor(text);
    g2.drawString(message, x, y);

    String sub = "Move mouse over options";
    g2.setFont(getFont().deriveFont(Font.PLAIN, 10f));
    fm = g2.getFontMetrics();
    x = plot.x + (plot.width - fm.stringWidth(sub)) / 2;
    g2.setColor(subText);
    g2.drawString(sub, x, y + 16);
  }

  private int mapY(double value, double max, Rectangle plot) {
    double normalized = max <= 0 ? 0 : value / max;
    normalized = Math.max(0d, Math.min(1d, normalized));
    return plot.y + plot.height - (int) Math.round(normalized * (plot.height - 1));
  }

  private double[] aggregate(double[] source, int bucketCount) {
    double[] result = new double[bucketCount];

    for (int i = 0; i < bucketCount; i++) {
      int start = (int) Math.floor(i * source.length / (double) bucketCount);
      int end = (int) Math.floor((i + 1) * source.length / (double) bucketCount);
      if (end <= start) {
        end = Math.min(source.length, start + 1);
      }

      double sum = 0d;
      int count = 0;
      for (int j = start; j < end && j < source.length; j++) {
        sum += source[j];
        count++;
      }

      result[i] = count == 0 ? 0 : sum / count;
    }

    return result;
  }

  private int getBucketCount(TimeRangeFunction function) {
    return switch (function) {
      case AUTO -> TIME_RANGE_SAMPLE.length;
      case MINUTE -> 18;
      case HOUR -> 10;
      case DAY -> 6;
      case MONTH -> 3;
    };
  }

  private String getShortTimeRangeLabel(TimeRangeFunction function) {
    return switch (function) {
      case AUTO -> "auto";
      case MINUTE -> "1m";
      case HOUR -> "1h";
      case DAY -> "1d";
      case MONTH -> "1M";
    };
  }

  private double getNormPreviewFactor(NormFunction function) {
    // Влияет на высоту отображения: Second - самые маленькие графики, увеличивается до None
    return switch (function) {
      case NONE -> 1.0d;
      case DAY -> 0.8d;
      case HOUR -> 0.6d;
      case MINUTE -> 0.4d;
      case SECOND -> 0.2d;
    };
  }

  private String getNormOperationLabel(NormFunction function) {
    return switch (function) {
      case NONE -> "as is";
      case SECOND -> "to second";
      case MINUTE -> "to minute";
      case HOUR -> "to hour";
      case DAY -> "to day";
    };
  }

  private String getNormUnitLabel(NormFunction function) {
    return switch (function) {
      case NONE -> "no normalization";
      case SECOND -> "per second";
      case MINUTE -> "per minute";
      case HOUR -> "per hour";
      case DAY -> "per day";
    };
  }

  private double maxOf(double[] values) {
    return Arrays.stream(values).max().orElse(1d);
  }

  private void paintCard(Graphics2D g2, Rectangle card) {
    g2.setColor(getCardColor());
    g2.fillRoundRect(card.x, card.y, card.width, card.height, 16, 16);

    g2.setColor(getBorderColor());
    g2.drawRoundRect(card.x, card.y, card.width, card.height, 16, 16);
  }

  private Color getCardColor() {
    Color bg = getBackground();
    return isDark(bg) ? mix(bg, Color.WHITE, 0.05f) : mix(bg, Color.WHITE, 0.38f);
  }

  private Color getChartBoxColor() {
    Color bg = getBackground();
    return isDark(bg) ? mix(bg, Color.WHITE, 0.10f) : mix(bg, Color.WHITE, 0.70f);
  }

  private Color getBorderColor() {
    Color bg = getBackground();
    return isDark(bg) ? mix(bg, Color.WHITE, 0.22f) : mix(bg, Color.BLACK, 0.14f);
  }

  private Color getPrimaryTextColor() {
    return isDark(getBackground()) ? new Color(235, 240, 245) : new Color(55, 60, 66);
  }

  private Color getSecondaryTextColor() {
    return isDark(getBackground()) ? new Color(170, 178, 188) : new Color(110, 118, 128);
  }

  private Color getRawSeriesColor() {
    return isDark(getBackground())
        ? new Color(186, 194, 206, 180)
        : new Color(128, 136, 148, 180);
  }

  private static Color withAlpha(Color color, int alpha) {
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
  }

  private static boolean isDark(Color color) {
    double luminance = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
    return luminance < 140;
  }

  private static Color mix(Color a, Color b, float ratio) {
    float r = Math.max(0f, Math.min(1f, ratio));
    int red = (int) (a.getRed() * (1 - r) + b.getRed() * r);
    int green = (int) (a.getGreen() * (1 - r) + b.getGreen() * r);
    int blue = (int) (a.getBlue() * (1 - r) + b.getBlue() * r);
    return new Color(red, green, blue);
  }

  private static String formatValue(double value) {
    if (value >= 100) {
      return String.format(Locale.US, "%.0f", value);
    }
    if (value >= 10) {
      return String.format(Locale.US, "%.1f", value);
    }
    return String.format(Locale.US, "%.2f", value);
  }
}