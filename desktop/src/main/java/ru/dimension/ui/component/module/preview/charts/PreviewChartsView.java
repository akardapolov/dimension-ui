package ru.dimension.ui.component.module.preview.charts;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.module.PreviewChartModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;

@Log4j2
public class PreviewChartsView extends JPanel {
  private final JXTaskPaneContainer cardContainer;
  private final JScrollPane cardScrollPane;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public PreviewChartsView(PreviewChartsModel model) {
    setBorder(new EtchedBorder());
    LaF.setBackgroundColor(CHART_PANEL, this);

    cardContainer = new JXTaskPaneContainer();
    LaF.setBackgroundColor(CHART_PANEL, cardContainer);
    cardContainer.setBackgroundPainter(null);

    cardScrollPane = new JScrollPane();
    GUIHelper.setScrolling(cardScrollPane);
    cardScrollPane.setViewportView(cardContainer);

    buildLayout();
  }

  private void buildLayout() {
    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(cardScrollPane).fillXY();
    gbl.done();
  }

  public void addChartCard(PreviewChartModule taskPane,
                           BiConsumer<PreviewChartModule, Exception> onComplete) {

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

  public void removeChartCard(PreviewChartModule taskPane) {
    if (taskPane != null) {
      cardContainer.remove(taskPane);
      cardContainer.revalidate();
      cardContainer.repaint();
    }

    for (Component comp : cardContainer.getComponents()) {
      if (comp instanceof PreviewChartModule chartModule) {
        log.info("Task pane: " + chartModule.getTitle());
      }
    }
  }

  private void addTaskPane(PreviewChartModule taskPane) {
    cardContainer.add(taskPane);
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  private void removeTaskPane(PreviewChartModule taskPane) {
    cardContainer.remove(taskPane);
    cardContainer.revalidate();
    cardContainer.repaint();
  }

  public JXTaskPaneContainer getCardContainer() {
    return cardContainer;
  }

  public void clearAllCharts() {
    cardContainer.removeAll();
    cardContainer.revalidate();
    cardContainer.repaint();
  }
}
