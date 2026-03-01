package ru.dimension.ui.component.panel.popup.action;

import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.panel.popup.base.ConfigPopupPanel;

public class ActionPanel extends ConfigPopupPanel {
  private final MessageBroker.Component component;

  public ActionPanel(MessageBroker.Component component) {
    super(ActionPanel::createPopupContent, "Action >>", "Action <<");

    this.component = component;
  }

  private static JPanel createPopupContent() {
    JPanel panel = new JPanel();
    panel.add(new JLabel("Module is under development"));
    panel.setPreferredSize(new Dimension(200, 200));
    return panel;
  }
}