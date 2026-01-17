package ru.dimension.ui.component.module.charts;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.REPORT;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.VerticalLayout;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.module.chart.ChartModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;

@Log4j2
public class ChartsView extends JPanel {
  private final JXTaskPaneContainer cardContainer;
  private final JScrollPane cardScrollPane;
  private final JPanel cardPanel;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public ChartsView() {
    cardContainer = new JXTaskPaneContainer();
    LaF.setBackgroundColor(REPORT, cardContainer);
    cardContainer.setBackgroundPainter(null);

    cardScrollPane = new JScrollPane();
    GUIHelper.setScrolling(cardScrollPane);

    cardPanel = new JPanel(new VerticalLayout());
    LaF.setBackgroundColor(REPORT, cardPanel);
    cardPanel.add(cardContainer);

    cardScrollPane.setViewportView(cardPanel);

    setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(cardScrollPane).fillXY();
    gbl.done();
  }

  public void addChartCard(ChartModule taskPane,
                           BiConsumer<ChartModule, Exception> onComplete) {

    addTaskPane(taskPane);

    SwingTaskRunner.runWithProgress(
        taskPane,
        executor,
        taskPane::initializeUI,
        e -> {
          removeTaskPane(taskPane);
          onComplete.accept(null, e);
        },
        () -> createProgressBar("Loading, please wait..."),
        () -> onComplete.accept(taskPane, null)
    );
  }

  public void removeChartCard(ChartModule taskPane) {
    if (taskPane != null) {
      cardContainer.remove(taskPane);
      cardContainer.revalidate();
      cardContainer.repaint();
    }

    for (Component comp : cardContainer.getComponents()) {
      if (comp instanceof ChartModule chartModule) {
        log.info("Task pane: {}", chartModule.getTitle());
      }
    }
  }

  private void addTaskPane(ChartModule taskPane) {
    cardContainer.add(taskPane);
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  private void removeTaskPane(ChartModule taskPane) {
    cardContainer.remove(taskPane);
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  void centerComponentInScrollPane(Component component) {
    SwingUtilities.invokeLater(() -> {
      Point componentPos = SwingUtilities.convertPoint(
          component.getParent(),
          component.getLocation(),
          cardPanel
      );

      JViewport viewport = cardScrollPane.getViewport();
      int topMargin = 10;
      int targetY = Math.max(0, componentPos.y - topMargin);

      int startY = viewport.getViewPosition().y;
      int distance = targetY - startY;

      if (Math.abs(distance) < 5) {
        viewport.setViewPosition(new Point(0, targetY));
        return;
      }

      final int DURATION = 500;
      final int DELAY = 15;
      final long startTime = System.currentTimeMillis();

      javax.swing.Timer timer = new javax.swing.Timer(DELAY, null);
      timer.addActionListener(e -> {
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;
        float progress = (float) elapsed / DURATION;

        if (progress >= 1.0f) {
          viewport.setViewPosition(new Point(0, targetY));
          timer.stop();
        } else {
          double ease = 0.5 * (1 - Math.cos(progress * Math.PI));
          int newY = (int) (startY + (distance * ease));
          viewport.setViewPosition(new Point(0, newY));
        }
      });
      timer.start();
    });
  }
}
