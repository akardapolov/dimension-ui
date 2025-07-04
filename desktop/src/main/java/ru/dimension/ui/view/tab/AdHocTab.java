package ru.dimension.ui.view.tab;

import javax.swing.JTabbedPane;
import ru.dimension.ui.model.view.ProcessTypeWorkspace;

public class AdHocTab extends JTabbedPane {

  public AdHocTab() {
    this.setTabPlacement(JTabbedPane.TOP);
  }

  public void setSelectedTab(ProcessTypeWorkspace tab) {
    switch (tab) {
      case VISUALIZE -> this.setSelectedIndex(0);
      case ANALYZE -> this.setSelectedIndex(1);
    }
  }
}
