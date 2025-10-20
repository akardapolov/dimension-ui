package ru.dimension.ui.component.module.chart.unit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.block.HistoryConfigBlock;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.panel.FunctionPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.function.NormFunctionPanel;
import ru.dimension.ui.component.panel.function.TimeRangeFunctionPanel;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.helper.GUIHelper;

@Log4j2
public class HistoryUnitView extends JPanel {

  private static final Dimension DEFAULT_DIMENSION = new Dimension(100, 600);

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
  private final HistoryConfigBlock historyConfigBlock;

  @Getter
  private final JPanel historyChartPanel;
  @Getter
  private final JPanel historyDetailPanel;

  @Getter
  private final JSplitPane historyChartDetailSplitPane;
  @Getter
  private final JSplitPane historyConfigChartDetail;

  public HistoryUnitView(MessageBroker.Component component) {
    super(new BorderLayout());
    this.component = component;

    this.historyTimeRangeFunctionPanel = new TimeRangeFunctionPanel();
    this.historyNormFunctionPanel = new NormFunctionPanel();
    this.historyFunctionPanel = new FunctionPanel(getBoldLabel("Group: "), historyTimeRangeFunctionPanel, historyNormFunctionPanel);
    this.historyRangePanel = new HistoryRangePanel(getBoldLabel("Range: "));
    this.historyLegendPanel = new LegendPanel(getBoldLabel("Legend: "));
    this.historyFilterPanel = new FilterPanel(component);
    this.historyActionPanel = new ActionPanel(component);

    this.historyConfigBlock = new HistoryConfigBlock(historyFunctionPanel,
                                                     historyRangePanel,
                                                     historyLegendPanel,
                                                     historyFilterPanel,
                                                     historyActionPanel);

    this.historyChartPanel = new JPanel(new BorderLayout());
    this.historyDetailPanel = new JPanel(new BorderLayout());

    this.historyConfigChartDetail = createBaseSplitPane();
    this.historyChartDetailSplitPane = createChartDetailSplitPane();

    historyChartDetailSplitPane.setTopComponent(historyChartPanel);
    historyChartDetailSplitPane.setBottomComponent(historyDetailPanel);

    historyConfigChartDetail.setTopComponent(historyConfigBlock);
    historyConfigChartDetail.setBottomComponent(historyChartDetailSplitPane);

    historyConfigChartDetail.setDividerLocation(configDividerLocation);
    historyChartDetailSplitPane.setDividerLocation(chartDividerLocation);
  }

  private JLabel getBoldLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }

  private JSplitPane createBaseSplitPane() {
    JSplitPane splitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, configDividerLocation);
    splitPane.setDividerLocation(configDividerLocation);
    splitPane.setResizeWeight(0.5);
    splitPane.setPreferredSize(DEFAULT_DIMENSION);
    splitPane.setMaximumSize(DEFAULT_DIMENSION);
    return splitPane;
  }

  private JSplitPane createChartDetailSplitPane() {
    JSplitPane splitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, chartDividerLocation);
    splitPane.setDividerLocation(chartDividerLocation);
    splitPane.setResizeWeight(0.5);
    return splitPane;
  }

  public java.awt.Component getRootComponent() {
    return historyConfigChartDetail;
  }
}
