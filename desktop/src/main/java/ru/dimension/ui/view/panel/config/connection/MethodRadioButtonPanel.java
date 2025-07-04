package ru.dimension.ui.view.panel.config.connection;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import lombok.Data;
import org.apache.hc.core5.http.Method;

@Data
public class MethodRadioButtonPanel {

  private final JRadioButton getMethod;
  private final ButtonGroup buttonGroup;

  public MethodRadioButtonPanel() {
    this.getMethod = new JRadioButton(Method.GET.name(), true);

    buttonGroup = new ButtonGroup();
    buttonGroup.add(getMethod);
    setButtonNotView();
  }

  public void setButtonNotView() {
    getMethod.setEnabled(false);
  }

  public void setButtonView() {
    getMethod.setEnabled(true);
  }

  public void setSelectedRadioButton(Method name) {

    switch (name) {
      case GET -> buttonGroup.setSelected(getMethod.getModel(), true);
    }
  }
}
