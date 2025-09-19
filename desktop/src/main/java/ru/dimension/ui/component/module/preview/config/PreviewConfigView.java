package ru.dimension.ui.component.module.preview.config;

import static ru.dimension.ui.helper.GUIHelper.getLabel;
import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import lombok.Getter;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.block.PreviewConfigBlock;
import ru.dimension.ui.component.panel.CollapseCardPanel;
import ru.dimension.ui.component.panel.ConfigShowHidePanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.range.RealTimeRangePanel;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;

@Getter
public class PreviewConfigView extends JPanel {
  private final PreviewConfigBlock previewConfigBlock;
  private final RealTimeRangePanel realTimeRangePanel;
  private final LegendPanel realTimeLegendPanel;
  private final ConfigShowHidePanel configShowHidePanel;
  private final CollapseCardPanel collapseCardPanel;

  public PreviewConfigView() {
    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    this.realTimeRangePanel = new RealTimeRangePanel(getLabel("Range: "));
    this.realTimeLegendPanel = new LegendPanel(getLabel("Legend: "));
    this.configShowHidePanel = new ConfigShowHidePanel(getLabel("Config: "));
    this.collapseCardPanel = new CollapseCardPanel(getLabel("Dashboard"));
    this.previewConfigBlock = new PreviewConfigBlock(realTimeRangePanel,
                                                     realTimeLegendPanel,
                                                     configShowHidePanel,
                                                     collapseCardPanel);

    buildLayout();
  }

  private void buildLayout() {
    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(previewConfigBlock).fillXY();
    gbl.done();
  }
}
