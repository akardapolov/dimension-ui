package ru.dimension.ui.view.tab;

import javax.swing.JTabbedPane;
import ru.dimension.ui.model.view.tab.ConfigEditTabPane;

public class ConfigTab extends JTabbedPane {

  public void setSelectedTab(ConfigEditTabPane tab) {
    switch (tab) {
      case PROFILE -> this.setSelectedIndex(0);
      case TASK -> this.setSelectedIndex(1);
      case CONNECTION -> this.setSelectedIndex(2);
      case QUERY -> this.setSelectedIndex(3);
    }
  }
}
