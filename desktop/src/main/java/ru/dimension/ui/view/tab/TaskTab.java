package ru.dimension.ui.view.tab;

import javax.swing.JTabbedPane;
import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.state.GUIState;

public class TaskTab extends JTabbedPane {

  public TaskTab() {
    addChangeListener(e -> {
      int index = getSelectedIndex();
      if (index == 0) {
        GUIState.getInstance().setTaskTabState(ProcessType.REAL_TIME);
      } else if (index == 1) {
        GUIState.getInstance().setTaskTabState(ProcessType.HISTORY);
      }
    });
  }

  public void setSelectedTab(ProcessType tab) {
    switch (tab) {
      case REAL_TIME -> this.setSelectedIndex(0);
      case HISTORY -> this.setSelectedIndex(1);
    }
  }
}
