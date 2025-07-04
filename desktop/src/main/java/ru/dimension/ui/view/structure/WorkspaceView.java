package ru.dimension.ui.view.structure;

import ru.dimension.ui.view.structure.workspace.profile.WorkspaceProfileView;

public interface WorkspaceView {

  WorkspaceProfileView addWorkspaceProfileView(int profileId);

  void bindPresenter();
}
