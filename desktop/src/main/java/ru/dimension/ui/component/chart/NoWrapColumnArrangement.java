package ru.dimension.ui.component.chart;

import java.awt.Graphics2D;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.ColumnArrangement;
import org.jfree.chart.block.LengthConstraintType;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.ui.Size2D;

public class NoWrapColumnArrangement extends ColumnArrangement {

  private volatile boolean noWrap = true;

  public NoWrapColumnArrangement() {
  }

  public void setNoWrap(boolean noWrap) {
    this.noWrap = noWrap;
  }

  public boolean isNoWrap() {
    return noWrap;
  }

  @Override
  public Size2D arrange(BlockContainer container,
                        Graphics2D g2,
                        RectangleConstraint constraint) {
    if (!noWrap) {
      return super.arrange(container, g2, constraint);
    }

    Size2D natural = arrangeNN(container, g2);

    double height = natural.height;
    if (constraint.getHeightConstraintType() == LengthConstraintType.FIXED) {
      height = constraint.getHeight();
    }

    return new Size2D(natural.width, height);
  }
}