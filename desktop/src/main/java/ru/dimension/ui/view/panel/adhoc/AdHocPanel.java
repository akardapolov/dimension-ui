package ru.dimension.ui.view.panel.adhoc;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.GridLayout;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableColumn;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.structure.workspace.handler.DetailsControlPanelHandler;
import ru.dimension.ui.view.structure.workspace.query.DetailsControlPanel;
import ru.dimension.ui.view.tab.AdHocTab;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.view.ProcessTypeWorkspace;
import ru.dimension.ui.view.panel.RangeChartHistoryPanel;

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
  private final JXTableCase timestampCase;
  private final RangeChartHistoryPanel rangeChartHistoryPanel;
  private final DetailsControlPanel detailsControlPanel;
  private final DetailsControlPanelHandler detailsControlPanelHandler;
  private final JXTableCase metricCase;
  private final JXTableCase columnCase;
  private final AdHocTab adHocTab;
  private final JXTableCase jxTableCaseRecent;

  private boolean clearFlag;

  @Inject
  public AdHocPanel(@Named("tableNameAdHocTitle") JXTitledSeparator tableNameAdHocTitle,
                    @Named("timestampAdHocCase") JXTableCase timestampCase,
                    @Named("metricAdHocCase") JXTableCase metricCase,
                    @Named("columnAdHocCase") JXTableCase columnCase) {

    this.tableNameAdHocTitle = tableNameAdHocTitle;
    this.timestampCase = timestampCase;
    this.metricCase = metricCase;
    this.columnCase = columnCase;

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

    this.jxTableCaseRecent = getJXTableCaseRecent();

    this.rangeChartHistoryPanel = new RangeChartHistoryPanel();
    this.rangeChartHistoryPanel.setEnabled(true);
    this.rangeChartHistoryPanel.getJToggleButtonCustom().setEnabled(false);

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
        .cellX(new JXTitledSeparator("Timestamp"), 2).fillX(4)
        .cellX(new JXTitledSeparator("Metrics"), 2).fillX(4)
        .cellX(new JXTitledSeparator("Columns"), 2).fillX(4)
        .cellX(new JXTitledSeparator("Details"), 2).fillX(4)
        .cellX(new JXTitledSeparator("History"), 1).fillX(1);

    gbl.row()
        .cellX(timestampCase.getJScrollPane(), 2).fillXY(4, 3)
        .cellX(metricCase.getJScrollPane(), 2).fillXY(4, 3)
        .cellX(columnCase.getJScrollPane(), 2).fillXY(4, 3)
        .cellX(detailsControlPanel, 2).fillXY(4, 3)
        .cellX(rangeChartHistoryPanel, 1).fillXY(1, 1);

    gbl.done();

    return panelEntities;
  }

  private JXTableCase getJXTableCaseRecent() {
    JXTableCase jxTableCase = GUIHelper.getJXTableCase(4, new String[]{"Created at", "S", "Begin", "End"});
    jxTableCase.getJxTable().getColumnExt(0).setVisible(false);

    TableColumn col = jxTableCase.getJxTable().getColumnModel().getColumn(0);
    col.setMinWidth(10);
    col.setMaxWidth(15);

    return jxTableCase;
  }

  private JPanel getPanelLaF() {
    JPanel panel = new JPanel();
    panel.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, panel);
    return panel;
  }
}
