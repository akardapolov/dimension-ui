package ru.dimension.ui.component.module.preview.chart;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.WindowConstants;
import ru.dimension.ui.component.module.ChartModule;

public class ChartDetailDialog extends JDialog {

  private final ChartModule chartModule;

  public ChartDetailDialog(ChartModule chartModule) {
    this.chartModule = chartModule;
    initializeDialog();
  }

  private void initializeDialog() {
    setTitle("Chart Details");
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLayout(new BorderLayout());
    setPreferredSize(new Dimension(1500, 900));
    setMinimumSize(new Dimension(1500, 900));
    add(chartModule, BorderLayout.CENTER);
    pack();
    setLocationRelativeTo(null);
    setModal(true);
  }

  public ChartModule getChartModule() {
    return chartModule;
  }
}
