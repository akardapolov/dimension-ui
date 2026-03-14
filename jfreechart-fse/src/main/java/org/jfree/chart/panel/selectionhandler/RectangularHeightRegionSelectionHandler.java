/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2013, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * --------------------------------------
 * RectangularRegionSelectionHandler.java
 * --------------------------------------
 * (C) Copyright 2009-2013, by Object Refinery Limited and Contributors.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   Michael Zinsmaier;
 *
 * Changes:
 * --------
 * 19-Jun-2009 : Version 1 (DG);
 *
 */

package org.jfree.chart.panel.selectionhandler;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.ShapeUtilities;
import org.jfree.data.xy.CategoryTableXYDataset;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A mouse handler that allows data items to be selected. The selection shape
 * is a rectangle that can be expanded by dragging the mouse away from the starting
 * point. 
 * 
 * Will only work together with a ChartPanel as event source
 * @author zinsmaie
 */
public class RectangularHeightRegionSelectionHandler extends RegionSelectionHandler {

  private static final long serialVersionUID = -8496935828054326324L;

  public static final int HANDLE_WIDTH = 8;
  private static final int CLICK_THRESHOLD = 3;
  private static final double AUTO_RANGE_FRACTION = 0.10;

  public enum DragMode {
    IDLE, CREATING, RESIZE_LEFT, RESIZE_RIGHT, MOVING
  }

  private Rectangle selectionRect;
  private Point2D startPoint;
  private DragMode dragMode = DragMode.IDLE;

  private int dragOffsetX;
  private int fixedLeftX;
  private int fixedRightX;
  private Point2D lastDragPoint;

  private Paint normalFillPaint;
  private Paint movingFillPaint;

  private CategoryTableXYDataset dataset;

  public RectangularHeightRegionSelectionHandler() {
    super();
    this.selectionRect = null;
    this.startPoint = null;
  }

  public RectangularHeightRegionSelectionHandler(int modifier) {
    super(modifier);
    this.selectionRect = null;
    this.startPoint = null;
  }

  public RectangularHeightRegionSelectionHandler(Stroke outlineStroke,
                                                 Paint outlinePaint, Paint fillPaint) {
    super(outlineStroke, outlinePaint, fillPaint);
    this.selectionRect = null;
    this.startPoint = null;
  }

  public Point2D getStartPoint() {
    return startPoint;
  }

  public DragMode getDragMode() {
    return dragMode;
  }

  public void setDataset(CategoryTableXYDataset dataset) {
    this.dataset = dataset;
  }

  public CategoryTableXYDataset getDataset() {
    return this.dataset;
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (!(e.getSource() instanceof ChartPanel)) {
      return;
    }
    ChartPanel panel = (ChartPanel) e.getSource();
    Rectangle2D dataArea = panel.getScreenDataArea();
    if (!dataArea.contains(e.getPoint())) {
      return;
    }

    SelectionManager selectionManager = panel.getSelectionManager();
    if (selectionManager == null) {
      return;
    }

    Point pt = e.getPoint();
    Shape existingShape = panel.getSelectionShape();

    if (existingShape != null) {
      Rectangle2D bounds = existingShape.getBounds2D();

      Rectangle2D leftHandle = getLeftHandleRect(bounds);
      Rectangle2D rightHandle = getRightHandleRect(bounds);

      if (leftHandle.contains(pt)) {
        dragMode = DragMode.RESIZE_LEFT;
        fixedRightX = (int) bounds.getMaxX();
        lastDragPoint = new Point(pt);
        return;
      }

      if (rightHandle.contains(pt)) {
        dragMode = DragMode.RESIZE_RIGHT;
        fixedLeftX = (int) bounds.getMinX();
        lastDragPoint = new Point(pt);
        return;
      }

      if (bounds.contains(pt)) {
        dragMode = DragMode.MOVING;
        lastDragPoint = new Point(pt);
        selectionRect = new Rectangle(
            (int) bounds.getX(), (int) bounds.getY(),
            (int) bounds.getWidth(), (int) bounds.getHeight()
        );
        normalFillPaint = this.fillPaint;
        movingFillPaint = createDarkerPaint(this.fillPaint);
        panel.setSelectionFillPaint(movingFillPaint);
        panel.repaint();
        return;
      }
    }

    if (!e.isShiftDown()) {
      selectionManager.clearSelection();
    }
    dragMode = DragMode.CREATING;
    this.startPoint = new Point(pt);
    this.selectionRect = new Rectangle(pt.x, pt.y, 1, 1);
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (!(e.getSource() instanceof ChartPanel)) {
      return;
    }
    ChartPanel panel = (ChartPanel) e.getSource();

    if (dragMode == DragMode.IDLE) {
      panel.clearLiveMouseHandler();
      return;
    }

    Rectangle2D dataArea = panel.getScreenDataArea();
    Point pt = e.getPoint();
    Point2D pt2 = ShapeUtilities.getPointInRectangle(pt.x, pt.y, dataArea);

    switch (dragMode) {
      case CREATING:
        if (this.startPoint == null) {
          panel.clearLiveMouseHandler();
          return;
        }
        selectionRect = getRect(startPoint, pt2, dataArea);
        break;

      case RESIZE_LEFT: {
        int newLeftX = (int) Math.max(pt2.getX(), dataArea.getMinX());
        newLeftX = Math.min(newLeftX, fixedRightX - 1);
        int minY = (int) dataArea.getMinY();
        int h = (int) dataArea.getHeight();
        selectionRect = new Rectangle(newLeftX, minY, fixedRightX - newLeftX, h);
        break;
      }

      case RESIZE_RIGHT: {
        int newRightX = (int) Math.min(pt2.getX(), dataArea.getMaxX());
        newRightX = Math.max(newRightX, fixedLeftX + 1);
        int minY = (int) dataArea.getMinY();
        int h = (int) dataArea.getHeight();
        selectionRect = new Rectangle(fixedLeftX, minY, newRightX - fixedLeftX, h);
        break;
      }

      case MOVING: {
        if (lastDragPoint == null || selectionRect == null) {
          break;
        }
        int deltaX = (int) (pt2.getX() - lastDragPoint.getX());
        int newX = selectionRect.x + deltaX;
        int minAllowed = (int) dataArea.getMinX();
        int maxAllowed = (int) dataArea.getMaxX() - selectionRect.width;
        newX = Math.max(newX, minAllowed);
        newX = Math.min(newX, maxAllowed);
        selectionRect = new Rectangle(
            newX, (int) dataArea.getMinY(),
            selectionRect.width, (int) dataArea.getHeight()
        );
        lastDragPoint = pt2;
        break;
      }

      default:
        break;
    }

    panel.setSelectionShape(selectionRect);
    panel.setSelectionFillPaint(
        dragMode == DragMode.MOVING ? movingFillPaint : this.fillPaint
    );
    panel.setSelectionOutlinePaint(this.outlinePaint);
    panel.repaint();
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (!(e.getSource() instanceof ChartPanel)) {
      return;
    }
    ChartPanel panel = (ChartPanel) e.getSource();

    if (dragMode == DragMode.IDLE) {
      panel.clearLiveMouseHandler();
      return;
    }

    if (dragMode == DragMode.MOVING && normalFillPaint != null) {
      panel.setSelectionFillPaint(normalFillPaint);
      normalFillPaint = null;
      movingFillPaint = null;
    }

    if (dragMode == DragMode.CREATING && isClick(e)) {
      Rectangle autoRect = buildAutoRangeRect(e, panel);
      if (autoRect != null) {
        selectionRect = autoRect;
      }
    }

    SelectionManager selectionManager = panel.getSelectionManager();
    if (selectionManager != null && selectionRect != null) {
      selectionManager.select(selectionRect);
    }

    panel.setSelectionShape(selectionRect);
    panel.setSelectionFillPaint(this.fillPaint);
    panel.setSelectionOutlinePaint(this.outlinePaint);
    panel.repaint();

    dragMode = DragMode.IDLE;
    lastDragPoint = null;
    panel.clearLiveMouseHandler();
  }

  private boolean isClick(MouseEvent e) {
    if (startPoint == null) {
      return true;
    }
    double dx = Math.abs(e.getX() - startPoint.getX());
    double dy = Math.abs(e.getY() - startPoint.getY());
    return dx <= CLICK_THRESHOLD && dy <= CLICK_THRESHOLD;
  }

  private Rectangle buildAutoRangeRect(MouseEvent e, ChartPanel panel) {
    if (dataset == null || dataset.getItemCount() == 0 || dataset.getSeriesCount() == 0) {
      return null;
    }

    try {
      Rectangle2D dataArea = panel.getScreenDataArea();
      if (dataArea == null) {
        return null;
      }

      double dataStart = ((Number) dataset.getX(0, 0)).doubleValue();
      double dataEnd = ((Number) dataset.getX(0, dataset.getItemCount() - 1)).doubleValue();
      double totalRange = dataEnd - dataStart;

      if (totalRange <= 0) {
        return null;
      }

      double selectionRange = totalRange * AUTO_RANGE_FRACTION;

      XYPlot plot = (XYPlot) panel.getChart().getPlot();
      ValueAxis domainAxis = plot.getDomainAxis();
      RectangleEdge domainEdge = plot.getDomainAxisEdge();

      double clickDomainValue = domainAxis.java2DToValue(e.getX(), dataArea, domainEdge);

      double clickPosition = (clickDomainValue - dataStart) / totalRange;
      clickPosition = Math.max(0.0, Math.min(1.0, clickPosition));

      double leftFraction = clickPosition;
      double rightFraction = 1.0 - clickPosition;
      double totalFractions = leftFraction + rightFraction;

      double leftPart;
      double rightPart;

      if (totalFractions == 0) {
        leftPart = selectionRange / 2.0;
        rightPart = selectionRange / 2.0;
      } else {
        leftPart = selectionRange * (leftFraction / totalFractions);
        rightPart = selectionRange * (rightFraction / totalFractions);
      }

      double selStart = clickDomainValue - leftPart;
      double selEnd = clickDomainValue + rightPart;

      selStart = Math.max(selStart, dataStart);
      selEnd = Math.min(selEnd, dataEnd);

      if (selEnd - selStart < 1) {
        return null;
      }

      double x1 = domainAxis.valueToJava2D(selStart, dataArea, domainEdge);
      double x2 = domainAxis.valueToJava2D(selEnd, dataArea, domainEdge);

      double minX = Math.max(Math.min(x1, x2), dataArea.getMinX());
      double maxX = Math.min(Math.max(x1, x2), dataArea.getMaxX());
      double width = maxX - minX;

      if (width < 1) {
        width = 1;
      }

      return new Rectangle(
          (int) minX,
          (int) dataArea.getMinY(),
          (int) width,
          (int) dataArea.getHeight()
      );
    } catch (Exception ex) {
      return null;
    }
  }

  public Rectangle2D getRectangle2D() {
    return this.selectionRect;
  }

  public Rectangle getRect(Point2D p1, Point2D p2, Rectangle2D plotarea) {
    int minX, w;

    if (p1.getX() < p2.getX()) {
      minX = (int) p1.getX();
      w = (int) p2.getX() - minX;
    } else {
      minX = (int) p2.getX();
      w = (int) p1.getX() - minX;
      if (w <= 0) {
        w = 1;
      }
    }

    int minY = (int) plotarea.getMinY();
    int h = (int) plotarea.getHeight();

    return new Rectangle(minX, minY, w, h);
  }

  public static Rectangle2D getLeftHandleRect(Rectangle2D bounds) {
    double hx = bounds.getMinX() - HANDLE_WIDTH / 2.0;
    return new Rectangle2D.Double(hx, bounds.getMinY(), HANDLE_WIDTH, bounds.getHeight());
  }

  public static Rectangle2D getRightHandleRect(Rectangle2D bounds) {
    double hx = bounds.getMaxX() - HANDLE_WIDTH / 2.0;
    return new Rectangle2D.Double(hx, bounds.getMinY(), HANDLE_WIDTH, bounds.getHeight());
  }

  private Paint createDarkerPaint(Paint paint) {
    if (paint instanceof Color) {
      Color c = (Color) paint;
      int newAlpha = Math.min(255, c.getAlpha() + 60);
      return new Color(
          Math.max(0, c.getRed() - 30),
          Math.max(0, c.getGreen() - 30),
          Math.max(0, c.getBlue() - 30),
          newAlpha
      );
    }
    return paint;
  }
}