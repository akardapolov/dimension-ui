package ru.dimension.ui.component.module.chart.preview.history;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import javax.swing.JButton;
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
public class PHChartView extends BaseUnitView {
  private static final Dimension dimension = new Dimension(100, 200);

  private final MessageBroker.Component component;

  private final FunctionPanel historyFunctionPanel;
  private final TimeRangeFunctionPanel historyTimeRangeFunctionPanel;
  private final NormFunctionPanel historyNormFunctionPanel;
  private final HistoryRangePanel historyRangePanel;
  private final LegendPanel historyLegendPanel;
  private final FilterPanel historyFilterPanel;
  private final ActionPanel historyActionPanel;
  private final JButton detailsButton;

  private final HistoryConfigBlock historyConfigBlock;

  private final int configDividerLocation = 32;

  public PHChartView(MessageBroker.Component component) {
    super(LayoutMode.CONFIG_CHART_ONLY);
    this.component = component;

    this.historyTimeRangeFunctionPanel = new TimeRangeFunctionPanel();
    this.historyNormFunctionPanel = new NormFunctionPanel();
    this.historyFunctionPanel = new FunctionPanel(getBoldLabel("Group: "), historyTimeRangeFunctionPanel, historyNormFunctionPanel);
    this.historyRangePanel = new HistoryRangePanel(getBoldLabel("Range: "));
    this.historyLegendPanel = new LegendPanel(getBoldLabel("Legend: "));
    this.historyFilterPanel = new FilterPanel(component);
    this.historyActionPanel = new ActionPanel(component);
    this.detailsButton = new JButton("Details");

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
    getConfigChartSplitPane().setPreferredSize(dimension);
    getConfigChartSplitPane().setMaximumSize(dimension);
  }

  private JLabel getBoldLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    return label;
  }

  @Override
  public Component getRootComponent() {
    return getConfigChartSplitPane();
  }

  public void setChartConfigState(boolean visible) {
    if (visible) {
      getConfigChartSplitPane().getTopComponent().setVisible(true);
      getConfigChartSplitPane().setDividerLocation(configDividerLocation);
    } else {
      getConfigChartSplitPane().getTopComponent().setVisible(false);
      getConfigChartSplitPane().setDividerLocation(0);
    }
    getConfigChartSplitPane().revalidate();
    getConfigChartSplitPane().repaint();
  }

  public void setDetailsButtonAction(ActionListener listener) {
    detailsButton.addActionListener(listener);
  }
}