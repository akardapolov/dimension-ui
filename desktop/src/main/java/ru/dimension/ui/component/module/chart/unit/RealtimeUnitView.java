package ru.dimension.ui.component.module.chart.unit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.block.RealTimeConfigBlock;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.panel.FunctionPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.range.RealTimeRangePanel;
import ru.dimension.ui.helper.GUIHelper;

@Log4j2
public class RealtimeUnitView extends JPanel {

  private static final Dimension DEFAULT_DIMENSION = new Dimension(100, 600);

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

  @Getter
  private final JPanel realTimeChartPanel;
  @Getter
  private final JPanel realTimeDetailPanel;

  @Getter
  private final JSplitPane realTimeChartDetailSplitPane;
  @Getter
  private final JSplitPane realTimeConfigChartDetail;

  public RealtimeUnitView(MessageBroker.Component component) {
    super(new BorderLayout());
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

    this.realTimeChartPanel = new JPanel(new BorderLayout());
    this.realTimeDetailPanel = new JPanel(new BorderLayout());

    this.realTimeConfigChartDetail = createBaseSplitPane();
    this.realTimeChartDetailSplitPane = createChartDetailSplitPane();

    GUIHelper.addToJSplitPane(realTimeChartDetailSplitPane, realTimeChartPanel, JSplitPane.TOP);
    GUIHelper.addToJSplitPane(realTimeChartDetailSplitPane, realTimeDetailPanel, JSplitPane.BOTTOM);

    realTimeConfigChartDetail.setTopComponent(realTimeConfigBlock);
    realTimeConfigChartDetail.setBottomComponent(realTimeChartDetailSplitPane);

    realTimeConfigChartDetail.setDividerLocation(configDividerLocation);
    realTimeChartDetailSplitPane.setDividerLocation(chartDividerLocation);
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
    return realTimeConfigChartDetail;
  }
}
