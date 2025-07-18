package ru.dimension.ui.component.panel.popup;

import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FilterPanel extends ConfigPopupPanel {

  public FilterPanel() {
    super(FilterPanel::createPopupContent, "Filter >>", "Filter <<");
  }

  private static JPanel createPopupContent() {
    JPanel panel = new JPanel();
    panel.add(new JLabel("Module is under development"));
    panel.setPreferredSize(new Dimension(200, 200));
    return panel;
  }
}