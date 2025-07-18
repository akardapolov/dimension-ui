package ru.dimension.ui.component.panel;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import java.util.function.Consumer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.view.analyze.model.DetailState;

public class DetailShowHidePanel extends JPanel {
  private final JCheckBox detailCheckBox;
  private DetailState currentState;
  private Consumer<DetailState> stateChangeConsumer;

  public DetailShowHidePanel() {
    this.currentState = DetailState.SHOW;
    this.detailCheckBox = new JCheckBox(currentState.getName());
    detailCheckBox.setSelected(currentState == DetailState.SHOW);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);
    gbl.row()
        .cell(detailCheckBox)
        .cellXRemainder(new JLabel()).fillX();

    PGHelper.setConstrainsInsets(gbl, detailCheckBox, 0);
    gbl.done();

    detailCheckBox.addActionListener(e -> {
      currentState = currentState.toggle();
      detailCheckBox.setText(currentState.getName());
      detailCheckBox.setSelected(currentState == DetailState.SHOW);
      if (stateChangeConsumer != null) {
        stateChangeConsumer.accept(currentState);
      }
    });
  }

  public void setStateChangeConsumer(Consumer<DetailState> consumer) {
    this.stateChangeConsumer = consumer;
  }
}