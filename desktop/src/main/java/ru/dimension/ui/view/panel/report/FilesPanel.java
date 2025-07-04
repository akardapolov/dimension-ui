package ru.dimension.ui.view.panel.report;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JDialog;

public class FilesPanel extends JDialog {

  public FilesPanel() {
    this.setTitle("Design file management");
    this.packTemplate(false);
  }

  private void packTemplate(boolean visible) {
    this.setVisible(visible);
    this.setResizable(false);
    this.setModal(true);
    this.setResizable(false);
    this.pack();

    this.setSize(new Dimension(440, 85));
    this.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - getWidth() / 2,
                     (Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - getHeight() / 2);
  }
}
