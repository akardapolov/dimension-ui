package ru.dimension.ui.component.module.preview.chart;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import lombok.Data;
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

@Data
@Log4j2
public class PreviewChartView extends JPanel {
  private static Dimension dimension = new Dimension(100, 200);

  private final MessageBroker.Component component;

  private int configDividerLocation = 32;

  private FunctionPanel realTimeFunctionPanel;
  private RealTimeRangePanel realTimeRangePanel;
  private LegendPanel realTimeLegendPanel;
  private FilterPanel realTimeFilterPanel;
  private ActionPanel realTimeActionPanel;

  @Getter
  private JButton detailsButton;

  private RealTimeConfigBlock realTimeConfigBlock;

  @Getter
  private JPanel realTimeChartPanel;

  @Getter
  private JSplitPane realTimeConfigChart;

  public PreviewChartView(MessageBroker.Component component) {
    this.component = component;
    this.setLayout(new BorderLayout());

    realTimeFunctionPanel = new FunctionPanel(getLabel("Group: "));
    realTimeRangePanel = new RealTimeRangePanel(getLabel("Range: "));
    realTimeLegendPanel = new LegendPanel(getLabel("Legend: "));
    realTimeFilterPanel = new FilterPanel(component);
    realTimeActionPanel = new ActionPanel(component);
    detailsButton = new JButton("Details");

    realTimeConfigBlock = new RealTimeConfigBlock(realTimeFunctionPanel,
                                                  realTimeRangePanel,
                                                  realTimeLegendPanel,
                                                  realTimeFilterPanel,
                                                  realTimeActionPanel,
                                                  detailsButton);

    realTimeChartPanel = new JPanel(new BorderLayout());
    realTimeConfigChart = createBaseSplitPane();

    realTimeConfigChart.setTopComponent(realTimeConfigBlock);
    realTimeConfigChart.setBottomComponent(realTimeChartPanel);
    realTimeConfigChart.setDividerLocation(configDividerLocation);

    this.add(realTimeConfigChart, BorderLayout.CENTER);
  }

  private JLabel getLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }

  private JSplitPane createBaseSplitPane() {
    JSplitPane splitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, configDividerLocation);
    splitPane.setDividerLocation(configDividerLocation);
    splitPane.setResizeWeight(0.5);
    splitPane.setPreferredSize(dimension);
    splitPane.setMaximumSize(dimension);
    return splitPane;
  }

  public void setChartConfigState(boolean visible) {
    if (visible) {
      realTimeConfigChart.getTopComponent().setVisible(true);
      realTimeConfigChart.setDividerLocation(configDividerLocation);
    } else {
      realTimeConfigChart.getTopComponent().setVisible(false);
      realTimeConfigChart.setDividerLocation(0.0); // Moves divider to top
    }

    // Update UI
    realTimeConfigChart.revalidate();
    realTimeConfigChart.repaint();
  }

  public void setDetailsButtonAction(ActionListener listener) {
    detailsButton.addActionListener(listener);
  }
}