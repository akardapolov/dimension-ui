package ru.dimension.ui.component.block;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.awt.FlowLayout;
import javax.swing.JPanel;
import lombok.Getter;
import ru.dimension.ui.component.panel.CollapseCardPanel;
import ru.dimension.ui.laf.LaF;

@Getter
public class InsightConfigBlock extends JPanel {

  private final CollapseCardPanel collapseCardPanel;

  public InsightConfigBlock() {
    this(new CollapseCardPanel());
  }

  public InsightConfigBlock(CollapseCardPanel collapseCardPanel) {
    this.collapseCardPanel = collapseCardPanel;
    LaF.setBackgroundConfigPanel(CHART_PANEL, this);
    setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    add(this.collapseCardPanel);
  }
}