package ru.dimension.ui.component.module.adhoc.chart;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.block.HistoryConfigBlock;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.MetricFunctionPanel;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.helper.GUIHelper;

@Data
@Log4j2
public class AdHocChartView extends JPanel {
  private static Dimension dimension = new Dimension(100, 500);

  private int configDividerLocation = 32;
  private int chartDividerLocation = 250;

  private int lastHistoryConfigDividerLocation = 32;
  private int lastHistoryChartDividerLocation = 250;

  @Getter
  private JPanel tabbedPane;

  private MetricFunctionPanel historyMetricFunctionPanel;
  private HistoryRangePanel historyRangePanel;
  private LegendPanel historyLegendPanel;
  private FilterPanel historyFilterPanel;
  private ActionPanel historyActionPanel;

  private HistoryConfigBlock historyConfigBlock;

  @Getter
  private JPanel historyChartPanel;
  private JPanel historyDetailPanel;

  @Getter
  private JSplitPane historyChartDetailSplitPane;
  private JSplitPane historyConfigChartDetail;

  public AdHocChartView() {
    tabbedPane = new JPanel(new BorderLayout());
    tabbedPane.setBorder(new EtchedBorder());

    historyMetricFunctionPanel = new MetricFunctionPanel(getLabel("Group: "));
    historyRangePanel = new HistoryRangePanel(getLabel("Range: "));
    historyLegendPanel = new LegendPanel(getLabel("Legend: "));
    historyFilterPanel = new FilterPanel();
    historyActionPanel = new ActionPanel();

    historyConfigBlock = new HistoryConfigBlock(
        historyMetricFunctionPanel,
        historyRangePanel,
        historyLegendPanel,
        historyFilterPanel,
        historyActionPanel
    );

    historyChartPanel = new JPanel(new BorderLayout());
    historyDetailPanel = new JPanel(new BorderLayout());

    tabbedPane.add(createHistoryTab());
  }

  private JLabel getLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }

  private JSplitPane createHistoryTab() {
    historyConfigChartDetail = createBaseSplitPane();
    historyChartDetailSplitPane = createChartDetailSplitPane();

    historyChartDetailSplitPane.setTopComponent(historyChartPanel);
    historyChartDetailSplitPane.setBottomComponent(historyDetailPanel);

    historyConfigChartDetail.setTopComponent(historyConfigBlock);
    historyConfigChartDetail.setBottomComponent(historyChartDetailSplitPane);

    historyConfigChartDetail.setDividerLocation(lastHistoryConfigDividerLocation);
    historyChartDetailSplitPane.setDividerLocation(lastHistoryChartDividerLocation);

    return historyConfigChartDetail;
  }

  private JSplitPane createBaseSplitPane() {
    JSplitPane splitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, configDividerLocation);
    splitPane.setDividerLocation(configDividerLocation);
    splitPane.setResizeWeight(0.5);
    splitPane.setPreferredSize(dimension);
    splitPane.setMaximumSize(dimension);
    return splitPane;
  }

  private JSplitPane createChartDetailSplitPane() {
    JSplitPane splitPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, chartDividerLocation);
    splitPane.setDividerLocation(chartDividerLocation);
    splitPane.setResizeWeight(0.5);
    return splitPane;
  }

  public void setHistoryDetailVisible(boolean visible) {
    if (visible) {
      historyChartDetailSplitPane.getBottomComponent().setVisible(true);
      historyChartDetailSplitPane.setDividerLocation(lastHistoryChartDividerLocation);
    } else {
      historyChartDetailSplitPane.getBottomComponent().setVisible(false);
      historyChartDetailSplitPane.setDividerLocation(1.0);
    }

    historyChartDetailSplitPane.revalidate();
    historyChartDetailSplitPane.repaint();
    historyConfigChartDetail.revalidate();
    historyConfigChartDetail.repaint();
  }
}