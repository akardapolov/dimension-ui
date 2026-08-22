package org.jfree.chart.renderer.xy;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.urls.XYURLGenerator;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * A stacked-area renderer whose top/bottom band boundaries are smoothed
 * using <b>monotone cubic Hermite interpolation</b> (Fritsch-Carlson
 * method), performed entirely in Java2D (screen) space at paint time.
 *
 * <p>Why not a natural cubic spline? A natural cubic spline (see
 * {@link XYSplineRenderer}) can <em>overshoot</em> between data points -
 * for noisy/spiky real-time data this produces visible artifacts in a
 * stacked-area fill:
 * <ul>
 *   <li>Self-intersecting loops within a single band (the interpolated
 *       curve dips past a neighbouring point), which under the
 *       non-zero winding rule can punch a thin "hole" through the fill;</li>
 *   <li>Because top/bottom boundaries between adjacent stacked series
 *       share the same control points but were historically computed
 *       independently (once forward, once via a reversed point list),
 *       floating point rounding in the tridiagonal solve produced tiny,
 *       sub-pixel mismatches, visible as hairline seams between colours.</li>
 * </ul>
 * Monotone cubic interpolation never exceeds the local min/max of its
 * two neighbouring points (no overshoot, so no self-intersections), and
 * each interpolated value depends only on local neighbours (no global
 * linear system), so it is both branch- and order-independent. To
 * guarantee bit-identical boundaries between stacked series, the curve
 * for a set of control points is computed exactly once (forward); when
 * the same geometry needs to be traced in the opposite direction (e.g.
 * the bottom edge of a fill, traced right-to-left), the already
 * computed point list is simply reversed - never recomputed.
 *
 * @since custom (jfreechart-fse fork)
 */
public class SmoothedStackedXYAreaRenderer extends StackedXYAreaRenderer3 {

    private static final long serialVersionUID = 2L;

    /** Tolerance (Java2D units) used to detect/skip duplicate x points. */
    private static final double DUPLICATE_X_EPS = 1.0e-6;

    /** Number of interpolated segments inserted between two data points. */
    private int precision;

    /**
     * When {@code true}, each filled run is additionally outlined with a
     * thin stroke using the same paint as the fill. This is a standard,
     * cheap mitigation for the hairline anti-aliasing seams that Java2D
     * can leave between two abutting filled shapes even when their edges
     * share numerically identical coordinates.
     */
    private boolean hideSeams;

    public SmoothedStackedXYAreaRenderer() {
        this(null, null, 8);
    }

    public SmoothedStackedXYAreaRenderer(XYToolTipGenerator toolTipGenerator,
                                         XYURLGenerator urlGenerator) {
        this(toolTipGenerator, urlGenerator, 8);
    }

    public SmoothedStackedXYAreaRenderer(XYToolTipGenerator toolTipGenerator,
                                         XYURLGenerator urlGenerator,
                                         int precision) {
        super(toolTipGenerator, urlGenerator);
        if (precision <= 0) {
            throw new IllegalArgumentException("Requires precision > 0.");
        }
        this.precision = precision;
        this.hideSeams = true;
    }

    public int getPrecision() {
        return this.precision;
    }

    public void setPrecision(int precision) {
        if (precision <= 0) {
            throw new IllegalArgumentException("Requires precision > 0.");
        }
        this.precision = precision;
        fireChangeEvent();
    }

    public boolean isHideSeams() {
        return this.hideSeams;
    }

    public void setHideSeams(boolean hideSeams) {
        this.hideSeams = hideSeams;
        fireChangeEvent();
    }

    /**
     * State that accumulates the Java2D points (and resolved paints) for
     * the series currently being drawn. Cleared at the start of each
     * series pass so it works correctly with zoomed/panned views where
     * only a sub-range of items is visited (see
     * {@code processVisibleItemsOnly}).
     */
    public static class SmoothState extends XYItemRendererState {

        final List<Point2D> topPoints = new ArrayList<Point2D>();
        final List<Point2D> bottomPoints = new ArrayList<Point2D>();
        final List<Paint> itemPaints = new ArrayList<Paint>();
        double lastTransX = Double.NaN;

        public SmoothState(PlotRenderingInfo info) {
            super(info);
        }

        @Override
        public void startSeriesPass(XYDataset dataset, int series,
                                    int firstItem, int lastItem, int pass, int passCount) {
            this.topPoints.clear();
            this.bottomPoints.clear();
            this.itemPaints.clear();
            this.lastTransX = Double.NaN;
            super.startSeriesPass(dataset, series, firstItem, lastItem, pass,
                                  passCount);
        }
    }

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea,
                                          XYPlot plot, XYDataset data, PlotRenderingInfo info) {
        return new SmoothState(info);
    }

    @Override
    public void drawItem(Graphics2D g2,
                         XYItemRendererState state,
                         Rectangle2D dataArea,
                         PlotRenderingInfo info,
                         XYPlot plot,
                         ValueAxis domainAxis,
                         ValueAxis rangeAxis,
                         XYDataset dataset,
                         int series,
                         int item,
                         CrosshairState crosshairState,
                         int pass) {

        if (!getItemVisible(series, item)) {
            return;
        }

        SmoothState s = (SmoothState) state;
        TableXYDataset tdataset = (TableXYDataset) dataset;
        PlotOrientation orientation = plot.getOrientation();

        RectangleEdge domainEdge = plot.getDomainAxisEdge();
        RectangleEdge rangeEdge = plot.getRangeAxisEdge();

        // --- compute this item's stacked top/base values -------------
        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);
        if (Double.isNaN(y1)) {
            y1 = 0.0;
        }
        double[] stack1 = getStackValuesLocal(tdataset, series, item);
        double baseValue = stack1[1];
        double topValue = y1 + stack1[1];

        double transX = domainAxis.valueToJava2D(x1, dataArea, domainEdge);
        if (getRoundXCoordinates()) {
            transX = Math.round(transX);
        }
        double transTop = rangeAxis.valueToJava2D(topValue, dataArea, rangeEdge);
        double transBase = rangeAxis.valueToJava2D(baseValue, dataArea, rangeEdge);

        // --- accumulate points for later smoothed fill ----------------
        boolean duplicate = !Double.isNaN(s.lastTransX)
            && Math.abs(transX - s.lastTransX) < DUPLICATE_X_EPS;
        if (!Double.isNaN(transX) && !Double.isNaN(transTop)
            && !Double.isNaN(transBase) && !duplicate) {
            Point2D topPoint;
            Point2D basePoint;
            if (orientation == PlotOrientation.HORIZONTAL) {
                topPoint = new Point2D.Double(transTop, transX);
                basePoint = new Point2D.Double(transBase, transX);
            }
            else {
                topPoint = new Point2D.Double(transX, transTop);
                basePoint = new Point2D.Double(transX, transBase);
            }
            s.topPoints.add(topPoint);
            s.bottomPoints.add(basePoint);
            s.itemPaints.add(getItemPaint(series, item));
            s.lastTransX = transX;
        }

        // --- entity / tooltip hotspot (unchanged behaviour, per item) --
        EntityCollection entities = null;
        if (info != null) {
            entities = info.getOwner().getEntityCollection();
        }
        if (entities != null) {
            int itemCount = dataset.getItemCount(series);
            double x0 = dataset.getXValue(series, Math.max(item - 1, 0));
            double x2 = dataset.getXValue(series,
                                          Math.min(item + 1, itemCount - 1));
            double xleft = (x0 + x1) / 2.0;
            double xright = (x1 + x2) / 2.0;
            double transXLeft = domainAxis.valueToJava2D(xleft, dataArea, domainEdge);
            double transXRight = domainAxis.valueToJava2D(xright, dataArea, domainEdge);
            if (getRoundXCoordinates()) {
                transXLeft = Math.round(transXLeft);
                transXRight = Math.round(transXRight);
            }

            double top = Math.min(transTop, transBase);
            double height = Math.abs(transTop - transBase);
            Rectangle2D hotspot;
            if (orientation == PlotOrientation.HORIZONTAL) {
                hotspot = new Rectangle2D.Double(top,
                                                 Math.min(transXLeft, transXRight), height,
                                                 Math.abs(transXRight - transXLeft));
            }
            else {
                hotspot = new Rectangle2D.Double(
                    Math.min(transXLeft, transXRight), top,
                    Math.abs(transXRight - transXLeft), height);
            }
            addEntity(entities, hotspot, dataset, series, item, transX, transTop);
        }

        // --- flush: paint the smoothed area for this series ------------
        if (item == s.getLastItemIndex()) {
            paintSmoothedSeries(g2, orientation, s);
        }
    }

    /**
     * Splits the accumulated points into contiguous runs of identical
     * paint and fills each run as a smoothed polygon. Adjacent runs share
     * one boundary point so there is no seam/gap between differently
     * coloured sections (used for selection-region highlighting).
     */
    private void paintSmoothedSeries(Graphics2D g2, PlotOrientation orientation,
                                     SmoothState s) {
        int n = s.topPoints.size();
        if (n < 1) {
            return;
        }
        if (n == 1) {
            return; // a single point cannot form a visible filled area
        }

        int idx = 0;
        while (idx < n) {
            Paint runPaint = s.itemPaints.get(idx);
            int runEnd = idx;
            while (runEnd + 1 < n
                && Objects.equals(s.itemPaints.get(runEnd + 1), runPaint)) {
                runEnd++;
            }
            int extEnd = (runEnd < n - 1) ? runEnd + 1 : runEnd;

            fillRun(g2, orientation,
                    s.topPoints.subList(idx, extEnd + 1),
                    s.bottomPoints.subList(idx, extEnd + 1),
                    runPaint);

            idx = runEnd + 1;
        }
    }

    private void fillRun(Graphics2D g2, PlotOrientation orientation,
                         List<Point2D> tops, List<Point2D> bottoms, Paint paint) {
        int m = tops.size();
        if (m < 1) {
            return;
        }
        if (m == 1) {
            // degenerate run - draw a thin line so something is visible
            // rather than silently dropping it.
            Point2D t = tops.get(0);
            Point2D b = bottoms.get(0);
            GeneralPath line = new GeneralPath();
            line.moveTo(b.getX(), b.getY());
            line.lineTo(t.getX(), t.getY());
            g2.setPaint(paint);
            g2.draw(line);
            return;
        }

        // Compute each boundary curve exactly once, forward, so that
        // whenever the *same* control points are used as the opposite
        // boundary of a neighbouring series, the resulting geometry is
        // bit-identical (only the traversal direction differs).
        List<Point2D> topCurve = computeMonotoneCurve(tops, orientation);
        List<Point2D> bottomCurve = computeMonotoneCurve(bottoms, orientation);

        GeneralPath path = new GeneralPath();
        Point2D b0 = bottomCurve.get(0);
        path.moveTo(b0.getX(), b0.getY());
        for (Point2D p : topCurve) {
            path.lineTo(p.getX(), p.getY());
        }
        List<Point2D> bottomReversed = new ArrayList<Point2D>(bottomCurve);
        Collections.reverse(bottomReversed);
        // first point of bottomReversed == last point of bottomCurve ==
        // top-right corner-ish point already reached via topCurve's last
        // point in value, but geometrically it's the base at the same x,
        // so start appending from index 0 of the reversed list (it is the
        // right-most base point, distinct from the top boundary).
        for (Point2D p : bottomReversed) {
            path.lineTo(p.getX(), p.getY());
        }
        path.closePath();

        g2.setPaint(paint);
        g2.fill(path);

        if (this.hideSeams) {
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(1.0f));
            g2.draw(path);
            g2.setStroke(oldStroke);
        }
    }

    /**
     * Computes a monotone cubic Hermite interpolation (Fritsch-Carlson)
     * through the given control points, expressed in Java2D space, and
     * returns the full list of points (including the originals) tracing
     * the curve in the <em>same order</em> as the input. The curve never
     * exceeds the local min/max of its two neighbouring control points,
     * so - unlike a natural cubic spline - it cannot overshoot and
     * therefore cannot produce self-intersecting loops in the resulting
     * fill path.
     *
     * @param points  control points, must have monotonically increasing
     *                domain-axis coordinate (x for VERTICAL orientation,
     *                y for HORIZONTAL orientation).
     * @param orientation  the plot orientation.
     *
     * @return the interpolated point list, same traversal order as input.
     */
    private List<Point2D> computeMonotoneCurve(List<Point2D> points,
                                               PlotOrientation orientation) {

        int n = points.size();
        List<Point2D> result = new ArrayList<Point2D>();
        if (n == 0) {
            return result;
        }
        result.add(points.get(0));
        if (n == 1) {
            return result;
        }

        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            Point2D p = points.get(i);
            if (orientation == PlotOrientation.HORIZONTAL) {
                x[i] = p.getY();
                y[i] = p.getX();
            }
            else {
                x[i] = p.getX();
                y[i] = p.getY();
            }
        }

        int segCount = n - 1;
        double[] d = new double[segCount]; // secant slopes
        for (int i = 0; i < segCount; i++) {
            double h = x[i + 1] - x[i];
            d[i] = (h > 1.0e-9) ? (y[i + 1] - y[i]) / h : 0.0;
        }

        double[] mtan = new double[n]; // tangents
        mtan[0] = d[0];
        mtan[n - 1] = d[segCount - 1];
        for (int i = 1; i < n - 1; i++) {
            mtan[i] = (d[i - 1] + d[i]) / 2.0;
        }

        // Fritsch-Carlson monotonicity constraint (sequential sweep -
        // deliberately uses already-adjusted tangents from the previous
        // iteration, matching the standard reference algorithm).
        for (int i = 0; i < segCount; i++) {
            if (d[i] == 0.0) {
                mtan[i] = 0.0;
                mtan[i + 1] = 0.0;
                continue;
            }
            double a = mtan[i] / d[i];
            double b = mtan[i + 1] / d[i];
            if (a < 0.0) {
                mtan[i] = 0.0;
                a = 0.0;
            }
            if (b < 0.0) {
                mtan[i + 1] = 0.0;
                b = 0.0;
            }
            double sSq = a * a + b * b;
            if (sSq > 9.0) {
                double t = 3.0 / Math.sqrt(sSq);
                mtan[i] = t * a * d[i];
                mtan[i + 1] = t * b * d[i];
            }
        }

        // Sample each segment using cubic Hermite basis functions.
        for (int i = 0; i < segCount; i++) {
            double h = x[i + 1] - x[i];
            if (h <= 1.0e-9) {
                // degenerate (near-zero width) segment - nothing to
                // interpolate, just carry the endpoint forward.
                Point2D endPoint = orientation == PlotOrientation.HORIZONTAL
                    ? new Point2D.Double(y[i + 1], x[i + 1])
                    : new Point2D.Double(x[i + 1], y[i + 1]);
                result.add(endPoint);
                continue;
            }
            for (int j = 1; j <= this.precision; j++) {
                double t = (double) j / this.precision;
                double t2 = t * t;
                double t3 = t2 * t;
                double h00 = 2 * t3 - 3 * t2 + 1;
                double h10 = t3 - 2 * t2 + t;
                double h01 = -2 * t3 + 3 * t2;
                double h11 = t3 - t2;
                double yv = h00 * y[i] + h10 * h * mtan[i]
                    + h01 * y[i + 1] + h11 * h * mtan[i + 1];
                double xv = x[i] + t * h;
                Point2D p = orientation == PlotOrientation.HORIZONTAL
                    ? new Point2D.Double(yv, xv)
                    : new Point2D.Double(xv, yv);
                result.add(p);
            }
        }

        return result;
    }

    /**
     * Duplicate of the private {@code getStackValues} helper in
     * {@link StackedXYAreaRenderer3} (not accessible from a subclass).
     */
    private double[] getStackValuesLocal(TableXYDataset dataset, int series,
                                         int index) {
        double[] result = new double[2];
        for (int i = 0; i < series; i++) {
            double v = dataset.getYValue(i, index);
            if (!Double.isNaN(v)) {
                if (v >= 0.0) {
                    result[1] += v;
                }
                else {
                    result[0] += v;
                }
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SmoothedStackedXYAreaRenderer)) {
            return false;
        }
        SmoothedStackedXYAreaRenderer that = (SmoothedStackedXYAreaRenderer) obj;
        if (this.precision != that.precision) {
            return false;
        }
        if (this.hideSeams != that.hideSeams) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}