package org.jfree.chart;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import org.jfree.chart.panel.selectionhandler.SelectionManager;

public class SelectionWheelHandler implements MouseWheelListener, Serializable {

  private static final long serialVersionUID = 1L;

  private final ChartPanel chartPanel;
  private double stepFraction;
  private double minWidthFraction;

  public SelectionWheelHandler(ChartPanel chartPanel) {
    this(chartPanel, 0.02, 0.01);
  }

  public SelectionWheelHandler(ChartPanel chartPanel, double stepFraction, double minWidthFraction) {
    this.chartPanel = chartPanel;
    this.stepFraction = stepFraction;
    this.minWidthFraction = minWidthFraction;
    this.chartPanel.addMouseWheelListener(this);
  }

  public double getStepFraction() {
    return this.stepFraction;
  }

  public void setStepFraction(double stepFraction) {
    this.stepFraction = stepFraction;
  }

  public double getMinWidthFraction() {
    return this.minWidthFraction;
  }

  public void setMinWidthFraction(double minWidthFraction) {
    this.minWidthFraction = minWidthFraction;
  }

  public void remove() {
    this.chartPanel.removeMouseWheelListener(this);
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    Shape selectionShape = chartPanel.getSelectionShape();
    if (selectionShape == null) {
      return;
    }

    Rectangle2D bounds = selectionShape.getBounds2D();
    if (!bounds.contains(e.getPoint())) {
      return;
    }

    Rectangle2D dataArea = chartPanel.getScreenDataArea();
    if (dataArea == null) {
      return;
    }

    int clicks = e.getWheelRotation();
    if (clicks == 0) {
      return;
    }

    double currentWidth = bounds.getWidth();
    double delta = currentWidth * stepFraction * Math.abs(clicks);

    double dataAreaWidth = dataArea.getWidth();
    double minWidth = dataAreaWidth * minWidthFraction;

    double currentLeft = bounds.getMinX();
    double currentRight = bounds.getMaxX();
    double areaLeft = dataArea.getMinX();
    double areaRight = dataArea.getMaxX();

    double newLeft;
    double newRight;

    if (clicks < 0) {
      double newWidth = currentWidth - 2 * delta;
      if (newWidth < minWidth) {
        delta = (currentWidth - minWidth) / 2.0;
        if (delta < 0) {
          return;
        }
      }
      newLeft = currentLeft + delta;
      newRight = currentRight - delta;
    } else {
      newLeft = currentLeft - delta;
      newRight = currentRight + delta;
    }

    if (newLeft < areaLeft) {
      double overflow = areaLeft - newLeft;
      newLeft = areaLeft;
      newRight = newRight + overflow;
    }

    if (newRight > areaRight) {
      double overflow = newRight - areaRight;
      newRight = areaRight;
      newLeft = newLeft - overflow;
    }

    newLeft = Math.max(newLeft, areaLeft);
    newRight = Math.min(newRight, areaRight);

    double finalWidth = newRight - newLeft;
    if (finalWidth < minWidth) {
      return;
    }

    Rectangle newRect = new Rectangle(
        (int) newLeft,
        (int) dataArea.getMinY(),
        (int) (newRight - newLeft),
        (int) dataArea.getHeight()
    );

    chartPanel.setSelectionShape(newRect);

    SelectionManager selectionManager = chartPanel.getSelectionManager();
    if (selectionManager != null) {
      selectionManager.select(newRect);
    }

    chartPanel.repaint();
    chartPanel.fireReleaseMouse();
  }
}