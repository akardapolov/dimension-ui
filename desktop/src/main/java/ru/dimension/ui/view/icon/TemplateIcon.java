package ru.dimension.ui.view.icon;

import java.awt.*;
import java.awt.geom.*;

/**
 * Document/template icon for template button.
 */
public class TemplateIcon extends AbstractPaintedIcon {

  private final Color primaryColor;

  public TemplateIcon() {
    this(20, 20);
  }

  public TemplateIcon(int size) {
    this(size, size);
  }

  public TemplateIcon(int width, int height) {
    super(width, height);
    this.primaryColor = SOFT_AMBER;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Graphics2D g2d = prepareGraphics(g);
    g2d.translate(x, y);

    float margin = 2f;
    float foldSize = width * 0.25f;
    float docWidth = width - margin * 2;
    float docHeight = height - margin * 2;

    // Create document shape with folded corner
    Path2D document = new Path2D.Float();
    document.moveTo(margin, margin);
    document.lineTo(margin + docWidth - foldSize, margin);
    document.lineTo(margin + docWidth, margin + foldSize);
    document.lineTo(margin + docWidth, margin + docHeight);
    document.lineTo(margin, margin + docHeight);
    document.closePath();

    // Draw shadow
    g2d.translate(0.7, 0.7);
    g2d.setColor(new Color(0, 0, 0, 50));
    g2d.fill(document);
    g2d.translate(-0.7, -0.7);

    // Fill document with gradient (using SOFT_AMBER as base)
    GradientPaint gradient = new GradientPaint(
        margin, margin, brighter(primaryColor, 0.1f),
        margin + docWidth, margin + docHeight, darker(primaryColor, 0.15f)
    );
    g2d.setPaint(gradient);
    g2d.fill(document);

    // Draw document outline
    g2d.setColor(brighter(primaryColor, 0.15f));
    g2d.setStroke(new BasicStroke(1.0f));
    g2d.draw(document);

    // Draw fold
    Path2D fold = new Path2D.Float();
    fold.moveTo(margin + docWidth - foldSize, margin);
    fold.lineTo(margin + docWidth - foldSize, margin + foldSize);
    fold.lineTo(margin + docWidth, margin + foldSize);

    g2d.setColor(darker(primaryColor, 0.25f));
    g2d.fill(fold);
    g2d.setColor(brighter(primaryColor, 0.1f));
    g2d.draw(fold);

    // Draw content lines
    Color lineColor = darker(primaryColor, 0.35f);
    g2d.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 200));
    g2d.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

    float lineMargin = margin + 3f;
    float lineWidth = docWidth - 8f;
    float lineStartY = margin + foldSize + 3f;
    float lineSpacing = 3.5f;

    for (int i = 0; i < 3; i++) {
      float lineY = lineStartY + (i * lineSpacing);
      float currentLineWidth = (i == 2) ? lineWidth * 0.6f : lineWidth;

      if (lineY < margin + docHeight - 3) {
        g2d.draw(new Line2D.Float(
            lineMargin, lineY,
            lineMargin + currentLineWidth, lineY
        ));
      }
    }

    g2d.dispose();
  }
}