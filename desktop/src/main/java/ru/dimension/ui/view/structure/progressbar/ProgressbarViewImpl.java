package ru.dimension.ui.view.structure.progressbar;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.view.ProgressbarState;
import ru.dimension.ui.view.structure.ProgressbarView;

@Log4j2
@Singleton
public class ProgressbarViewImpl extends JDialog implements ProgressbarView {

  @Inject
  public ProgressbarViewImpl() {
    this.setTitle("Progress..");
    this.setVisible(false);
  }

  @Override
  public void setProgressbarVisible(ProgressbarState progressbarState) {
    if (progressbarState == ProgressbarState.SHOW) {
      this.createProgressBar();
    } else if (progressbarState == ProgressbarState.HIDE) {
      this.getContentPane().removeAll();
      this.setVisible(false);
    }
  }

  private void createProgressBar() {
    String title = "Loading, please wait..";

    JProgressBar progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setStringPainted(true);
    progressBar.setString(title);

    this.setContentPane(progressBar);

    Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();

    int x = (activeWindow.getX() + activeWindow.getWidth() / 2) - 150;
    int y = (activeWindow.getY() + activeWindow.getHeight() / 2) - 25;
    this.setBounds(x, y, 300, 80);
    this.setVisible(true);
  }

  @Override
  public void bindPresenter(ProgressbarPresenter presenter) {
    log.info("Bind presenter");
  }

}
