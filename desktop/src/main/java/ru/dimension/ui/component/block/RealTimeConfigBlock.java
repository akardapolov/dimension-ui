package ru.dimension.ui.component.block;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.util.List;
import javax.swing.JButton;
import javax.swing.border.EtchedBorder;
import lombok.Getter;
import ru.dimension.ui.component.block.base.AbstractConfigBlock;
import ru.dimension.ui.component.panel.FunctionPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.range.RealTimeRangePanel;
import ru.dimension.ui.laf.LaF;

public class RealTimeConfigBlock extends AbstractConfigBlock {

  @Getter
  private final LegendPanel legendPanel;

  public RealTimeConfigBlock(FunctionPanel functionPanel,
                             RealTimeRangePanel realTimePanel,
                             LegendPanel legendPanel,
                             FilterPanel filterPanel,
                             ActionPanel actionPanel) {
    this(functionPanel, realTimePanel, legendPanel, filterPanel, actionPanel, null);
  }

  public RealTimeConfigBlock(FunctionPanel functionPanel,
                             RealTimeRangePanel realTimePanel,
                             LegendPanel legendPanel,
                             FilterPanel filterPanel,
                             ActionPanel actionPanel,
                             JButton detailsButton) {
    super(AbstractConfigBlock.Spec.builder()
              .leftItems(List.of(
                  AbstractConfigBlock.Item.builder().component(functionPanel).weightx(2.0 / 15).build(),
                  AbstractConfigBlock.Item.builder().component(realTimePanel).weightx(2.0 / 15).build(),
                  AbstractConfigBlock.Item.builder().component(legendPanel).weightx(1.0 / 15).build()
              ))
              .rightItems(List.of(filterPanel, actionPanel))
              .trailing(detailsButton)
              .build());

    this.legendPanel = legendPanel;

    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);
  }
}