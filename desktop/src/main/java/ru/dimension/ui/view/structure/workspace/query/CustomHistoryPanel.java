package ru.dimension.ui.view.structure.workspace.query;

import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JDialog;

public class CustomHistoryPanel extends JDialog {

  public CustomHistoryPanel() {
    this.setTitle("Custom");
    this.packTemplate(false);
  }

  private void packTemplate(boolean visible) {
    this.setVisible(visible);
    this.setResizable(false);
    this.setModal(true);
    this.setResizable(true);
    this.pack();

    this.setSize(new Dimension(600, 160));
    this.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - getWidth() / 2,
                     (Toolkit.getDefaultToolkit().getScreenSize().height) / 3 - getHeight() / 3);
  }
}
