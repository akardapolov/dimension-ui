package ru.dimension.ui.view.handler.core;

import jakarta.inject.Singleton;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Objects;

@Singleton
public final class ConfigSelectionContext {

  public static final String PROP_PROFILE_ID = "selectedProfileId";
  public static final String PROP_TASK_ID = "selectedTaskId";
  public static final String PROP_CONNECTION_ID = "selectedConnectionId";
  public static final String PROP_QUERY_ID = "selectedQueryId";

  private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  private Integer selectedProfileId;
  private Integer selectedTaskId;
  private Integer selectedConnectionId;
  private Integer selectedQueryId;

  public Integer getSelectedProfileId() {
    return selectedProfileId;
  }

  public void setSelectedProfileId(Integer id) {
    Integer old = this.selectedProfileId;
    if (Objects.equals(old, id)) {
      return;
    }
    this.selectedProfileId = id;
    pcs.firePropertyChange(PROP_PROFILE_ID, old, id);
  }

  public Integer getSelectedTaskId() {
    return selectedTaskId;
  }

  public void setSelectedTaskId(Integer id) {
    Integer old = this.selectedTaskId;
    if (Objects.equals(old, id)) {
      return;
    }
    this.selectedTaskId = id;
    pcs.firePropertyChange(PROP_TASK_ID, old, id);
  }

  public Integer getSelectedConnectionId() {
    return selectedConnectionId;
  }

  public void setSelectedConnectionId(Integer id) {
    Integer old = this.selectedConnectionId;
    if (Objects.equals(old, id)) {
      return;
    }
    this.selectedConnectionId = id;
    pcs.firePropertyChange(PROP_CONNECTION_ID, old, id);
  }

  public Integer getSelectedQueryId() {
    return selectedQueryId;
  }

  public void setSelectedQueryId(Integer id) {
    Integer old = this.selectedQueryId;
    if (Objects.equals(old, id)) {
      return;
    }
    this.selectedQueryId = id;
    pcs.firePropertyChange(PROP_QUERY_ID, old, id);
  }

  public void addListener(PropertyChangeListener listener) {
    pcs.addPropertyChangeListener(listener);
  }

  public void removeListener(PropertyChangeListener listener) {
    pcs.removePropertyChangeListener(listener);
  }
}