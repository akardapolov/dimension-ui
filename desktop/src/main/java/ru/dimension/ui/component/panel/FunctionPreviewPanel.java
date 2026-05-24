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
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.JPanel;
import javax.swing.Timer;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.function.NormFunction;
import ru.dimension.ui.model.function.PercentileFunction;
import ru.dimension.ui.model.function.TimeRangeFunction;

public class FunctionPreviewPanel extends JPanel {
  private enum PreviewType { IDLE, TIME_RANGE, NORM, PERCENTILE }

  private static final double[] TIME_RANGE_SAMPLE = {
      8, 12, 10, 14, 11, 17, 15, 13, 18, 16, 14, 12,
      19, 23, 20, 18, 17, 15, 13, 14, 12, 10, 9, 11
  };

  private static final double[] NORM_SAMPLE = {
      1200, 1800, 900, 2200, 1600, 2400, 1750
  };

  private static final double[] PERCENTILE_SAMPLE = {
      8, 9, 10, 10, 11, 11, 12, 13, 13, 14,
      14, 15, 15, 16, 17, 18, 20, 23, 31, 48
  };

  private PreviewType previewType = PreviewType.IDLE;

  private TimeRangeFunction currentTimeRangeFunction;
  private TimeRangeFunction timeRangeFunction;

  private NormFunction currentNormFunction;
  private NormFunction normFunction;

  private PercentileFunction currentPercentileFunction;
  private PercentileFunction percentileFunction;

  private float percentileLineProgress = 0f;
  private float percentileZoneAlpha   = 0f;
  private float percentileGaugeProgress = 0f;

  private float percentileLineFrom  = 0f;
  private float percentileLineTarget = 0f;

  private Timer animationTimer;
  private long  animationStartTime;
  private static final int ANIM_DURATION_MS = 350;

  public FunctionPreviewPanel() {
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);
    setOpaque(true);
    setPreferredSize(new Dimension(360, 128));
    setMinimumSize(new Dimension(220, 128));
    setToolTipText("Наведите курсор на вариант настройки ниже");
  }

  public void showTimeRangePreview(TimeRangeFunction current, TimeRangeFunction hovered) {
    stopAnimation();
    this.currentTimeRangeFunction = current;
    this.timeRangeFunction = hovered;
    this.currentNormFunction = null;
    this.normFunction = null;
    this.currentPercentileFunction = null;
    this.percentileFunction = null;
    this.previewType = hovered == null ? PreviewType.IDLE : PreviewType.TIME_RANGE;
    repaint();
  }

  public void showNormPreview(NormFunction current, NormFunction hovered) {
    stopAnimation();
    this.currentNormFunction = current;
    this.normFunction = hovered;
    this.currentTimeRangeFunction = null;
    this.timeRangeFunction = null;
    this.currentPercentileFunction = null;
    this.percentileFunction = null;
    this.previewType = hovered == null ? PreviewType.IDLE : PreviewType.NORM;
    repaint();
  }

  public void showPercentilePreview(PercentileFunction current, PercentileFunction hovered) {
    this.currentTimeRangeFunction = null;
    this.timeRangeFunction = null;
    this.currentNormFunction = null;
    this.normFunction = null;
    this.currentPercentileFunction = current;

    if (hovered == null) {
      this.percentileFunction = null;
      this.previewType = PreviewType.IDLE;
      stopAnimation();
      repaint();
      return;
    }

    float newTarget = (float) getPercentileRatio(hovered);

    if (this.percentileFunction == null || previewType != PreviewType.PERCENTILE) {
      percentileLineFrom     = 0f;
      percentileLineProgress = 0f;
      percentileZoneAlpha    = 0f;
      percentileGaugeProgress = 0f;
    } else {
      percentileLineFrom = percentileLineProgress;
    }

    percentileLineTarget   = newTarget;
    this.percentileFunction = hovered;
    this.previewType        = PreviewType.PERCENTILE;

    startAnimation();
  }

  public void clearPreview() {
    stopAnimation();
    this.currentTimeRangeFunction = null;
    this.timeRangeFunction = null;
    this.currentNormFunction = null;
    this.normFunction = null;
    this.currentPercentileFunction = null;
    this.percentileFunction = null;
    this.previewType = PreviewType.IDLE;
    setToolTipText("Наведите курсор на вариант настройки ниже");
    repaint();
  }

  public void setText(String text) {
    setToolTipText(text);
  }

  private void startAnimation() {
    stopAnimation();
    animationStartTime = System.currentTimeMillis();
    animationTimer = new Timer(16, e -> {
      long elapsed = System.currentTimeMillis() - animationStartTime;
      float t = Math.min(1f, (float) elapsed / ANIM_DURATION_MS);
      float eased = easeOutCubic(t);

      percentileLineProgress  = percentileLineFrom + (percentileLineTarget - percentileLineFrom) * eased;
      percentileZoneAlpha     = eased;
      percentileGaugeProgress = eased * percentileLineTarget;

      repaint();

      if (t >= 1f) {
        stopAnimation();
      }
    });
    animationTimer.start();
  }

  private void stopAnimation() {
    if (animationTimer != null && animationTimer.isRunning()) {
      animationTimer.stop();
    }
  }

  private float easeOutCubic(float t) {
    float f = 1f - t;
    return 1f - f * f * f;
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
      case TIME_RANGE  -> paintTimeRangePreview(g2, card);
      case NORM        -> paintNormPreview(g2, card);
      case PERCENTILE  -> paintPercentilePreview(g2, card);
      case IDLE        -> paintIdlePreview(g2, card);
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

    drawTimeRangeChart(g2, layout[0], "Current", currentTimeRangeFunction, getRawSeriesColor(), true, accent);
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

    double currentFactor = getNormPreviewFactor(currentNormFunction);
    double hoverFactor   = getNormPreviewFactor(normFunction);

    double[] currentNormalized = Arrays.stream(NORM_SAMPLE).map(v -> v * currentFactor).toArray();
    double[] hoverNormalized   = Arrays.stream(NORM_SAMPLE).map(v -> v * hoverFactor).toArray();

    double commonMax = maxOf(NORM_SAMPLE) * 1.15d;

    Rectangle beforePlot = drawChartBox(g2, layout[0], "Current", getNormUnitLabel(currentNormFunction));
    drawGrid(g2, beforePlot);
    drawBarChart(g2, beforePlot, currentNormalized, getRawSeriesColor(), "max " + formatValue(maxOf(currentNormalized)), commonMax);

    Rectangle afterPlot = drawChartBox(g2, layout[2], "Will be", getNormUnitLabel(normFunction));
    drawGrid(g2, afterPlot);
    drawBarChart(g2, afterPlot, hoverNormalized, accent, "max " + formatValue(maxOf(hoverNormalized)), commonMax);

    drawArrowArea(g2, layout[1], getNormOperationLabel(normFunction), accent);
  }

  private void paintPercentilePreview(Graphics2D g2, Rectangle card) {
    Color accent = new Color(255, 160, 50);

    String meta = percentileFunction == PercentileFunction.NONE
        ? "disabled"
        : percentileFunction.getName();
    drawHeader(g2, card, "Percentile preview", meta);

    int gaugeW   = 38;
    int gapRight = 8;
    int contentX = card.x + 10;
    int contentY = card.y + 28;
    int contentW = card.width - 20;
    int contentH = card.height - 38;

    Rectangle chartBox = new Rectangle(contentX, contentY, contentW - gaugeW - gapRight, contentH);
    Rectangle gaugeBox = new Rectangle(contentX + contentW - gaugeW, contentY, gaugeW, contentH);

    Rectangle plot = drawChartBox(g2, chartBox, "Distribution", null);

    drawGrid(g2, plot);
    drawLineSeries(g2, plot, PERCENTILE_SAMPLE, getRawSeriesColor(), withAlpha(getRawSeriesColor(), 18), 1.6f, true);

    if (percentileFunction != PercentileFunction.NONE && percentileLineProgress > 0f) {
      double pValue  = getPercentileValue(PERCENTILE_SAMPLE, percentileFunction);
      double dataMax = Math.max(1d, maxOf(PERCENTILE_SAMPLE));
      int lineY = mapY(pValue, dataMax, plot);

      int zoneAlphaInt = Math.round(percentileZoneAlpha * 35f);
      g2.setColor(withAlpha(accent, zoneAlphaInt));
      g2.fillRect(plot.x, plot.y, plot.width, lineY - plot.y);

      int dashedAlpha = Math.round(percentileZoneAlpha * 220f);
      g2.setColor(withAlpha(accent, dashedAlpha));
      float[] dash = {5f, 4f};
      g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
      g2.drawLine(plot.x, lineY, plot.x + plot.width, lineY);

      g2.setStroke(new BasicStroke(1f));
      g2.setFont(getFont().deriveFont(Font.BOLD, 9f));
      g2.setColor(withAlpha(accent, dashedAlpha));
      String pLabel = percentileFunction.getName();
      g2.drawString(pLabel, plot.x + 3, lineY - 3);

      int countBelow = countBelowPercentile(PERCENTILE_SAMPLE, percentileFunction);
      int total = PERCENTILE_SAMPLE.length;
      String pctText = countBelow + "/" + total + " below";
      g2.setFont(getFont().deriveFont(Font.PLAIN, 9f));
      FontMetrics fm = g2.getFontMetrics();
      g2.drawString(pctText, plot.x + plot.width - fm.stringWidth(pctText) - 2, lineY - 3);
    }

    drawGauge(g2, gaugeBox, accent);
  }

  private void drawGauge(Graphics2D g2, Rectangle box, Color accent) {
    Color boxBg  = getChartBoxColor();
    Color border = getBorderColor();

    g2.setColor(boxBg);
    g2.fill(new RoundRectangle2D.Float(box.x, box.y, box.width, box.height, 10, 10));
    g2.setColor(border);
    g2.draw(new RoundRectangle2D.Float(box.x, box.y, box.width, box.height, 10, 10));

    int padX = 10;
    int padT = 8;
    int padB = 8;
    int trackX = box.x + padX;
    int trackW = 10;
    int trackY = box.y + padT;
    int trackH = box.height - padT - padB;

    g2.setColor(withAlpha(border, 160));
    g2.fill(new RoundRectangle2D.Float(trackX, trackY, trackW, trackH, 6, 6));

    if (percentileGaugeProgress > 0f) {
      int fillH = Math.round(percentileGaugeProgress * trackH);
      int fillY = trackY + trackH - fillH;
      g2.setColor(withAlpha(accent, 200));
      g2.fill(new RoundRectangle2D.Float(trackX, fillY, trackW, fillH, 6, 6));

      g2.setColor(accent);
      g2.fill(new RoundRectangle2D.Float(trackX, fillY, trackW, Math.min(6, fillH), 6, 6));
    }

    PercentileFunction[] marks = {
        PercentileFunction.P50,
        PercentileFunction.P90,
        PercentileFunction.P95,
        PercentileFunction.P99
    };

    g2.setFont(getFont().deriveFont(Font.PLAIN, 8f));
    FontMetrics fm = g2.getFontMetrics();

    for (PercentileFunction mark : marks) {
      double ratio = getPercentileRatio(mark);
      int markY = trackY + trackH - (int) Math.round(ratio * trackH);

      boolean isActive = percentileFunction == mark;
      Color markColor = isActive
          ? accent
          : withAlpha(getSecondaryTextColor(), 140);

      g2.setColor(markColor);
      g2.setStroke(new BasicStroke(isActive ? 1.4f : 0.8f));
      g2.drawLine(trackX + trackW, markY, trackX + trackW + 5, markY);

      String lbl = mark.getName();
      g2.setColor(markColor);
      g2.drawString(lbl, trackX + trackW + 7, markY + fm.getAscent() / 2);
    }

    g2.setFont(getFont().deriveFont(Font.PLAIN, 7f));
    fm = g2.getFontMetrics();
    g2.setColor(withAlpha(getSecondaryTextColor(), 120));

    String topLbl = "max";
    g2.drawString(topLbl, trackX + trackW / 2 - fm.stringWidth(topLbl) / 2, trackY - 1);

    String botLbl = "min";
    g2.drawString(botLbl, trackX + trackW / 2 - fm.stringWidth(botLbl) / 2, trackY + trackH + fm.getAscent() + 1);
  }

  private double getPercentileRatio(PercentileFunction function) {
    return switch (function) {
      case NONE -> 0.0;
      case P50  -> 0.50;
      case P90  -> 0.90;
      case P95  -> 0.95;
      case P99  -> 0.99;
    };
  }

  private double getPercentileValue(double[] data, PercentileFunction function) {
    double[] sorted = Arrays.stream(data).sorted().toArray();
    int idx = switch (function) {
      case NONE -> sorted.length - 1;
      case P50  -> (int) Math.floor(sorted.length * 0.50);
      case P90  -> (int) Math.floor(sorted.length * 0.90);
      case P95  -> (int) Math.floor(sorted.length * 0.95);
      case P99  -> (int) Math.floor(sorted.length * 0.99);
    };
    return sorted[Math.min(idx, sorted.length - 1)];
  }

  private int countBelowPercentile(double[] data, PercentileFunction function) {
    double threshold = getPercentileValue(data, function);
    return (int) Arrays.stream(data).filter(v -> v <= threshold).count();
  }

  private Rectangle[] buildTwoChartsLayout(Rectangle card) {
    int contentX = card.x + 10;
    int contentY = card.y + 28;
    int contentW = card.width - 20;
    int contentH = card.height - 38;

    int arrowW = 54;
    int gap    = 10;
    int boxW   = (contentW - arrowW - gap * 2) / 2;

    Rectangle before = new Rectangle(contentX, contentY, boxW, contentH);
    Rectangle arrow  = new Rectangle(contentX + boxW + gap, contentY, arrowW, contentH);
    Rectangle after  = new Rectangle(contentX + boxW + gap + arrowW + gap, contentY, boxW, contentH);

    return new Rectangle[]{before, arrow, after};
  }

  private void drawHeader(Graphics2D g2, Rectangle card, String title, String meta) {
    Color text    = getPrimaryTextColor();
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
    Color boxBg   = getChartBoxColor();
    Color border  = getBorderColor();
    Color text    = getPrimaryTextColor();
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

    int y  = area.y + area.height / 2 + 4;
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
    if (values == null || values.length == 0) return;

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
    if (values == null || values.length == 0) return;

    double max         = Math.max(1d, maxOf(values));
    double bucketWidth = plot.width / (double) values.length;

    Path2D line = new Path2D.Double();
    Path2D fill = new Path2D.Double();
    int baseY = plot.y + plot.height;

    for (int i = 0; i < values.length; i++) {
      int x1 = plot.x + (int) Math.round(i * bucketWidth);
      int x2 = plot.x + (int) Math.round((i + 1) * bucketWidth);
      int y  = mapY(values[i], max, plot);

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
    if (bucketCount <= 0) return;

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
    if (values == null || values.length == 0) return;

    double max = Math.max(1d, scaleMax);
    int n      = values.length;
    int gap    = 6;
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
      int end   = (int) Math.floor((i + 1) * source.length / (double) bucketCount);
      if (end <= start) end = Math.min(source.length, start + 1);

      double sum = 0d;
      int count  = 0;
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
      case AUTO   -> TIME_RANGE_SAMPLE.length;
      case MINUTE -> 18;
      case HOUR   -> 10;
      case DAY    -> 6;
      case MONTH  -> 3;
    };
  }

  private String getShortTimeRangeLabel(TimeRangeFunction function) {
    return switch (function) {
      case AUTO   -> "auto";
      case MINUTE -> "1m";
      case HOUR   -> "1h";
      case DAY    -> "1d";
      case MONTH  -> "1M";
    };
  }

  private double getNormPreviewFactor(NormFunction function) {
    return switch (function) {
      case NONE   -> 1.0d;
      case DAY    -> 0.8d;
      case HOUR   -> 0.6d;
      case MINUTE -> 0.4d;
      case SECOND -> 0.2d;
    };
  }

  private String getNormOperationLabel(NormFunction function) {
    return switch (function) {
      case NONE   -> "as is";
      case SECOND -> "to second";
      case MINUTE -> "to minute";
      case HOUR   -> "to hour";
      case DAY    -> "to day";
    };
  }

  private String getNormUnitLabel(NormFunction function) {
    return switch (function) {
      case NONE   -> "no normalization";
      case SECOND -> "per second";
      case MINUTE -> "per minute";
      case HOUR   -> "per hour";
      case DAY    -> "per day";
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
    return new Color(
        (int) (a.getRed()   * (1 - r) + b.getRed()   * r),
        (int) (a.getGreen() * (1 - r) + b.getGreen() * r),
        (int) (a.getBlue()  * (1 - r) + b.getBlue()  * r)
    );
  }

  private static String formatValue(double value) {
    if (value >= 100) return String.format(Locale.US, "%.0f", value);
    if (value >= 10)  return String.format(Locale.US, "%.1f", value);
    return String.format(Locale.US, "%.2f", value);
  }
}