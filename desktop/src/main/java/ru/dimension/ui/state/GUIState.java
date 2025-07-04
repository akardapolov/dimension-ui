package ru.dimension.ui.state;

import ru.dimension.ui.model.view.ProcessType;
import ru.dimension.ui.model.view.ProcessTypeWorkspace;

public class GUIState {
  private static GUIState instance;
  private ProcessType taskTabState = ProcessType.REAL_TIME;
  private ProcessTypeWorkspace realTimeTabState = ProcessTypeWorkspace.VISUALIZE;
  private ProcessTypeWorkspace historyTabState = ProcessTypeWorkspace.VISUALIZE;

  private GUIState() {}

  public static synchronized GUIState getInstance() {
    if (instance == null) {
      instance = new GUIState();
    }
    return instance;
  }

  public ProcessType getTaskTabState() {
    return taskTabState;
  }

  public void setTaskTabState(ProcessType taskTabState) {
    this.taskTabState = taskTabState;
  }

  public ProcessTypeWorkspace getRealTimeTabState() {
    return realTimeTabState;
  }

  public void setRealTimeTabState(ProcessTypeWorkspace realTimeTabState) {
    this.realTimeTabState = realTimeTabState;
  }

  public ProcessTypeWorkspace getHistoryTabState() {
    return historyTabState;
  }

  public void setHistoryTabState(ProcessTypeWorkspace historyTabState) {
    this.historyTabState = historyTabState;
  }
}
