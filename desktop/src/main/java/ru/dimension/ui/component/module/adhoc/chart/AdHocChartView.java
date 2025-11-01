package ru.dimension.ui.component.module.adhoc.chart;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import lombok.Getter;
import ru.dimension.ui.component.module.adhoc.chart.unit.AdHocHistoryUnitView;

@Getter
public class AdHocChartView {

  private final JPanel panel;

  public AdHocChartView(AdHocHistoryUnitView historyUnitView) {
    this.panel = new JPanel(new BorderLayout());
    this.panel.setBorder(new EtchedBorder());
    this.panel.add(historyUnitView.getRootComponent(), BorderLayout.CENTER);
  }
}