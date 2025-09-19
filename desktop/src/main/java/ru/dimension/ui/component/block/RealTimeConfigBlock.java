package ru.dimension.ui.component.block;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import lombok.Getter;
import ru.dimension.ui.component.panel.FunctionPanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.component.panel.popup.ActionPanel;
import ru.dimension.ui.component.panel.popup.FilterPanel;
import ru.dimension.ui.component.panel.range.RealTimeRangePanel;
import ru.dimension.ui.laf.LaF;

public class RealTimeConfigBlock extends JPanel {
  private final FunctionPanel functionPanel;
  private final RealTimeRangePanel realTimePanel;
  @Getter
  private final LegendPanel legendPanel;
  private final FilterPanel filterPanel;
  private final ActionPanel actionPanel;

  private final JButton detailsButton;

  public RealTimeConfigBlock(FunctionPanel functionPanel,
                             RealTimeRangePanel realTimePanel,
                             LegendPanel legendPanel,
                             FilterPanel filterPanel,
                             ActionPanel actionPanel) {
    this(functionPanel, realTimePanel, legendPanel, filterPanel, actionPanel, null);
  }

  public RealTimeConfigBlock(FunctionPanel functionPanel,
                             RealTimeRangePanel realTimePanel,
                             LegendPanel legendPanel,
                             FilterPanel filterPanel,
                             ActionPanel actionPanel,
                             JButton detailsButton) {
    this.functionPanel = functionPanel;
    this.realTimePanel = realTimePanel;
    this.legendPanel = legendPanel;
    this.filterPanel = filterPanel;
    this.actionPanel = actionPanel;
    this.detailsButton = detailsButton;

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
    gbc.weightx = 2.0 / 15;
    add(functionPanel, gbc);

    gbc.gridx = 1;
    gbc.weightx = 2.0 / 15;
    add(realTimePanel, gbc);

    gbc.gridx = 2;
    gbc.weightx = 1.0 / 15;
    add(legendPanel, gbc);

    gbc.gridx = 3;
    gbc.weightx = 0;
    add(containerPanel, gbc);

    if (detailsButton != null) {
      gbc.gridx = 4;
      gbc.weightx = 0;
      add(detailsButton, gbc);
    }

    gbc.gridx = (detailsButton != null) ? 5 : 4;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    add(new JLabel(), gbc);
  }
}