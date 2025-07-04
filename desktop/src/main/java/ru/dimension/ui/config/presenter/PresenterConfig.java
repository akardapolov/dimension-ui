package ru.dimension.ui.config.presenter;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.ui.view.structure.navigator.NavigatorPresenter;
import ru.dimension.ui.view.structure.progressbar.ProgressbarPresenter;
import ru.dimension.ui.view.structure.adhoc.AdHocPresenter;
import ru.dimension.ui.view.structure.config.ConfigPresenter;
import ru.dimension.ui.view.structure.report.ReportPresenter;
import ru.dimension.ui.view.structure.toolbar.ToolbarPresenter;
import ru.dimension.ui.view.structure.workspace.WorkspacePresenter;

@Module
public abstract class PresenterConfig {

  @Binds
  @Named("workspacePresenter")
  public abstract WorkspacePresenter bindWorkspacePresenter(WorkspacePresenter workspacePresenter);

  @Binds
  @Named("toolbarPresenter")
  public abstract ToolbarPresenter bindToolbarPresenter(ToolbarPresenter toolbarPresenter);

  @Binds
  @Named("progressbarPresenter")
  public abstract ProgressbarPresenter bindProgressbarPresenter(ProgressbarPresenter progressbarPresenter);

  @Binds
  @Named("navigatorPresenter")
  public abstract NavigatorPresenter bindNavigatorPresenter(NavigatorPresenter navigatorPresenter);

  @Binds
  @Named("profilePresenter")
  public abstract ConfigPresenter bindProfilePresenter(ConfigPresenter configPresenter);

  @Binds
  @Named("reportPresenter")
  public abstract ReportPresenter bindReportPresenter(ReportPresenter reportPresenter);

  @Binds
  @Named("adHocPresenter")
  public abstract AdHocPresenter bindAdHocPresenter(AdHocPresenter adHocPresenter);
}
