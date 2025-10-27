package ru.dimension.ui.component.module.chart.report;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
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
import ru.dimension.ui.component.panel.popup.DescriptionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;

@Log4j2
public class ReportChartView extends BaseUnitView {

  private int configDividerLocation = 32;
  private int chartDividerLocation = 250;

  private final MessageBroker.Component component;

  @Getter
  private final FunctionPanel historyFunctionPanel;
  @Getter
  private final TimeRangeFunctionPanel historyTimeRangeFunctionPanel;
  @Getter
  private final NormFunctionPanel historyNormFunctionPanel;
  @Getter
  private final HistoryRangePanel historyRangePanel;
  @Getter
  private final LegendPanel historyLegendPanel;
  @Getter
  private final FilterPanel historyFilterPanel;
  @Getter
  private final ActionPanel historyActionPanel;
  @Getter
  private final DescriptionPanel historyDescriptionPanel;

  @Getter
  private final HistoryConfigBlock historyConfigBlock;

  public ReportChartView(MessageBroker.Component component,
                         DescriptionPanel descriptionPanel) {
    super(LayoutMode.CONFIG_CHART_DETAIL);
    this.component = component;

    this.historyTimeRangeFunctionPanel = new TimeRangeFunctionPanel();
    this.historyNormFunctionPanel = new NormFunctionPanel();
    this.historyFunctionPanel = new FunctionPanel(getBoldLabel("Group: "), historyTimeRangeFunctionPanel, historyNormFunctionPanel);
    this.historyRangePanel = new HistoryRangePanel(getBoldLabel("Range: "));
    this.historyLegendPanel = new LegendPanel(getBoldLabel("Legend: "));
    this.historyFilterPanel = new FilterPanel(component);
    this.historyActionPanel = new ActionPanel(component);
    this.historyDescriptionPanel = descriptionPanel;

    this.historyConfigBlock = new HistoryConfigBlock(historyFunctionPanel,
                                                     historyRangePanel,
                                                     historyLegendPanel,
                                                     historyFilterPanel,
                                                     historyActionPanel,
                                                     historyDescriptionPanel);

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

  public java.awt.Component getRootComponent() {
    return getConfigChartSplitPane();
  }

  public JPanel getHistoryChartPanel() {
    return getChartPanel();
  }

  public JPanel getHistoryDetailPanel() {
    return getDetailPanel().orElseGet(() -> new JPanel(new BorderLayout()));
  }

  public JSplitPane getHistoryChartDetailSplitPane() {
    return getChartDetailSplitPane();
  }

  public JSplitPane getHistoryConfigChartDetail() {
    return getConfigChartSplitPane();
  }
}