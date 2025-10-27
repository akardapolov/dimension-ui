package ru.dimension.ui.component.module.chart.main.unit;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.block.RealTimeConfigBlock;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.base.BaseUnitView;
import ru.dimension.ui.component.panel.FunctionPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.range.RealTimeRangePanel;

@Log4j2
public class RealtimeUnitView extends BaseUnitView {

  private int configDividerLocation = 32;
  private int chartDividerLocation = 250;

  private final MessageBroker.Component component;

  @Getter
  private final FunctionPanel realTimeFunctionPanel;
  @Getter
  private final RealTimeRangePanel realTimeRangePanel;
  @Getter
  private final LegendPanel realTimeLegendPanel;
  @Getter
  private final FilterPanel realTimeFilterPanel;
  @Getter
  private final ActionPanel realTimeActionPanel;

  @Getter
  private final RealTimeConfigBlock realTimeConfigBlock;

  public RealtimeUnitView(MessageBroker.Component component) {
    super(LayoutMode.CONFIG_CHART_DETAIL);
    this.component = component;

    this.realTimeFunctionPanel = new FunctionPanel(getBoldLabel("Group: "));
    this.realTimeRangePanel = new RealTimeRangePanel(getBoldLabel("Range: "));
    this.realTimeLegendPanel = new LegendPanel(getBoldLabel("Legend: "));
    this.realTimeFilterPanel = new FilterPanel(component);
    this.realTimeActionPanel = new ActionPanel(component);

    this.realTimeConfigBlock = new RealTimeConfigBlock(realTimeFunctionPanel,
                                                       realTimeRangePanel,
                                                       realTimeLegendPanel,
                                                       realTimeFilterPanel,
                                                       realTimeActionPanel);

    getConfigPanel().setLayout(new BorderLayout());
    getConfigPanel().add(realTimeConfigBlock, BorderLayout.CENTER);

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

  public JPanel getRealTimeChartPanel() {
    return getChartPanel();
  }

  public JSplitPane getRealTimeChartDetailSplitPane() {
    return getChartDetailSplitPane();
  }

  public JSplitPane getRealTimeConfigChartDetail() {
    return getConfigChartSplitPane();
  }
}