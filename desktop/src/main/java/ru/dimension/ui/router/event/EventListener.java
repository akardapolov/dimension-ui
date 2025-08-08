package ru.dimension.ui.router.event;

import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.router.listener.AdHocListener;
import ru.dimension.ui.router.listener.CollectStartStopListener;
import ru.dimension.ui.router.listener.ConfigListener;
import ru.dimension.ui.router.listener.DashboardListener;
import ru.dimension.ui.router.listener.ProfileAddListener;
import ru.dimension.ui.router.listener.ProfileStartStopListener;
import ru.dimension.ui.router.listener.ProgressbarListener;
import ru.dimension.ui.router.listener.ReportListener;
import ru.dimension.ui.router.listener.TemplateListener;
import ru.dimension.ui.router.listener.ToolbarListener;
import ru.dimension.ui.router.listener.WorkspaceListener;

public interface EventListener extends ToolbarListener, ConfigListener, TemplateListener, ReportListener,
    DashboardListener, AdHocListener,
    ProgressbarListener, WorkspaceListener, ProfileStartStopListener, CollectStartStopListener,
    ProfileAddListener {

  void addProfileButtonStateListener(ToolbarListener toolbarListener);

  void addProfileSelectOnNavigator(WorkspaceListener workspaceListener);

  void addConfigStateListener(ConfigListener configListener);

  void addTemplateStateListener(TemplateListener templateListener);

  void addReportStateListener(ReportListener reportListener);

  void addDashboardStateListener(DashboardListener dashboardListener);

  void addAdHocStateListener(AdHocListener adHocListener);

  void addProgressbarListener(ProgressbarListener progressbarListener);

  void addProfileStartStopListener(ProfileStartStopListener profileStartStopListener);

  void addCollectStartStopWorkspaceListener(ProfileTaskQueryKey profileTaskQueryKey,
                                            CollectStartStopListener collectStartStopListener);

  void addCollectStartStopPreviewListener(ProfileTaskQueryKey profileTaskQueryKey,
                                          CollectStartStopListener collectStartStopListener);

  void addCollectStartStopDashboardListener(ProfileTaskQueryKey profileTaskQueryKey,
                                            CollectStartStopListener collectStartStopListener);
  void addProfileAddListener(ProfileAddListener profileAddListener);

  <T> void clearListener(Class<T> genericClass);

  void clearListenerWorkspaceByKey(ProfileTaskQueryKey profileTaskQueryKey);
  <T> void clearListenerPreviewByClass(Class<T> genericClass);

  void clearListenerDashboardByKey(ProfileTaskQueryKey profileTaskQueryKey);

  boolean isProfileOnDashboardRunning(int profileId);
}
