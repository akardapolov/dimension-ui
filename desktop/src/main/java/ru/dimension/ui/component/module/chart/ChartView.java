package ru.dimension.ui.component.module.chart;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.model.AnalyzeTabType;
import ru.dimension.ui.component.model.PanelTabType;
import ru.dimension.ui.component.block.HistoryConfigBlock;
import ru.dimension.ui.component.block.RealTimeConfigBlock;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.MetricFunctionPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.component.panel.range.RealTimeRangePanel;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.view.analyze.timeseries.AnalyzeAnomalyPanel;
import ru.dimension.ui.view.analyze.timeseries.AnalyzeForecastPanel;

@Data
@Log4j2
public class ChartView extends JPanel {
  private static Dimension dimension = new Dimension(100, 500);

  private int configDividerLocation = 32;
  private int chartDividerLocation = 250;

  private int lastRealTimeConfigDividerLocation = 32;
  private int lastRealTimeChartDividerLocation = 250;
  private int lastHistoryConfigDividerLocation = 32;
  private int lastHistoryChartDividerLocation = 250;

  private JTabbedPane tabbedPane;

  private MetricFunctionPanel realTimeMetricFunctionPanel;
  private RealTimeRangePanel realTimeRangePanel;
  private LegendPanel realTimeLegendPanel;
  private FilterPanel realTimeFilterPanel;
  private ActionPanel realTimeActionPanel;

  private MetricFunctionPanel historyMetricFunctionPanel;
  private HistoryRangePanel historyRangePanel;
  private LegendPanel historyLegendPanel;
  private FilterPanel historyFilterPanel;
  private ActionPanel historyActionPanel;

  private RealTimeConfigBlock realTimeConfigBlock;
  private HistoryConfigBlock historyConfigBlock;

  @Getter
  private JPanel realTimeChartPanel;
  private JPanel realTimeDetailPanel;
  @Getter
  private JPanel historyChartPanel;
  private JPanel historyDetailPanel;

  @Getter
  private JTabbedPane analyzeTabbedPane;
  private AnalyzeAnomalyPanel analyzeAnomalyPanel;
  private AnalyzeForecastPanel analyzeForecastPanel;

  private JSplitPane realTimeChartDetailSplitPane;
  private JSplitPane historyChartDetailSplitPane;

  private JSplitPane realTimeConfigChartDetail;
  private JSplitPane historyConfigChartDetail;

  public ChartView() {
    tabbedPane = new JTabbedPane();
    tabbedPane.setBorder(new EtchedBorder());

    realTimeMetricFunctionPanel = new MetricFunctionPanel(getLabel("Group: "));
    realTimeRangePanel = new RealTimeRangePanel(getLabel("Range: "));
    realTimeLegendPanel = new LegendPanel(getLabel("Legend: "));
    realTimeFilterPanel = new FilterPanel();
    realTimeActionPanel = new ActionPanel();

    realTimeConfigBlock = new RealTimeConfigBlock(realTimeMetricFunctionPanel,
                                                  realTimeRangePanel,
                                                  realTimeLegendPanel,
                                                  realTimeFilterPanel,
                                                  realTimeActionPanel);

    historyMetricFunctionPanel = new MetricFunctionPanel(getLabel("Group: "));
    historyRangePanel = new HistoryRangePanel(getLabel("Range: "));
    historyLegendPanel = new LegendPanel(getLabel("Legend: "));
    historyFilterPanel = new FilterPanel();
    historyActionPanel = new ActionPanel();

    historyConfigBlock = new HistoryConfigBlock(historyMetricFunctionPanel,
                                                historyRangePanel,
                                                historyLegendPanel,
                                                historyFilterPanel,
                                                historyActionPanel);

    realTimeChartPanel = new JPanel(new BorderLayout());
    realTimeDetailPanel = new JPanel(new BorderLayout());

    historyChartPanel = new JPanel(new BorderLayout());
    historyDetailPanel = new JPanel(new BorderLayout());

    analyzeTabbedPane = new JTabbedPane();
    analyzeTabbedPane.addTab(AnalyzeTabType.ANOMALY.getName(), new JPanel());
    analyzeTabbedPane.addTab(AnalyzeTabType.FORECAST.getName(), new JPanel());

    // Create tabs
    tabbedPane.addTab(PanelTabType.REALTIME.getName(), createRealTimeTab());
    tabbedPane.addTab(PanelTabType.HISTORY.getName(), createHistoryTab());
  }

  private JLabel getLabel(String text) {
    JLabel label = new JLabel(text);
    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
    return label;
  }

  private JSplitPane createRealTimeTab() {
    realTimeConfigChartDetail = createBaseSplitPane();
    realTimeChartDetailSplitPane = createChartDetailSplitPane();

    GUIHelper.addToJSplitPane(realTimeChartDetailSplitPane, realTimeChartPanel, JSplitPane.TOP);
    GUIHelper.addToJSplitPane(realTimeChartDetailSplitPane, realTimeDetailPanel, JSplitPane.BOTTOM);

    realTimeConfigChartDetail.setTopComponent(realTimeConfigBlock);
    realTimeConfigChartDetail.setBottomComponent(realTimeChartDetailSplitPane);

    realTimeConfigChartDetail.setDividerLocation(configDividerLocation);
    realTimeChartDetailSplitPane.setDividerLocation(chartDividerLocation);

    return realTimeConfigChartDetail;
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

  public void setRealTimeDetailVisible(boolean visible) {
    if (visible) {
      realTimeChartDetailSplitPane.getBottomComponent().setVisible(true);
      realTimeChartDetailSplitPane.setDividerLocation(lastRealTimeChartDividerLocation);
    } else {
      realTimeChartDetailSplitPane.getBottomComponent().setVisible(false);
      realTimeChartDetailSplitPane.setDividerLocation(1.0); // Moves divider to bottom
    }

    // Update UI
    realTimeChartDetailSplitPane.revalidate();
    realTimeChartDetailSplitPane.repaint();
    realTimeConfigChartDetail.revalidate();
    realTimeConfigChartDetail.repaint();
  }

  public void setHistoryDetailVisible(boolean visible) {
    if (visible) {
      historyChartDetailSplitPane.getBottomComponent().setVisible(true);
      historyChartDetailSplitPane.setDividerLocation(lastHistoryChartDividerLocation);
    } else {
      historyChartDetailSplitPane.getBottomComponent().setVisible(false);
      historyChartDetailSplitPane.setDividerLocation(1.0); // Moves divider to bottom
    }

    historyChartDetailSplitPane.revalidate();
    historyChartDetailSplitPane.repaint();
    historyConfigChartDetail.revalidate();
    historyConfigChartDetail.repaint();
  }
}