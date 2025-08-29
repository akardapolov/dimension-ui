package ru.dimension.ui.view.panel.report;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import lombok.Data;
import ru.dimension.ui.model.function.GroupFunction;

@Data
public class ButtonGroupFunction extends ButtonGroup {

  private final JRadioButton count;
  private final JRadioButton sum;
  private final JRadioButton average;

  public ButtonGroupFunction() {
    super();

    this.count = new JRadioButton(GroupFunction.COUNT.getName(), false);
    this.sum = new JRadioButton(GroupFunction.SUM.getName(), false);
    this.average = new JRadioButton(GroupFunction.AVG.getName(), false);

    this.add(count);
    this.add(sum);
    this.add(average);
  }

  public void setSelectedRadioButton(GroupFunction groupFunction) {
    switch (groupFunction) {
      case COUNT -> this.setSelected(count.getModel(), true);
      case SUM -> this.setSelected(sum.getModel(), true);
      case AVG -> this.setSelected(average.getModel(), true);
    }
  }
}
