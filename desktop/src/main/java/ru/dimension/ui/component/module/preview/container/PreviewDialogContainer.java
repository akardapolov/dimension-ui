package ru.dimension.ui.component.module.preview.container;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JDialog;
import ru.dimension.ui.component.module.preview.PreviewView;

public class PreviewDialogContainer implements IPreviewContainer {
  private final JDialog dialog;
  private final PreviewView panel;

  public PreviewDialogContainer(String title, PreviewView panel) {
    this.panel = panel;

    this.dialog = new JDialog();
    this.dialog.setTitle(title);
    this.dialog.add(panel);
    packAndCenter();
  }

  private void packAndCenter() {
    dialog.setModal(true);
    dialog.setResizable(true);
    dialog.pack();

    dialog.setSize(new Dimension(
        Toolkit.getDefaultToolkit().getScreenSize().width - 400,
        Toolkit.getDefaultToolkit().getScreenSize().height - 100
    ));
    dialog.setLocation(
        (Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - dialog.getWidth() / 2,
        (Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - dialog.getHeight() / 2
    );
  }

  @Override
  public void show() {
    dialog.setVisible(true);
  }

  @Override
  public Component getComponent() {
    return dialog;
  }
}