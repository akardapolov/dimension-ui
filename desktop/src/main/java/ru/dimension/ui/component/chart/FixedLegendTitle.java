package ru.dimension.ui.component.chart;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.FlowArrangement;
import org.jfree.chart.block.LengthConstraintType;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.Size2D;
import org.jfree.data.Range;

public class FixedLegendTitle extends LegendTitle {

  static final double ITEM_GAP = 1.0;

  private static final double SCROLLBAR_WIDTH = 4.0;
  private static final double SCROLLBAR_MARGIN = 2.0;
  private static final double SCROLLBAR_MIN_THUMB_HEIGHT = 12.0;
  private static final Color SCROLLBAR_TRACK_COLOR = new Color(128, 128, 128, 40);
  private static final Color SCROLLBAR_THUMB_COLOR = new Color(90, 90, 90, 170);

  private final NoWrapColumnArrangement columnArrangement;

  private volatile boolean fixedWidthEnabled = false;
  private volatile double fixedItemWidth = 150;

  private volatile Rectangle2D lastDrawnArea;
  private volatile double measuredItemHeight = -1.0;
  private volatile float measuredForFontSize = -1f;
  private volatile double lastHeightConstraintHint = -1.0;

  private volatile int scrollOffset = 0;
  private volatile int scrollVisibleCount = 0;
  private volatile int scrollTotalCount = 0;

  public FixedLegendTitle(LegendItemSource source) {
    this(source, new NoWrapColumnArrangement());
  }

  public FixedLegendTitle(LegendItemSource source,
                          NoWrapColumnArrangement vLayout) {
    super(source, new FlowArrangement(), vLayout);
    this.columnArrangement = vLayout;
  }

  public void setNoWrap(boolean noWrap) {
    columnArrangement.setNoWrap(noWrap);
  }

  public void setFixedWidthEnabled(boolean enabled) {
    this.fixedWidthEnabled = enabled;
  }

  public void setFixedItemWidth(double width) {
    this.fixedItemWidth = width;
  }

  public void setScrollState(int offset, int visibleCount, int totalCount) {
    this.scrollOffset = offset;
    this.scrollVisibleCount = visibleCount;
    this.scrollTotalCount = totalCount;
  }

  @Override
  protected Block createLegendItemBlock(LegendItem item) {
    Block original = super.createLegendItemBlock(item);
    if (!fixedWidthEnabled) {
      return original;
    }
    return new FixedWidthBlock(original, fixedItemWidth);
  }

  @Override
  public Size2D arrange(Graphics2D g2,
                        RectangleConstraint constraint) {
    this.lastHeightConstraintHint = extractHeightBound(constraint);
    ensureItemHeightMeasured(g2);

    return super.arrange(g2, constraint);
  }

  private void ensureItemHeightMeasured(Graphics2D g2) {
    Font font = getItemFont();
    if (measuredItemHeight > 0 && measuredForFontSize == font.getSize2D()) {
      return;
    }

    LegendItem plainSample = new LegendItem("Sample", Color.GRAY);
    plainSample.setLabelFont(font);
    double plainHeight = createLegendItemBlock(plainSample)
        .arrange(g2, RectangleConstraint.NONE).height;

    LegendItem boldSample = new LegendItem("Sample", Color.GRAY);
    boldSample.setLabelFont(font.deriveFont(Font.BOLD));
    double boldHeight = createLegendItemBlock(boldSample)
        .arrange(g2, RectangleConstraint.NONE).height;

    this.measuredItemHeight = Math.max(plainHeight, boldHeight) + ITEM_GAP;
    this.measuredForFontSize = font.getSize2D();
  }

  @Override
  public Object draw(Graphics2D g2,
                     Rectangle2D area,
                     Object params) {
    this.lastDrawnArea = (Rectangle2D) area.clone();
    Object result = super.draw(g2, area, params);
    drawScrollbarIfNeeded(g2, area);
    return result;
  }

  private void drawScrollbarIfNeeded(Graphics2D g2, Rectangle2D area) {
    if (scrollTotalCount <= scrollVisibleCount || scrollVisibleCount <= 0) {
      return;
    }

    double trackHeight = area.getHeight() - 2 * SCROLLBAR_MARGIN;
    if (trackHeight <= 0) {
      return;
    }

    double trackX = area.getMaxX() - SCROLLBAR_WIDTH - SCROLLBAR_MARGIN;
    double trackY = area.getY() + SCROLLBAR_MARGIN;

    double thumbHeight = Math.max(SCROLLBAR_MIN_THUMB_HEIGHT,
                                  trackHeight * scrollVisibleCount / (double) scrollTotalCount);
    thumbHeight = Math.min(thumbHeight, trackHeight);

    int maxOffset = Math.max(1, scrollTotalCount - scrollVisibleCount);
    double thumbY = trackY + (trackHeight - thumbHeight) * scrollOffset / maxOffset;

    Color originalColor = g2.getColor();
    try {
      g2.setColor(SCROLLBAR_TRACK_COLOR);
      g2.fill(new Rectangle2D.Double(trackX, trackY, SCROLLBAR_WIDTH, trackHeight));

      g2.setColor(SCROLLBAR_THUMB_COLOR);
      g2.fill(new Rectangle2D.Double(trackX, thumbY, SCROLLBAR_WIDTH, thumbHeight));
    } finally {
      g2.setColor(originalColor);
    }
  }

  public Rectangle2D getLastDrawnArea() {
    return lastDrawnArea;
  }

  public double getMeasuredItemHeight() {
    return measuredItemHeight;
  }

  public double getHeightConstraintHint() {
    return lastHeightConstraintHint;
  }

  private static double extractHeightBound(RectangleConstraint constraint) {
    if (constraint == null) {
      return -1;
    }
    try {
      LengthConstraintType type = constraint.getHeightConstraintType();
      if (type == LengthConstraintType.FIXED) {
        return constraint.getHeight();
      }
      if (type == LengthConstraintType.RANGE) {
        Range range = constraint.getHeightRange();
        if (range != null) {
          return range.getUpperBound();
        }
      }
    } catch (Exception e) {
    }
    return -1;
  }
}