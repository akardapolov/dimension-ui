package ru.dimension.ui.config;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.ui.view.handler.adhoc.AdHocSelectionHandler;
import ru.dimension.ui.view.handler.profile.MultiSelectTaskHandler;
import ru.dimension.ui.view.handler.profile.ProfileButtonPanelHandler;
import ru.dimension.ui.view.handler.profile.ProfileSelectionHandler;
import ru.dimension.ui.view.handler.query.QueryButtonPanelHandler;
import ru.dimension.ui.view.handler.query.QueryMetadataHandler;
import ru.dimension.ui.view.handler.query.QueryMetricButtonPanelHandler;
import ru.dimension.ui.view.handler.query.QueryMetricHandler;
import ru.dimension.ui.view.handler.query.QuerySelectionHandler;
import ru.dimension.ui.view.handler.task.MultiSelectQueryHandler;
import ru.dimension.ui.view.handler.task.TaskButtonPanelHandler;
import ru.dimension.ui.view.handler.task.TaskSelectionHandler;
import ru.dimension.ui.view.handler.connection.ConnectionButtonPanelHandler;
import ru.dimension.ui.view.handler.connection.ConnectionSelectionHandler;
import ru.dimension.ui.view.handler.connection.ConnectionTemplateTableHandler;
import ru.dimension.ui.view.handler.report.design.DesignPanelHandler;
import ru.dimension.ui.view.handler.report.report.ReportPanelHandler;

@Module
public abstract class HandlerConfig {


  @Binds
  @Named("profileSelectionHandler")
  public abstract ProfileSelectionHandler bindProfileSelectionHandler(
      ProfileSelectionHandler profileSelectionHandler);

  @Binds
  @Named("taskSelectionHandler")
  public abstract TaskSelectionHandler bindTaskSelectionHandler(
      TaskSelectionHandler profileTaskSelectionHandler);

  @Binds
  @Named("connectionSelectionHandler")
  public abstract ConnectionSelectionHandler bindConnectionSelectionHandler(
      ConnectionSelectionHandler profileConnectionSelectionHandler);

  @Binds
  @Named("querySelectionHandler")
  public abstract QuerySelectionHandler bindQuerySelectionHandler(
      QuerySelectionHandler profileQuerySelectionHandler);

  @Binds
  @Named("queryMetadataHandler")
  public abstract QueryMetadataHandler bindQueryMetadataHandler(
      QueryMetadataHandler profileQueryMetadataHandler);

  @Binds
  @Named("profileButtonPanelHandler")
  public abstract ProfileButtonPanelHandler bindProfileButtonPanelHandler(
      ProfileButtonPanelHandler profileButtonPanelHandler);

  @Binds
  @Named("taskButtonPanelHandler")
  public abstract TaskButtonPanelHandler bindTaskButtonPanelHandler(
      TaskButtonPanelHandler taskButtonPanelHandler);

  @Binds
  @Named("connectionButtonPanelHandler")
  public abstract ConnectionButtonPanelHandler bindConnectionButtonPanelHandler(
      ConnectionButtonPanelHandler connectionButtonPanelHandler);

  @Binds
  @Named("queryButtonPanelHandler")
  public abstract QueryButtonPanelHandler bindQueryButtonPanelHandler(
      QueryButtonPanelHandler queryButtonPanelHandler);

  @Binds
  @Named("queryMetricButtonPanelHandler")
  public abstract QueryMetricButtonPanelHandler bindQueryMetricButtonPanelHandler(
      QueryMetricButtonPanelHandler queryMetricButtonPanelHandler);

  @Binds
  @Named("queryMetricHandler")
  public abstract QueryMetricHandler bindQueryMetricHandler(
      QueryMetricHandler queryMetricHandler);

  @Binds
  @Named("multiSelectTaskHandler")
  public abstract MultiSelectTaskHandler bindMultiSelectTaskHandler(
      MultiSelectTaskHandler multiSelectTaskHandler);

  @Binds
  @Named("multiSelectQueryHandler")
  public abstract MultiSelectQueryHandler bindMultiSelectQueryHandler(
      MultiSelectQueryHandler multiSelectQueryHandler);

  @Binds
  @Named("connectionTemplateTableHandler")
  public abstract ConnectionTemplateTableHandler bindConnectionTemplateTableHandler(
      ConnectionTemplateTableHandler connectionTemplateTableHandler);

  @Binds
  @Named("designPanelHandler")
  public abstract DesignPanelHandler bindDesignPanelHandler(
      DesignPanelHandler designPanelHandler);

  @Binds
  @Named("reportPanelHandler")
  public abstract ReportPanelHandler bindReportPanelHandler(
      ReportPanelHandler reportPanelHandler);

  @Binds
  @Named("adHocPanelHandler")
  public abstract AdHocSelectionHandler bindAdHocSelectionHandler(
      AdHocSelectionHandler adHocSelectionHandler);
}

