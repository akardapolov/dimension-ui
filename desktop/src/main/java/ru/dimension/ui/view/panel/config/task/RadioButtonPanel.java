package ru.dimension.ui.view.panel.config.task;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import lombok.Data;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.view.RadioButtonPane;

@Data
public class RadioButtonPanel extends JPanel {

  private final JRadioButton jRadioButton1;
  private final JRadioButton jRadioButton3;
  private final JRadioButton jRadioButton5;
  private final JRadioButton jRadioButton10;
  private final JRadioButton jRadioButton30;
  private final ButtonGroup buttonGroup;

  private int pullTimeout;

  public RadioButtonPanel() {
    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);

    this.jRadioButton1 = new JRadioButton(RadioButtonPane.ONE_SEC.getName(), true);
    this.jRadioButton3 = new JRadioButton(RadioButtonPane.THREE_SEC.getName(), false);
    this.jRadioButton5 = new JRadioButton(RadioButtonPane.FIVE_SEC.getName(), false);
    this.jRadioButton10 = new JRadioButton(RadioButtonPane.TEN_SEC.getName(), false);
    this.jRadioButton30 = new JRadioButton(RadioButtonPane.THIRTY_SEC.getName(), false);

    buttonGroup = new ButtonGroup();
    buttonGroup.add(jRadioButton1);
    buttonGroup.add(jRadioButton3);
    buttonGroup.add(jRadioButton5);
    buttonGroup.add(jRadioButton10);
    buttonGroup.add(jRadioButton30);
    setButtonNotView();
    gbl.row()
        .cell(jRadioButton1).cell(jRadioButton3).cell(jRadioButton5).cell(jRadioButton10).cell(jRadioButton30).fillX();
    gbl.done();
  }

  public void setButtonNotView() {
    jRadioButton1.setEnabled(false);
    jRadioButton3.setEnabled(false);
    jRadioButton5.setEnabled(false);
    jRadioButton10.setEnabled(false);
    jRadioButton30.setEnabled(false);
  }

  public void setButtonView() {
    jRadioButton1.setEnabled(true);
    jRadioButton3.setEnabled(true);
    jRadioButton5.setEnabled(true);
    jRadioButton10.setEnabled(true);
    jRadioButton30.setEnabled(true);
  }

  public void setSelectedRadioButton(String name) {

    switch (name) {
      case "1 sec": {
        buttonGroup.setSelected(jRadioButton1.getModel(), true);
        return;
      }
      case "3 sec": {
        buttonGroup.setSelected(jRadioButton3.getModel(), true);
        return;
      }
      case "5 sec": {
        buttonGroup.setSelected(jRadioButton5.getModel(), true);
        return;
      }
      case "10 sec": {
        buttonGroup.setSelected(jRadioButton10.getModel(), true);
        return;
      }
      case "30 sec": {
        buttonGroup.setSelected(jRadioButton30.getModel(), true);
      }
    }
  }
}
