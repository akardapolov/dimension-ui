package ru.dimension.ui.view.panel.report;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class ImagePanel extends JPanel {

  private static final long serialVersionUID = -8483797305070521030L;

  private Image image;
  private int width;
  private int height;

  public ImagePanel(Image image,
                    int width,
                    int height) {
    this.image = image;
    this.width = width;
    this.height = height;

    setBorder(new EmptyBorder(0, 0, 0, 0));
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);
    graphics.drawImage(image, 0, 0, width, height, null);
  }
}