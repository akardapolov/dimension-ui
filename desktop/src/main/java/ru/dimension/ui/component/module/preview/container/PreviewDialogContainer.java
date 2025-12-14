package ru.dimension.ui.component.module.preview.container;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import ru.dimension.ui.component.module.preview.PreviewView;

public class PreviewDialogContainer implements IPreviewContainer {
  private final JDialog dialog;
  private final PreviewView panel;

  public PreviewDialogContainer(String title, PreviewView panel) {
    this.panel = panel;

    this.dialog = new JDialog();
    this.dialog.setTitle(title);
    this.dialog.add(panel);

    setEscToClose();
    packAndCenter();
  }

  private void setEscToClose() {
    dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

    final String actionKey = "PREVIEW_DIALOG_CLOSE_ON_ESC";
    JRootPane root = dialog.getRootPane();

    root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);

    root.getActionMap().put(actionKey, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
      }
    });
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