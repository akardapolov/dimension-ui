package ru.dimension.ui.view.structure.toolbar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.router.Router;
import ru.dimension.ui.router.listener.ToolbarListener;
import ru.dimension.ui.state.NavigatorState;
import ru.dimension.ui.view.structure.ToolbarView;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.view.ToolbarAction;
import ru.dimension.ui.model.view.ToolbarButtonState;
import ru.dimension.ui.router.event.EventListener;

@Log4j2
@Singleton
public class ToolbarPresenter implements ActionListener, ToolbarListener {

  private final ToolbarView toolbarView;
  private final NavigatorState navigatorState;
  private final EventListener eventListener;
  private final ProfileManager profileManager;
  private final Router router;

  @Inject
  public ToolbarPresenter(@Named("toolbarView") ToolbarView toolbarView,
                          @Named("navigatorState") NavigatorState navigatorState,
                          @Named("eventListener") EventListener eventListener,
                          @Named("profileManager") ProfileManager profileManager,
                          @Named("router") Router router) {
    this.toolbarView = toolbarView;
    this.navigatorState = navigatorState;
    this.eventListener = eventListener;
    this.profileManager = profileManager;
    this.router = router;

    this.eventListener.addProfileButtonStateListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals(ToolbarAction.CONFIGURATION.name())) {
      router.runConfigDialog(navigatorState.getSelectedProfile());
    }

    if (e.getActionCommand().equals(ToolbarAction.TEMPLATE.name())) {
      router.runTemplateDialog();
    }

    if (e.getActionCommand().equals(ToolbarAction.REPORT.name())) {
      router.runReportDialog();
    }
  }

  @Override
  public void fireToolbarButtonStateChange(ToolbarButtonState toolbarButtonState) {
    toolbarView.setProfileButtonState(toolbarButtonState);
  }
}
