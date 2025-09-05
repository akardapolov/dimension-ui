package ru.dimension.ui.component.module.adhoc.charts;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.REPORT;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.VerticalLayout;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.module.adhoc.AdHocChartModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;

public class AdHocChartsView extends JPanel {

  private final JTabbedPane tabbedPane;
  private final Map<String, JPanel> tabPanels;
  private final Map<String, JXTaskPaneContainer> tabContainers;

  private Consumer<String> tabChangeListener;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public AdHocChartsView() {
    tabbedPane = new JTabbedPane();
    tabPanels = new HashMap<>();
    tabContainers = new HashMap<>();

    setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(tabbedPane).fillXY();
    gbl.done();

    tabbedPane.addChangeListener(e -> handleTabChange());
  }

  public void setTabChangeListener(Consumer<String> listener) {
    this.tabChangeListener = listener;
  }

  private void handleTabChange() {
    int selectedIndex = tabbedPane.getSelectedIndex();
    if (selectedIndex >= 0 && tabChangeListener != null) {
      JPanel selectedPanel = (JPanel) tabbedPane.getComponentAt(selectedIndex);
      String globalKey = (String) selectedPanel.getClientProperty("globalKey");
      if (globalKey != null) {
        tabChangeListener.accept(globalKey);
      }
    }
  }

  public void addChartCard(String tabKey,
                           String globalKey,
                           AdHocChartModule taskPane,
                           BiConsumer<AdHocChartModule, Exception> onComplete) {

    JPanel tabPanel = tabPanels.computeIfAbsent(tabKey, k -> {
      JXTaskPaneContainer container = new JXTaskPaneContainer();
      LaF.setBackgroundColor(REPORT, container);
      container.setBackgroundPainter(null);

      JPanel containerPanel = new JPanel(new VerticalLayout());
      LaF.setBackgroundColor(REPORT, containerPanel);
      containerPanel.add(container);

      JScrollPane scrollPane = new JScrollPane(containerPanel);
      GUIHelper.setScrolling(scrollPane);

      JPanel panel = new JPanel();
      panel.putClientProperty("globalKey", globalKey);
      LaF.setBackgroundColor(REPORT, panel);

      PainlessGridBag gbl = new PainlessGridBag(panel, PGHelper.getPGConfig(), false);
      gbl.row().cellXYRemainder(scrollPane).fillXY();
      gbl.done();

      tabContainers.put(tabKey, container);

      tabbedPane.addTab(tabKey, panel);
      panel.putClientProperty("tabKey", tabKey);

      return panel;
    });

    JXTaskPaneContainer container = tabContainers.get(tabKey);
    container.add(taskPane);
    container.revalidate();
    container.repaint();

    int index = tabbedPane.indexOfComponent(tabPanel);
    if (index != -1) {
      tabbedPane.setSelectedIndex(index);
    }

    SwingTaskRunner.runWithProgress(
        taskPane,
        executor,
        taskPane::initializeUI,
        e -> {
          removeChartCard(tabKey, taskPane);
          onComplete.accept(null, e);
        },
        () -> createProgressBar("Loading, please wait..."),
        () -> onComplete.accept(taskPane, null)
    );
  }

  public void removeChartCard(String tabKey, AdHocChartModule taskPane) {
    JXTaskPaneContainer container = tabContainers.get(tabKey);
    if (container != null && taskPane != null) {
      container.remove(taskPane);
      container.revalidate();
      container.repaint();

      if (container.getComponentCount() == 0) {
        tabbedPane.remove(tabPanels.get(tabKey));
        tabPanels.remove(tabKey);
        tabContainers.remove(tabKey);
      }
    }
  }

  public void clearAllCharts() {
    tabbedPane.removeAll();
    tabPanels.clear();
    tabContainers.clear();
  }
}