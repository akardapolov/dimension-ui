package ru.dimension.ui.component.panel;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import lombok.Data;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.component.model.PanelTabType;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;

@Data
public class SwitchToTabPanel extends JPanel {

  private final JRadioButton realTime;
  private final JRadioButton history;
  private final ButtonGroup buttonGroup;

  public SwitchToTabPanel() {
    this.realTime = new JRadioButton(PanelTabType.REALTIME.getName(), false);
    this.history = new JRadioButton(PanelTabType.HISTORY.getName(), false);

    this.buttonGroup = new ButtonGroup();
    buttonGroup.add(realTime);
    buttonGroup.add(history);

    LaF.setBackgroundConfigPanel(CHART_PANEL, this);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(1), false);

    gbl.row()
        .cell(realTime).cell(history)
        .cellXRemainder(new JLabel()).fillX();

    PGHelper.setConstrainsInsets(gbl, realTime, 0);
    PGHelper.setConstrainsInsets(gbl, history, 0);

    gbl.done();
  }

  public void setSelectedTab(PanelTabType panelTabType) {
    switch (panelTabType) {
      case REALTIME -> realTime.setSelected(true);
      case HISTORY -> history.setSelected(true);
    }
  }
}
