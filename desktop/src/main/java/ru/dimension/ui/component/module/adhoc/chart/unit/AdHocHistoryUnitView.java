package ru.dimension.ui.component.module.adhoc.chart.unit;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JLabel;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.block.HistoryConfigBlock;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.base.BaseUnitView;
import ru.dimension.ui.component.panel.FunctionPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.function.NormFunctionPanel;
import ru.dimension.ui.component.panel.function.TimeRangeFunctionPanel;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;

@Log4j2
@Getter
public class AdHocHistoryUnitView extends BaseUnitView {

  private int configDividerLocation = 32;
  private int chartDividerLocation = 250;

  private final FunctionPanel historyFunctionPanel;
  private final TimeRangeFunctionPanel historyTimeRangeFunctionPanel;
  private final NormFunctionPanel historyNormFunctionPanel;
  private final HistoryRangePanel historyRangePanel;
  private final LegendPanel historyLegendPanel;
  private final FilterPanel historyFilterPanel;
  private final ActionPanel historyActionPanel;

  private final HistoryConfigBlock historyConfigBlock;

  public AdHocHistoryUnitView() {
    super(LayoutMode.CONFIG_CHART_DETAIL);

    this.historyTimeRangeFunctionPanel = new TimeRangeFunctionPanel();
    this.historyNormFunctionPanel = new NormFunctionPanel();
    this.historyFunctionPanel = new FunctionPanel(getBoldLabel("Group: "),
                                                  historyTimeRangeFunctionPanel,
                                                  historyNormFunctionPanel);
    this.historyRangePanel = new HistoryRangePanel(getBoldLabel("Range: "));
    this.historyLegendPanel = new LegendPanel(getBoldLabel("Legend: "));
    this.historyFilterPanel = new FilterPanel(MessageBroker.Component.ADHOC);
    this.historyActionPanel = new ActionPanel(MessageBroker.Component.ADHOC);

    this.historyConfigBlock = new HistoryConfigBlock(
        historyFunctionPanel,
        historyRangePanel,
        historyLegendPanel,
        historyFilterPanel,
        historyActionPanel
    );

    getConfigPanel().setLayout(new BorderLayout());
    getConfigPanel().add(historyConfigBlock, BorderLayout.CENTER);

    getConfigChartSplitPane().setDividerLocation(configDividerLocation);
    getChartDetailSplitPane().setDividerLocation(chartDividerLocation);
  }

  private JLabel getBoldLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    return label;
  }
}