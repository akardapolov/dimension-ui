package ru.dimension.ui.router.event;

import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.router.listener.AdHocListener;
import ru.dimension.ui.router.listener.CollectStartStopListener;
import ru.dimension.ui.router.listener.ConfigListener;
import ru.dimension.ui.router.listener.DashboardListener;
import ru.dimension.ui.router.listener.ProfileStartStopListener;
import ru.dimension.ui.router.listener.ProgressbarListener;
import ru.dimension.ui.router.listener.ReportListener;
import ru.dimension.ui.router.listener.TemplateListener;
import ru.dimension.ui.router.listener.ToolbarListener;
import ru.dimension.ui.router.listener.WorkspaceListener;

public interface EventListener extends ToolbarListener, ConfigListener, TemplateListener, ReportListener,
    DashboardListener, AdHocListener,
    ProgressbarListener, WorkspaceListener, ProfileStartStopListener, CollectStartStopListener {

  void addProfileButtonStateListener(ToolbarListener toolbarListener);

  void addConfigStateListener(ConfigListener configListener);

  void addTemplateStateListener(TemplateListener templateListener);

  void addProgressbarListener(ProgressbarListener progressbarListener);

  void addProfileStartStopListener(ProfileStartStopListener profileStartStopListener);

  void addCollectStartStopWorkspaceListener(ProfileTaskQueryKey profileTaskQueryKey,
                                            CollectStartStopListener collectStartStopListener);

  void addCollectStartStopPreviewListener(ProfileTaskQueryKey profileTaskQueryKey,
                                          CollectStartStopListener collectStartStopListener);

  void addCollectStartStopDashboardListener(ProfileTaskQueryKey profileTaskQueryKey,
                                            CollectStartStopListener collectStartStopListener);

  void addCollectStartStopZoomListener(ProfileTaskQueryKey profileTaskQueryKey,
                                       CollectStartStopListener collectStartStopListener);

  void clearListenerWorkspaceByKey(ProfileTaskQueryKey profileTaskQueryKey);
  <T> void clearListenerPreviewByClass(Class<T> genericClass);

  void clearListenerDashboardByKey(ProfileTaskQueryKey profileTaskQueryKey);

  void clearListenerZoomByKey(ProfileTaskQueryKey profileTaskQueryKey);

  boolean isProfileOnDashboardRunning(int profileId);
}