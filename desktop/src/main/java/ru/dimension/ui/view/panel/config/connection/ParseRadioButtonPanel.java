package ru.dimension.ui.view.panel.config.connection;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import lombok.Data;
import ru.dimension.ui.model.parse.ParseType;

@Data
public class ParseRadioButtonPanel {

  private final JRadioButton parsePrometheus;
  private final JRadioButton parseJson;

  private final ButtonGroup buttonGroup;

  public ParseRadioButtonPanel() {
    this.parsePrometheus = new JRadioButton(ParseType.PROMETHEUS.getName(), true);
    this.parseJson = new JRadioButton(ParseType.JSON.getName(), false);

    buttonGroup = new ButtonGroup();
    buttonGroup.add(parsePrometheus);
    buttonGroup.add(parseJson);

    setButtonNotView();
  }

  public void setButtonNotView() {
    parseJson.setEnabled(false);
    parsePrometheus.setEnabled(false);
  }

  public void setButtonView() {
    parseJson.setEnabled(true);
    parsePrometheus.setEnabled(true);
  }

  public void setSelectedRadioButton(ParseType name) {

    switch (name) {
      case PROMETHEUS -> buttonGroup.setSelected(parsePrometheus.getModel(), true);
      case JSON -> buttonGroup.setSelected(parseJson.getModel(), true);
    }
  }
}
