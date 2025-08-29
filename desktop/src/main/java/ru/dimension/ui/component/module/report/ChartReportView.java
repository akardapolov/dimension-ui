package ru.dimension.ui.component.module.report;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.FunctionPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.helper.GUIHelper;

@Log4j2
@Data
public class ChartReportView extends JPanel {

  private FunctionPanel historyFunctionPanel;
  private HistoryRangePanel historyRangePanel;
  private LegendPanel historyLegendPanel;

  private JSplitPane configChartSplitPane;
  private JSplitPane chartDetailSplitPane;

  @Getter
  private JPanel historyChartPanel;
  @Getter
  private JPanel historyDetailPanel;

  public ChartReportView() {
    setLayout(new BorderLayout());
    setBorder(new EtchedBorder());

    historyFunctionPanel = new FunctionPanel(GUIHelper.getLabel("Group: "));
    historyRangePanel = new HistoryRangePanel(GUIHelper.getLabel("Range: "));
    historyLegendPanel = new LegendPanel(GUIHelper.getLabel("Legend: "));

    JPanel configPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(2, 2, 2, 2);

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 2.0 / 15;
    configPanel.add(historyFunctionPanel, gbc);

    gbc.gridx = 1;
    gbc.weightx = 2.0 / 15;
    configPanel.add(historyRangePanel, gbc);

    gbc.gridx = 2;
    gbc.weightx = 1.0 / 15;
    configPanel.add(historyLegendPanel, gbc);

    historyChartPanel = new JPanel(new BorderLayout());
    historyDetailPanel = new JPanel(new BorderLayout());

    chartDetailSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    chartDetailSplitPane.setTopComponent(historyChartPanel);
    chartDetailSplitPane.setBottomComponent(historyDetailPanel);
    chartDetailSplitPane.setResizeWeight(0.7);

    configChartSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    configChartSplitPane.setTopComponent(configPanel);
    configChartSplitPane.setBottomComponent(chartDetailSplitPane);
    configChartSplitPane.setResizeWeight(0.1);

    add(configChartSplitPane, BorderLayout.CENTER);
  }

  public void setDetailVisible(boolean visible) {
    if (visible) {
      chartDetailSplitPane.setBottomComponent(historyDetailPanel);
      chartDetailSplitPane.setDividerLocation(0.7);
    } else {
      chartDetailSplitPane.setBottomComponent(null);
    }
    chartDetailSplitPane.revalidate();
    chartDetailSplitPane.repaint();
  }
}
