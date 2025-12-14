package ru.dimension.ui.component.module.chart.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import ru.dimension.ui.component.module.chart.ChartModule;

public class ChartDetailDialog extends JDialog {

  private final ChartModule chartModule;

  public ChartDetailDialog(ChartModule chartModule) {
    this.chartModule = chartModule;
    initializeDialog();
    installEscToClose();
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

  private void installEscToClose() {
    final String actionKey = "CHART_DETAIL_DIALOG_CLOSE_ON_ESC";
    JRootPane root = getRootPane();

    root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);

    root.getActionMap().put(actionKey, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ChartDetailDialog.this.dispatchEvent(
            new WindowEvent(ChartDetailDialog.this, WindowEvent.WINDOW_CLOSING)
        );
      }
    });
  }

  public ChartModule getChartModule() {
    return chartModule;
  }
}