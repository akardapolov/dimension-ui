// filename: src/main/java/ru/dimension/ui/view/detail/insight/InsightPanel.java
package ru.dimension.ui.view.detail.insight;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import lombok.Getter;
import ru.dimension.ui.component.chart.SCP;
import ru.dimension.ui.component.module.analyze.timeseries.AnalyzeAnomalyPanel;
import ru.dimension.ui.component.module.analyze.timeseries.AnalyzeForecastPanel;
import ru.dimension.ui.helper.GUIHelper;

public class InsightPanel extends JPanel {

  private static final Dimension DEFAULT_CHART_SIZE = new Dimension(100, 200);

  private final SCP chart;
  private final AnalyzeAnomalyPanel anomalyPanel;
  private final AnalyzeForecastPanel forecastPanel;
  @Getter
  private final JTabbedPane tabs = new JTabbedPane();

  private boolean anomalyPanelInitialized = false;
  private boolean forecastPanelInitialized = false;

  public InsightPanel(SCP chart) {
    this(chart, DEFAULT_CHART_SIZE);
  }

  public InsightPanel(SCP chart, Dimension preferredChartSize) {
    super(new BorderLayout());
    this.chart = chart;

    if (preferredChartSize != null) {
      chart.setPreferredSize(preferredChartSize);
      chart.setMaximumSize(preferredChartSize);
    }
    chart.setBorder(GUIHelper.getConfigureBorder(1));

    // Anomaly
    this.anomalyPanel = new AnalyzeAnomalyPanel(
        chart.getSeriesColorMap(),
        chart.getChartDataset()
    );
    this.anomalyPanel.setBorder(GUIHelper.getConfigureBorder(1));

    // Forecast
    this.forecastPanel = new AnalyzeForecastPanel(
        chart.getSeriesColorMap(),
        chart.getChartDataset()
    );
    this.forecastPanel.setBorder(GUIHelper.getConfigureBorder(1));

    // Tabs
    tabs.addTab("Data", chart);
    tabs.addTab("Anomaly", anomalyPanel);
    tabs.addTab("Forecast", forecastPanel);
    add(tabs, BorderLayout.CENTER);

    tabs.addChangeListener(e -> {
      int selectedIndex = tabs.getSelectedIndex();
      if (selectedIndex == 1 && !anomalyPanelInitialized) {
        anomalyPanel.init();
        anomalyPanelInitialized = true;
      } else if (selectedIndex == 2 && !forecastPanelInitialized) {
        forecastPanel.init();
        forecastPanelInitialized = true;
      }
    });
  }
}