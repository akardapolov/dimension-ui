package ru.dimension.ui.config.view;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.ui.view.structure.ConfigView;
import ru.dimension.ui.view.structure.NavigatorView;
import ru.dimension.ui.view.structure.ProgressbarView;
import ru.dimension.ui.view.structure.ReportView;
import ru.dimension.ui.view.structure.TemplateView;
import ru.dimension.ui.view.structure.ToolbarView;
import ru.dimension.ui.view.structure.WorkspaceView;
import ru.dimension.ui.view.structure.config.ConfigViewImpl;
import ru.dimension.ui.view.structure.navigator.NavigatorViewImpl;
import ru.dimension.ui.view.structure.progressbar.ProgressbarViewImpl;
import ru.dimension.ui.view.structure.report.ReportViewImpl;
import ru.dimension.ui.view.structure.template.TemplateViewImpl;
import ru.dimension.ui.view.structure.toolbar.ToolbarViewImpl;
import ru.dimension.ui.view.structure.workspace.WorkspaceViewImpl;

@Module
public abstract class ViewConfig {

  @Binds
  @Named("navigatorView")
  public abstract NavigatorView bindNavigator(NavigatorViewImpl navigatorView);

  @Binds
  @Named("workspaceView")
  public abstract WorkspaceView bindWorkspace(WorkspaceViewImpl workspaceView);

  @Binds
  @Named("toolbarView")
  public abstract ToolbarView bindToolbar(ToolbarViewImpl toolbarView);

  @Binds
  @Named("configView")
  public abstract ConfigView bindConfig(ConfigViewImpl profileView);

  @Binds
  @Named("templateView")
  public abstract TemplateView bindTemplate(TemplateViewImpl templateView);

  @Binds
  @Named("progressbarView")
  public abstract ProgressbarView bindProgressbar(ProgressbarViewImpl progressbarView);

  @Binds
  @Named("reportView")
  public abstract ReportView bindReport(ReportViewImpl reportView);
}
