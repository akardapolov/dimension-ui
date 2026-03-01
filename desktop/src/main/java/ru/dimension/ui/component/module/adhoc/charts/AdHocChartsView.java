package ru.dimension.ui.component.module.adhoc.charts;

import static ru.dimension.ui.helper.ProgressBarHelper.createProgressBar;
import static ru.dimension.ui.laf.LafColorGroup.REPORT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.VerticalLayout;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.module.adhoc.AdHocChartModule;
import ru.dimension.ui.component.module.adhoc.raw.AdHocRawPanel;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.SwingTaskRunner;
import ru.dimension.ui.laf.LaF;

public class AdHocChartsView extends JPanel {

  private final JTabbedPane tabbedPane;
  private final Map<String, JPanel> tabPanels;
  private final Map<String, JXTaskPaneContainer> tabContainers;
  private final Map<Integer, List<String>> connectionTabKeys;

  private Consumer<String> tabChangeListener;

  private BiConsumer<String, String> tabCloseListener;

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public AdHocChartsView() {
    tabbedPane = new JTabbedPane();
    tabPanels = new HashMap<>();
    tabContainers = new HashMap<>();
    connectionTabKeys = new HashMap<>();

    setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(tabbedPane).fillXY();
    gbl.done();

    tabbedPane.addChangeListener(e -> handleTabChange());
  }

  public void setTabChangeListener(Consumer<String> listener) {
    this.tabChangeListener = listener;
  }

  public void setTabCloseListener(BiConsumer<String, String> listener) {
    this.tabCloseListener = listener;
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
                           int connectionId,
                           AdHocChartModule taskPane,
                           BiConsumer<AdHocChartModule, Exception> onComplete) {

    JPanel tabPanel = getOrCreateTabPanel(tabKey, globalKey, connectionId);

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

  public void addRawCard(String tabKey,
                         String globalKey,
                         int connectionId,
                         AdHocRawPanel rawPanel,
                         Runnable onComplete) {

    JPanel tabPanel = getOrCreateTabPanel(tabKey, globalKey, connectionId);

    JXTaskPaneContainer container = tabContainers.get(tabKey);
    container.add(rawPanel);
    container.revalidate();
    container.repaint();

    int index = tabbedPane.indexOfComponent(tabPanel);
    if (index != -1) {
      tabbedPane.setSelectedIndex(index);
    }

    SwingTaskRunner.runWithProgress(
        rawPanel,
        executor,
        rawPanel::initializeUI,
        e -> {
          removeChartCard(tabKey, rawPanel);
          if (onComplete != null) onComplete.run();
        },
        () -> createProgressBar("Loading, please wait..."),
        () -> {
          if (onComplete != null) onComplete.run();
        }
    );
  }

  private JPanel getOrCreateTabPanel(String tabKey, String globalKey, int connectionId) {
    return tabPanels.computeIfAbsent(tabKey, k -> {
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
      panel.putClientProperty("connectionId", connectionId);
      LaF.setBackgroundColor(REPORT, panel);

      PainlessGridBag gbl = new PainlessGridBag(panel, PGHelper.getPGConfig(), false);
      gbl.row().cellXYRemainder(scrollPane).fillXY();
      gbl.done();

      tabContainers.put(tabKey, container);

      tabbedPane.addTab(tabKey, panel);

      int idx = tabbedPane.indexOfComponent(panel);
      if (idx >= 0) {
        tabbedPane.setTabComponentAt(idx, createClosableTabHeader(tabKey, globalKey));
      }
      panel.putClientProperty("tabKey", tabKey);

      connectionTabKeys.computeIfAbsent(connectionId, id -> new ArrayList<>()).add(tabKey);

      return panel;
    });
  }

  public void removeChartCard(String tabKey, JComponent component) {
    JXTaskPaneContainer container = tabContainers.get(tabKey);
    if (container != null && component != null) {
      container.remove(component);
      container.revalidate();
      container.repaint();

      if (container.getComponentCount() == 0) {
        JPanel panel = tabPanels.get(tabKey);
        if (panel != null) {
          Integer connectionId = (Integer) panel.getClientProperty("connectionId");
          if (connectionId != null) {
            List<String> keys = connectionTabKeys.get(connectionId);
            if (keys != null) {
              keys.remove(tabKey);
              if (keys.isEmpty()) {
                connectionTabKeys.remove(connectionId);
              }
            }
          }
        }

        tabbedPane.remove(tabPanels.get(tabKey));
        tabPanels.remove(tabKey);
        tabContainers.remove(tabKey);
      }
    }
  }

  public void removeAllChartsByConnectionId(int connectionId) {
    List<String> tabKeysToRemove = connectionTabKeys.remove(connectionId);
    if (tabKeysToRemove == null || tabKeysToRemove.isEmpty()) {
      return;
    }

    for (String tabKey : new ArrayList<>(tabKeysToRemove)) {
      JXTaskPaneContainer container = tabContainers.remove(tabKey);
      if (container != null) {
        container.removeAll();
      }

      JPanel panel = tabPanels.remove(tabKey);
      if (panel != null) {
        tabbedPane.remove(panel);
      }
    }

    tabbedPane.revalidate();
    tabbedPane.repaint();
  }

  public void clearAllCharts() {
    tabbedPane.removeAll();
    tabPanels.clear();
    tabContainers.clear();
    connectionTabKeys.clear();
  }

  private JComponent createClosableTabHeader(String tabKey, String globalKey) {
    JPanel pnl = new JPanel(new java.awt.BorderLayout());
    pnl.setOpaque(false);

    javax.swing.JLabel lbl = new javax.swing.JLabel(tabKey);
    lbl.setOpaque(false);

    javax.swing.JButton btn = new javax.swing.JButton("x");
    btn.setFocusable(false);
    btn.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 6));
    btn.setContentAreaFilled(false);

    btn.addActionListener(e -> {
      if (tabCloseListener != null) {
        tabCloseListener.accept(tabKey, globalKey);
      }
    });

    pnl.add(lbl, java.awt.BorderLayout.CENTER);
    pnl.add(btn, java.awt.BorderLayout.EAST);
    return pnl;
  }

  public void removeTabByKey(String tabKey) {
    JPanel panel = tabPanels.remove(tabKey);
    tabContainers.remove(tabKey);

    if (panel != null) {
      Integer connectionId = (Integer) panel.getClientProperty("connectionId");
      if (connectionId != null) {
        List<String> keys = connectionTabKeys.get(connectionId);
        if (keys != null) {
          keys.remove(tabKey);
          if (keys.isEmpty()) connectionTabKeys.remove(connectionId);
        }
      }
      tabbedPane.remove(panel);
      tabbedPane.revalidate();
      tabbedPane.repaint();
    }
  }

  public void removeAllChartsByGlobalKey(String globalKey) {
    List<String> toRemove = new ArrayList<>();
    tabPanels.forEach((tabKey, panel) -> {
      String gk = (String) panel.getClientProperty("globalKey");
      if (globalKey != null && globalKey.equals(gk)) {
        toRemove.add(tabKey);
      }
    });
    toRemove.forEach(this::removeTabByKey);
  }

  public String getSelectedGlobalKeyOrNull() {
    int selectedIndex = tabbedPane.getSelectedIndex();
    if (selectedIndex < 0) return null;

    JPanel selectedPanel = (JPanel) tabbedPane.getComponentAt(selectedIndex);
    return (String) selectedPanel.getClientProperty("globalKey");
  }

  public boolean hasTabs() {
    return tabbedPane.getTabCount() > 0;
  }
}