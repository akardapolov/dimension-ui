package ru.dimension.ui.component.block;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import lombok.Getter;
import ru.dimension.ui.component.panel.CollapseCardPanel;
import ru.dimension.ui.component.panel.ConfigShowHidePanel;
import ru.dimension.ui.component.panel.LegendPanel;
import ru.dimension.ui.laf.LaF;

public class PreviewConfigBlock extends JPanel {
  private final JPanel rangePanel;
  @Getter
  private final LegendPanel legendPanel;
  @Getter
  private final ConfigShowHidePanel configShowHidePanel;
  @Getter
  private final CollapseCardPanel collapseCardPanel;
  private final JButton clearButton;

  public PreviewConfigBlock(JPanel rangePanel,
                            LegendPanel legendPanel,
                            ConfigShowHidePanel configShowHidePanel,
                            CollapseCardPanel collapseCardPanel) {
    this(rangePanel, legendPanel, configShowHidePanel, collapseCardPanel, null);
  }

  public PreviewConfigBlock(JPanel rangePanel,
                            LegendPanel legendPanel,
                            ConfigShowHidePanel configShowHidePanel,
                            CollapseCardPanel collapseCardPanel,
                            JButton clearButton) {
    this.rangePanel = rangePanel;
    this.legendPanel = legendPanel;
    this.configShowHidePanel = configShowHidePanel;
    this.collapseCardPanel = collapseCardPanel;
    this.clearButton = clearButton;

    setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    initComponents();
  }

  private void initComponents() {
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    add(rangePanel);
    add(Box.createRigidArea(new Dimension(5, 0)));
    add(legendPanel);
    add(Box.createHorizontalGlue());
    add(configShowHidePanel);
    add(Box.createHorizontalGlue());
    add(collapseCardPanel);
    if (clearButton != null) {
      add(Box.createRigidArea(new Dimension(5, 0)));
      add(clearButton);
    }
  }
}