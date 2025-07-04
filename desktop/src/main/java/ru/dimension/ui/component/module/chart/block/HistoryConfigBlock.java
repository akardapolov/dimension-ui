package ru.dimension.ui.component.module.chart.block;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import ru.dimension.ui.component.module.config.panel.LegendPanel;
import ru.dimension.ui.component.module.config.panel.MetricFunctionPanel;
import ru.dimension.ui.component.module.config.panel.range.HistoryRangePanel;
import ru.dimension.ui.laf.LaF;

public class HistoryConfigBlock extends JPanel {
  private final MetricFunctionPanel metricFunctionPanel;
  private final HistoryRangePanel historyPanel;
  private final LegendPanel legendPanel;

  public HistoryConfigBlock(MetricFunctionPanel metricFunctionPanel,
                            HistoryRangePanel historyPanel,
                            LegendPanel legendPanel) {
    this.metricFunctionPanel = metricFunctionPanel;
    this.historyPanel = historyPanel;
    this.legendPanel = legendPanel;

    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);
    setLayout(new GridBagLayout());
    initComponents();
  }

  private void initComponents() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(2, 2, 2, 2);

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 2.0 / 15;
    add(metricFunctionPanel, gbc);

    gbc.gridx = 1;
    gbc.weightx = 2.0 / 15;
    add(historyPanel, gbc);

    gbc.gridx = 2;
    gbc.weightx = 1.0 / 15;
    add(legendPanel, gbc);

    gbc.gridx = 3;
    gbc.weightx = 10.0 / 15;
    gbc.fill = GridBagConstraints.BOTH;
    add(new JLabel(), gbc);
  }
}