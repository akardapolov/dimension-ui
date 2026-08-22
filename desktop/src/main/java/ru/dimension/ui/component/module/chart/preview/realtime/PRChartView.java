package ru.dimension.ui.component.module.chart.preview.realtime;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.block.RealTimeConfigBlock;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.base.BaseUnitView;
import ru.dimension.ui.component.module.preview.spi.PreviewMode;
import ru.dimension.ui.component.panel.FunctionPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.popup.action.ActionPanel;
import ru.dimension.ui.component.panel.popup.filter.FilterPanel;
import ru.dimension.ui.component.panel.range.RealTimeRangePanel;

@Log4j2
@Getter
public class PRChartView extends BaseUnitView {
  private static final Dimension dimension = new Dimension(100, 200);

  private final MessageBroker.Component component;

  private final FunctionPanel realTimeFunctionPanel;
  private final RealTimeRangePanel realTimeRangePanel;
  private final LegendPanel realTimeLegendPanel;
  private final FilterPanel realTimeFilterPanel;
  private final ActionPanel realTimeActionPanel;
  private final JButton detailsButton;

  private final PreviewMode previewMode;

  private final RealTimeConfigBlock realTimeConfigBlock;

  public PRChartView(MessageBroker.Component component, PreviewMode previewMode) {
    super(LayoutMode.CONFIG_CHART_ONLY);
    this.component = component;
    this.previewMode = previewMode;

    this.realTimeFunctionPanel = new FunctionPanel(getBoldLabel("Group: "));
    this.realTimeRangePanel = new RealTimeRangePanel(getBoldLabel("Range: "));
    this.realTimeLegendPanel = new LegendPanel(getBoldLabel("Legend: "));
    this.realTimeFilterPanel = new FilterPanel(component);
    this.realTimeActionPanel = new ActionPanel(component);
    this.detailsButton = new JButton("Details");
    syncButtonSize(realTimeActionPanel.getButton(), detailsButton);

    if (previewMode == PreviewMode.PREVIEW) {
      this.realTimeConfigBlock = new RealTimeConfigBlock(
          realTimeFunctionPanel,
          realTimeRangePanel,
          realTimeLegendPanel,
          realTimeActionPanel,
          detailsButton
      );
    } else {
      this.realTimeConfigBlock = new RealTimeConfigBlock(
          realTimeFunctionPanel,
          realTimeRangePanel,
          realTimeLegendPanel
      );
    }

    getConfigPanel().setLayout(new BorderLayout());
    getConfigPanel().add(realTimeConfigBlock, BorderLayout.CENTER);

    getConfigChartSplitPane().setPreferredSize(dimension);

    SwingUtilities.invokeLater(() -> {
      int h = realTimeConfigBlock.getPreferredSize().height;
      getConfigChartSplitPane().setDividerLocation(h);
    });
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
      int h = realTimeConfigBlock.getPreferredSize().height;
      getConfigChartSplitPane().setDividerLocation(h);
    } else {
      getConfigChartSplitPane().getTopComponent().setVisible(false);
      getConfigChartSplitPane().setDividerLocation(0); // Move divider to top
    }
    getConfigChartSplitPane().revalidate();
    getConfigChartSplitPane().repaint();
  }

  public void setDetailsButtonAction(ActionListener listener) {
    detailsButton.addActionListener(listener);
  }

  private void syncButtonSize(JButton actionButton, JButton detailsButton) {
    Dimension actionSize = actionButton.getPreferredSize();
    Dimension detailsSize = detailsButton.getPreferredSize();
    Dimension sameSize = new Dimension(
        Math.max(actionSize.width, detailsSize.width),
        Math.max(actionSize.height, detailsSize.height)
    );
    actionButton.setPreferredSize(sameSize);
    detailsButton.setPreferredSize(sameSize);
  }
}