package ru.dimension.ui.config.prototype.profile;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.swing.JButton;
import javax.swing.JLabel;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.config.prototype.WorkspaceProfileScope;
import ru.dimension.ui.model.ActionName;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.structure.workspace.profile.WorkspaceProfileView;

@Module
public class WorkspaceProfileModule {

  private final WorkspaceProfileView workspaceProfileView;

  public WorkspaceProfileModule(WorkspaceProfileView workspaceProfileView) {
    this.workspaceProfileView = workspaceProfileView;
  }

  @WorkspaceProfileScope
  @Provides
  public EventListener provideEventListener(@Named("eventListener") EventListener eventListener) {
    return eventListener;
  }

  @WorkspaceProfileScope
  @Provides
  @Named("workspaceProfileStartButton")
  public JButton getStartButton() {
    return GUIHelper.getJButton(ActionName.START.name());
  }

  @WorkspaceProfileScope
  @Provides
  @Named("workspaceProfileStopButton")
  public JButton getStopButton() {
    return GUIHelper.getJButton(ActionName.STOP.name());
  }

  @WorkspaceProfileScope
  @Provides
  @Named("profileStatusJLabel")
  public JLabel getToolbarProfileStatusJLabel() {
    return new JLabel();
  }

}
