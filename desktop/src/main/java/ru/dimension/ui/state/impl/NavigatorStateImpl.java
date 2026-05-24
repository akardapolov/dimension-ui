package ru.dimension.ui.state.impl;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.state.NavigatorState;

@Log4j2
@Singleton
public class NavigatorStateImpl implements NavigatorState {

  private volatile int selectionIndex;
  private volatile int profileId;

  @Inject
  public NavigatorStateImpl() {
  }

  @Override
  public int getSelectedProfile() {
    return profileId;
  }
}
