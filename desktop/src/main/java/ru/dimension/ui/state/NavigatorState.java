package ru.dimension.ui.state;

public interface NavigatorState {

  void setSelectionIndex(int selectionIndex);

  int getSelectionIndex();

  void setSelectedProfile(int profileId);

  int getSelectedProfile();
}
