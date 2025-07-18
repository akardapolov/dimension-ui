package ru.dimension.ui.view.panel.adhoc;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.GridLayout;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.view.ProcessTypeWorkspace;
import ru.dimension.ui.view.panel.RangeChartHistoryPanel;
import ru.dimension.ui.view.structure.workspace.handler.DetailsControlPanelHandler;
import ru.dimension.ui.view.structure.workspace.query.DetailsControlPanel;
import ru.dimension.ui.view.tab.AdHocTab;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class AdHocPanel extends JPanel {

  private final JSplitPane mainSplitPane;
  private final JSplitPane visualizeSplitPane;
  private final JPanel analyzePanel;
  private final JPanel bottomPanel;
  private final JPanel topPanel;
  private final JXTitledSeparator tableNameAdHocTitle;
  private final RangeChartHistoryPanel rangeChartHistoryPanel;
  private final DetailsControlPanel detailsControlPanel;
  private final DetailsControlPanelHandler detailsControlPanelHandler;
  private final AdHocTab adHocTab;

  private boolean clearFlag;

  @Inject
  public AdHocPanel(@Named("tableNameAdHocTitle") JXTitledSeparator tableNameAdHocTitle) {

    this.tableNameAdHocTitle = tableNameAdHocTitle;

    this.mainSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 150);
    this.mainSplitPane.setDividerLocation(150);
    this.mainSplitPane.setResizeWeight(0.5);

    this.visualizeSplitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 250);
    this.analyzePanel = getPanelLaF();

    this.cleanVisualizeAndAnalyze();

    this.topPanel = new JPanel();
    this.bottomPanel = new JPanel(new GridLayout(1, 1));

    bottomPanel.setBorder(new EtchedBorder());
    topPanel.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, bottomPanel);

    this.adHocTab = new AdHocTab();
    this.adHocTab.add(ProcessTypeWorkspace.VISUALIZE.getName(), visualizeSplitPane);
    this.adHocTab.add(ProcessTypeWorkspace.ANALYZE.getName(), analyzePanel);
    this.bottomPanel.add(adHocTab, JSplitPane.BOTTOM);

    this.rangeChartHistoryPanel = new RangeChartHistoryPanel();
    this.rangeChartHistoryPanel.setEnabled(true);

    this.detailsControlPanel = new DetailsControlPanel();
    this.detailsControlPanelHandler = new DetailsControlPanelHandler(this.detailsControlPanel);

    mainSplitPane.setTopComponent(fillTopPanel());
    mainSplitPane.setBottomComponent(bottomPanel);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(mainSplitPane).fillXY();
    gbl.done();

    clearFlag = false;
  }

  public void cleanVisualizeAndAnalyze() {
    JPanel jPanelVisualizeStacked = new JPanel();
    JPanel jPanelVisualizeDetail = new JPanel();
    LaF.setBackgroundConfigPanel(CHART_PANEL, jPanelVisualizeStacked);
    LaF.setBackgroundConfigPanel(CHART_PANEL, jPanelVisualizeDetail);

    this.visualizeSplitPane.add(jPanelVisualizeStacked, JSplitPane.TOP);
    this.visualizeSplitPane.add(jPanelVisualizeDetail, JSplitPane.BOTTOM);
    this.visualizeSplitPane.setDividerLocation(250);

    this.analyzePanel.removeAll();
    this.analyzePanel.repaint();
    this.analyzePanel.revalidate();
  }

  private JPanel fillTopPanel() {
    JPanel panelEntities = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(panelEntities, PGHelper.getPGConfig(0), false);

    gbl.row()
        .cellX(tableNameAdHocTitle, 9).fillX(9);

    gbl.row()
        .cellX(new JXTitledSeparator("Details"), 4).fillX(4)
        .cellX(new JXTitledSeparator("History"), 5).fillX(5);

    gbl.row()
        .cellX(detailsControlPanel, 4).fillXY(4, 3)
        .cellX(rangeChartHistoryPanel, 5).fillXY(5, 3);

    gbl.done();
    return panelEntities;
  }

  private JPanel getPanelLaF() {
    JPanel panel = new JPanel();
    panel.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, panel);
    return panel;
  }
}
