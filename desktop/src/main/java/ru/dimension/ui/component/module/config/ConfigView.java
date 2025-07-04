package ru.dimension.ui.component.module.config;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.module.config.panel.CollapseCardPanel;
import ru.dimension.ui.component.module.config.panel.DetailShowHidePanel;
import ru.dimension.ui.component.module.config.panel.LegendPanel;
import ru.dimension.ui.component.module.config.panel.SwitchToTabPanel;
import ru.dimension.ui.component.module.config.panel.range.HistoryRangePanel;
import ru.dimension.ui.component.module.config.panel.range.RealTimeRangePanel;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;

@Log4j2
public class ConfigView extends JPanel {

  @Getter
  private final SwitchToTabPanel switchToTabPanel;
  @Getter
  private final RealTimeRangePanel realTimePanel;
  @Getter
  private final HistoryRangePanel historyPanel;
  @Getter
  private final LegendPanel legendPanel;
  @Getter
  private final DetailShowHidePanel detailShowHidePanel;
  @Getter
  private final CollapseCardPanel collapseCardPanel;

  public ConfigView() {
    this.switchToTabPanel = new SwitchToTabPanel();
    this.realTimePanel = new RealTimeRangePanel();
    this.historyPanel = new HistoryRangePanel();
    this.legendPanel = new LegendPanel();
    this.detailShowHidePanel = new DetailShowHidePanel();
    this.collapseCardPanel = new CollapseCardPanel();

    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    JXTitledSeparator selectTab = new JXTitledSeparator("Switch to");
    JXTitledSeparator realTime = new JXTitledSeparator("Real-time");
    JXTitledSeparator history = new JXTitledSeparator("History");
    JXTitledSeparator legend = new JXTitledSeparator("Legend");
    JXTitledSeparator detail = new JXTitledSeparator("Detail");
    JXTitledSeparator dashboard = new JXTitledSeparator("Dashboard");

    gbl.row()
        .cellX(selectTab, 2).fillX(2)
        .cellX(realTime, 2).fillX(2)
        .cellX(history, 2).fillX(2)
        .cellX(legend, 1).fillX(1)
        .cellX(detail, 1).fillX(1)
        .cellX(dashboard, 1).fillX(1)
        .cellXRemainder(new JXTitledSeparator("")).fillX();
    gbl.row()
        .cellX(switchToTabPanel, 2).fillX(2)
        .cellX(realTimePanel, 2).fillX(2)
        .cellX(historyPanel, 2).fillX(2)
        .cellX(legendPanel, 1).fillX(1)
        .cellX(detailShowHidePanel, 1).fillX(1)
        .cellX(collapseCardPanel, 1).fillX(1)
        .cellX(new JLabel(), 1).fillX(1);

    gbl.done();
  }
}
