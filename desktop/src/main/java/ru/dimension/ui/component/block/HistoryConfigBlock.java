package ru.dimension.ui.component.block;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.util.List;
import javax.swing.JButton;
import javax.swing.border.EtchedBorder;
import ru.dimension.ui.component.block.base.AbstractConfigBlock;
import ru.dimension.ui.component.panel.FunctionPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.DescriptionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.laf.LaF;

public class HistoryConfigBlock extends AbstractConfigBlock {

  public HistoryConfigBlock(FunctionPanel functionPanel,
                            HistoryRangePanel historyPanel,
                            LegendPanel legendPanel,
                            FilterPanel filterPanel,
                            ActionPanel actionPanel) {
    this(functionPanel, historyPanel, legendPanel, filterPanel, actionPanel, null, null);
  }

  public HistoryConfigBlock(FunctionPanel functionPanel,
                            HistoryRangePanel historyPanel,
                            LegendPanel legendPanel,
                            FilterPanel filterPanel,
                            ActionPanel actionPanel,
                            DescriptionPanel descriptionPanel) {
    this(functionPanel, historyPanel, legendPanel, filterPanel, actionPanel, descriptionPanel, null);
  }

  public HistoryConfigBlock(FunctionPanel functionPanel,
                            HistoryRangePanel historyPanel,
                            LegendPanel legendPanel,
                            FilterPanel filterPanel,
                            ActionPanel actionPanel,
                            JButton detailsButton) {
    this(functionPanel, historyPanel, legendPanel, filterPanel, actionPanel, null, detailsButton);
  }

  public HistoryConfigBlock(FunctionPanel functionPanel,
                            HistoryRangePanel historyPanel,
                            LegendPanel legendPanel,
                            FilterPanel filterPanel,
                            ActionPanel actionPanel,
                            DescriptionPanel descriptionPanel,
                            JButton detailsButton) {
    super(AbstractConfigBlock.Spec.builder()
              .leftItems(List.of(
                  AbstractConfigBlock.Item.builder().component(functionPanel).weightx(2.0 / 15).build(),
                  AbstractConfigBlock.Item.builder().component(historyPanel).weightx(2.0 / 15).build(),
                  AbstractConfigBlock.Item.builder().component(legendPanel).weightx(1.0 / 15).build()
              ))
              .rightItems(descriptionPanel == null
                              ? List.of(filterPanel, actionPanel)
                              : List.of(filterPanel, actionPanel, descriptionPanel))
              .trailing(detailsButton)
              .build());

    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);
  }
}