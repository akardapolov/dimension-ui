package ru.dimension.ui.view.tab;

import javax.swing.JTabbedPane;
import ru.dimension.ui.model.view.tab.MainTabPane;

public class MainTab extends JTabbedPane {

  public void setSelectedTab(MainTabPane tab) {
    switch (tab) {
      case WORKSPACE -> this.setSelectedIndex(0);
      case DASHBOARD -> this.setSelectedIndex(1);
      case REPORT -> this.setSelectedIndex(2);
      case ADHOC -> this.setSelectedIndex(3);
    }
  }
}
