package ru.dimension.ui.component.block;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.MetricFunctionPanel;
import ru.dimension.ui.component.panel.range.HistoryRangePanel;
import ru.dimension.ui.laf.LaF;

public class HistoryConfigBlock extends JPanel {
  private final MetricFunctionPanel metricFunctionPanel;
  private final HistoryRangePanel historyPanel;
  private final LegendPanel legendPanel;
  private final FilterPanel filterPanel;
  private final ActionPanel actionPanel;

  public HistoryConfigBlock(MetricFunctionPanel metricFunctionPanel,
                            HistoryRangePanel historyPanel,
                            LegendPanel legendPanel,
                            FilterPanel filterPanel,
                            ActionPanel actionPanel) {
    this.metricFunctionPanel = metricFunctionPanel;
    this.historyPanel = historyPanel;
    this.legendPanel = legendPanel;
    this.filterPanel = filterPanel;
    this.actionPanel = actionPanel;

    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);
    setLayout(new GridBagLayout());
    initComponents();
  }

  private void initComponents() {
    JPanel containerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
    LaF.setBackgroundConfigPanel(CHART_PANEL, containerPanel);
    containerPanel.add(filterPanel);
    containerPanel.add(actionPanel);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(2, 4, 2, 4);

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
    gbc.gridwidth = 2;
    add(containerPanel, gbc);

    gbc.gridx = 4;
    gbc.weightx = 10.0 / 15;
    gbc.fill = GridBagConstraints.BOTH;
    add(new JLabel(), gbc);
  }
}