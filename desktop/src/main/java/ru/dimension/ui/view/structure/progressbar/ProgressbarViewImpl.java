package ru.dimension.ui.view.structure.progressbar;

import dagger.Lazy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.view.ProgressbarState;
import ru.dimension.ui.view.BaseFrame;
import ru.dimension.ui.view.structure.ProgressbarView;

@Log4j2
@Singleton
public class ProgressbarViewImpl extends JDialog implements ProgressbarView {

  private final Lazy<ProgressbarPresenter> profilePresenter;
  private final Lazy<BaseFrame> jFrame;

  @Inject
  public ProgressbarViewImpl(Lazy<BaseFrame> jFrame,
                             Lazy<ProgressbarPresenter> profilePresenter) {
    this.jFrame = jFrame;
    this.profilePresenter = profilePresenter;

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

    JFrame jFrameLocal = jFrame.get();
    int x = (jFrameLocal.getX() + jFrameLocal.getWidth() / 2) - 150;
    int y = (jFrameLocal.getY() + jFrameLocal.getHeight() / 2) - 25;
    this.setBounds(x, y, 300, 80);
    this.setVisible(true);
  }

  @Override
  public void bindPresenter() {
    profilePresenter.get();
  }

}
