package ru.dimension.ui.component.panel;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.util.function.Consumer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.view.analyze.model.ChartLegendState;

public class LegendPanel extends JPanel {
  private final JCheckBox legendCheckBox;
  private ChartLegendState currentState;
  private Consumer<ChartLegendState> visibilityConsumer;

  public LegendPanel() {
    this(null);
  }

  public LegendPanel(JLabel label) {
    this.currentState = ChartLegendState.SHOW;
    this.legendCheckBox = new JCheckBox(currentState.getName(), true);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    if (label != null) {
      gbl.row()
          .cell(label).cell(legendCheckBox)
          .cellXRemainder(new JLabel()).fillX();
      PGHelper.setConstrainsInsets(gbl, label, 0, 10);
    } else {
      gbl.row()
          .cell(legendCheckBox)
          .cellXRemainder(new JLabel()).fillX();
    }

    PGHelper.setConstrainsInsets(gbl, legendCheckBox, 0);
    gbl.done();

    // Add action listener
    legendCheckBox.addActionListener(e -> toggleState());
  }

  private void toggleState() {
    currentState = currentState.toggle();
    legendCheckBox.setText(currentState.getName());
    if (visibilityConsumer != null) {
      visibilityConsumer.accept(currentState);
    }
  }

  public void setSelected(Boolean showLegend) {
    currentState = showLegend ? ChartLegendState.SHOW : ChartLegendState.HIDE;
    legendCheckBox.setSelected(showLegend);
    legendCheckBox.setText(currentState.getName());
  }

  public ChartLegendState getCurrentState() {
    return currentState;
  }

  public void setVisibilityConsumer(Consumer<ChartLegendState> visibilityConsumer) {
    this.visibilityConsumer = visibilityConsumer;
  }
}