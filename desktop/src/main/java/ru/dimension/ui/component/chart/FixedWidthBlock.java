package ru.dimension.ui.component.chart;

import java.awt.Graphics2D;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.BorderArrangement;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.Size2D;

public class FixedWidthBlock extends BlockContainer {

  private final double fixedWidth;

  public FixedWidthBlock(Block content, double fixedWidth) {
    super(new BorderArrangement());
    this.fixedWidth = fixedWidth;
    add(content, RectangleEdge.LEFT);
  }

  @Override
  public Size2D arrange(Graphics2D g2, RectangleConstraint constraint) {
    Size2D natural = super.arrange(g2, constraint);

    Size2D result = new Size2D();
    result.width = fixedWidth;
    result.height = natural != null ? natural.height : 0;
    return result;
  }
}