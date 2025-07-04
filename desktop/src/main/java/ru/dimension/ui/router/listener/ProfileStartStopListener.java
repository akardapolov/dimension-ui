package ru.dimension.ui.router.listener;

public interface ProfileStartStopListener {

  void fireOnStartOnWorkspaceProfileView(int profileId);

  void fireOnStopOnWorkspaceProfileView(int profileId);
}
