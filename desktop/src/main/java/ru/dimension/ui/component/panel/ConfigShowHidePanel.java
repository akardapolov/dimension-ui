package ru.dimension.ui.component.panel;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.util.function.Consumer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.model.ChartConfigState;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;

public class ConfigShowHidePanel extends JPanel {
  private final JCheckBox configChartCheckBox;
  private ChartConfigState currentState;
  private Consumer<ChartConfigState> stateChangeConsumer;

  public ConfigShowHidePanel() {
    this(null);
  }

  public ConfigShowHidePanel(JLabel label) {
    this.currentState = ChartConfigState.SHOW;
    this.configChartCheckBox = new JCheckBox(currentState.getName());
    configChartCheckBox.setSelected(currentState == ChartConfigState.SHOW);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    if (label != null) {
      gbl.row()
          .cell(label).cell(configChartCheckBox)
          .cellXRemainder(new JLabel()).fillX();
      PGHelper.setConstrainsInsets(gbl, label, 0, 10);
    } else {
      gbl.row()
          .cell(configChartCheckBox)
          .cellXRemainder(new JLabel()).fillX();
    }

    PGHelper.setConstrainsInsets(gbl, configChartCheckBox, 0);
    gbl.done();

    configChartCheckBox.addActionListener(e -> {
      currentState = currentState.toggle();
      configChartCheckBox.setText(currentState.getName());
      configChartCheckBox.setSelected(currentState == ChartConfigState.SHOW);
      if (stateChangeConsumer != null) {
        stateChangeConsumer.accept(currentState);
      }
    });
  }

  public void setSelected(Boolean showLegend) {
    currentState = showLegend ? ChartConfigState.SHOW : ChartConfigState.HIDE;
    configChartCheckBox.setSelected(showLegend);
    configChartCheckBox.setText(currentState.getName());
  }

  public void setStateChangeConsumer(Consumer<ChartConfigState> consumer) {
    this.stateChangeConsumer = consumer;
  }
}