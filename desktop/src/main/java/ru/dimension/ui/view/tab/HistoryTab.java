package ru.dimension.ui.view.tab;

import javax.swing.JTabbedPane;
import ru.dimension.ui.model.view.ProcessTypeWorkspace;
import ru.dimension.ui.state.GUIState;

public class HistoryTab extends JTabbedPane {

  public HistoryTab() {
    this.setTabPlacement(JTabbedPane.TOP);
    addChangeListener(e -> {
      int index = getSelectedIndex();
      if (index == 0) {
        GUIState.getInstance().setHistoryTabState(ProcessTypeWorkspace.VISUALIZE);
      } else if (index == 1) {
        GUIState.getInstance().setHistoryTabState(ProcessTypeWorkspace.ANALYZE);
      }
    });
  }

  public void setSelectedTab(ProcessTypeWorkspace tab) {
    switch (tab) {
      case VISUALIZE -> this.setSelectedIndex(0);
      case ANALYZE -> this.setSelectedIndex(1);
    }
  }
}
