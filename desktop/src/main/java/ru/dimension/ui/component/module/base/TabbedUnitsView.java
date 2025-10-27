package ru.dimension.ui.component.module.base;

import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import lombok.Getter;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.api.ModuleView;
import ru.dimension.ui.helper.GUIHelper;

public class TabbedUnitsView extends JPanel implements ModuleView {

  @Getter
  private final JTabbedPane tabbedPane = new JTabbedPane();

  public TabbedUnitsView() {
    tabbedPane.setBorder(GUIHelper.getEtchedBorder());
  }

  public void addTab(MessageBroker.Panel panel, Component root) {
    tabbedPane.addTab(GUIHelper.getTabName(panel), root);
  }
}