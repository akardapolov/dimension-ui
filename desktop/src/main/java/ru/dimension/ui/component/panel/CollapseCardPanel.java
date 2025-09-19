package ru.dimension.ui.component.panel;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.util.function.Consumer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.component.model.ChartCardState;

public class CollapseCardPanel extends JPanel {
  private final JCheckBox collapseCheckBox;
  private ChartCardState currentState;
  private Consumer<ChartCardState> stateChangeConsumer;

  public CollapseCardPanel() {
    this(null);
  }

  public CollapseCardPanel(JLabel label) {
    this.currentState = ChartCardState.COLLAPSE_ALL;
    this.collapseCheckBox = new JCheckBox(currentState.getName());

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    if (label != null) {
      gbl.row()
          .cell(label).cell(collapseCheckBox)
          .cellXRemainder(new JLabel()).fillX();
      PGHelper.setConstrainsInsets(gbl, label, 0, 10);
    } else {
      gbl.row()
          .cell(collapseCheckBox)
          .cellXRemainder(new JLabel()).fillX();
    }

    PGHelper.setConstrainsInsets(gbl, collapseCheckBox, 0);
    gbl.done();

    // Action listener for state changes
    collapseCheckBox.addActionListener(e -> {
      currentState = currentState.toggle();
      collapseCheckBox.setText(currentState.getName());
      if (stateChangeConsumer != null) {
        stateChangeConsumer.accept(currentState);
      }
    });
  }

  public ChartCardState getCurrentState() {
    return currentState;
  }

  public void setState(ChartCardState state) {
    this.currentState = state;
    collapseCheckBox.setText(state.getName());
    collapseCheckBox.setSelected(state == ChartCardState.EXPAND_ALL);
  }

  public void setCollapseCheckBoxEnabled(boolean enabled) {
    collapseCheckBox.setEnabled(enabled);
  }

  public void setStateChangeConsumer(Consumer<ChartCardState> consumer) {
    this.stateChangeConsumer = consumer;
  }
}