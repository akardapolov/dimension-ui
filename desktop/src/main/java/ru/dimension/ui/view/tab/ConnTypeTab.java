package ru.dimension.ui.view.tab;

import javax.swing.JTabbedPane;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;

public class ConnTypeTab extends JTabbedPane {

  public void setSelectedTab(ConnectionTypeTabPane tab) {
    switch (tab) {
      case JDBC -> this.setSelectedIndex(0);
      case HTTP -> this.setSelectedIndex(1);
      case JMX -> {
        if (getTabCount() > 2) {
          this.setSelectedIndex(2);
        }
      }
    }
  }

  public void setEnabledTab(ConnectionTypeTabPane tab,
                            boolean enabled) {
    switch (tab) {
      case JDBC -> this.setEnabledAt(0, enabled);
      case HTTP -> this.setEnabledAt(1, enabled);
      case JMX -> {
        if (getTabCount() > 2) {
          this.setEnabledAt(2, enabled);
        }
      }
    }
  }
}
