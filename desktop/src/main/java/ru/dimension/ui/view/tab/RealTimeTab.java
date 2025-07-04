package ru.dimension.ui.view.tab;

import javax.swing.JTabbedPane;
import ru.dimension.ui.model.view.ProcessTypeWorkspace;
import ru.dimension.ui.state.GUIState;

public class RealTimeTab extends JTabbedPane {

  public RealTimeTab() {
    this.setTabPlacement(JTabbedPane.TOP);
    addChangeListener(e -> {
      int index = getSelectedIndex();
      if (index == 0) {
        GUIState.getInstance().setRealTimeTabState(ProcessTypeWorkspace.VISUALIZE);
      } else if (index == 1) {
        GUIState.getInstance().setRealTimeTabState(ProcessTypeWorkspace.ANALYZE);
      } else if (index == 2) {
        GUIState.getInstance().setRealTimeTabState(ProcessTypeWorkspace.SEARCH);
      }
    });
  }

  public void setSelectedTab(ProcessTypeWorkspace tab) {
    switch (tab) {
      case VISUALIZE -> this.setSelectedIndex(0);
      case ANALYZE -> this.setSelectedIndex(1);
      case SEARCH -> this.setSelectedIndex(2);
    }
  }
}
