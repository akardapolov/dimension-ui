package ru.dimension.ui.component.module.adhoc.charts;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.REPORT;

import java.awt.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.VerticalLayout;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.module.adhoc.AdHocChartModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.view.analyze.module.ChartAnalyzeModule;

public class AdHocChartsView extends JPanel {

  private final JXTaskPaneContainer cardContainer;
  private final JScrollPane cardScrollPane;
  private final JPanel cardPanel;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public AdHocChartsView() {
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

  public void addChartCard(AdHocChartModule taskPane,
                           BiConsumer<AdHocChartModule, Exception> onComplete) {

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

  public void removeChartCard(AdHocChartModule taskPane) {
    if (taskPane != null) {
      cardContainer.remove(taskPane);
      cardContainer.revalidate();
      cardContainer.repaint();
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

  private void addTaskPane(AdHocChartModule taskPane) {
    cardContainer.add(taskPane);
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  private void removeTaskPane(AdHocChartModule taskPane) {
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