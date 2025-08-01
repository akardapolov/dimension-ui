package ru.dimension.ui.component.module.charts;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.REPORT;

import java.awt.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.VerticalLayout;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.module.ChartModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.view.analyze.module.ChartAnalyzeModule;

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
        log.info("Task pane: " + chartModule.getTitle());
      }
    }
  }

  public void updateLegendVisibility(boolean isVisible) {
    for (Component comp : cardContainer.getComponents()) {
      if (comp instanceof ChartAnalyzeModule) {
        ((ChartAnalyzeModule) comp).getScp().getjFreeChart().getLegend().setVisible(isVisible);
      }
    }
  }

  public void expandCollapseChartCard(boolean collapseAll) {
    for (Component comp : cardContainer.getComponents()) {
      if (comp instanceof JXTaskPane) {
        ((JXTaskPane) comp).setCollapsed(collapseAll);
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

  public void clearAllCharts() {
    cardContainer.removeAll();
    cardContainer.revalidate();
    cardContainer.repaint();
  }
}
