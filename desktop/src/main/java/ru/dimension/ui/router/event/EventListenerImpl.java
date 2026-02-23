package ru.dimension.ui.router.event;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.module.charts.ChartsPresenter;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.view.ConfigState;
import ru.dimension.ui.model.view.ProgressbarState;
import ru.dimension.ui.model.view.ReportState;
import ru.dimension.ui.model.view.TemplateState;
import ru.dimension.ui.model.view.ToolbarButtonState;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Log4j2
@Singleton
public class EventListenerImpl implements EventListener {

  private final List<ToolbarListener> profileButtonStateListenerList = new CopyOnWriteArrayList<>();
  private final List<ConfigListener> configListenerList = new CopyOnWriteArrayList<>();
  private final List<TemplateListener> templateListenerList = new CopyOnWriteArrayList<>();
  private final List<ReportListener> reportListenerList = new CopyOnWriteArrayList<>();
  private final List<DashboardListener> dashboardListenerList = new CopyOnWriteArrayList<>();
  private final List<AdHocListener> adHocListenersList = new CopyOnWriteArrayList<>();
  private final List<ProgressbarListener> progressbarListenerList = new CopyOnWriteArrayList<>();
  private final List<WorkspaceListener> workspaceListenerList = new CopyOnWriteArrayList<>();
  private final List<ProfileStartStopListener> profileStartStopListenerList = new CopyOnWriteArrayList<>();

  private final Map<ProfileTaskQueryKey, CollectStartStopListener> collectStartStopListenerMap =
      new ConcurrentHashMap<>();
  private final Map<ProfileTaskQueryKey, CollectStartStopListener> collectStartStopPreviewListenerMap =
      new ConcurrentHashMap<>();
  private final Map<ProfileTaskQueryKey, CollectStartStopListener> collectStartStopDashboardListenerMap =
      new ConcurrentHashMap<>();
  private final Map<ProfileTaskQueryKey, CollectStartStopListener> collectStartStopZoomListenerMap =
      new ConcurrentHashMap<>();

  @Inject
  public EventListenerImpl() {
  }

  @Override
  public void addProfileButtonStateListener(ToolbarListener toolbarListener) {
    profileButtonStateListenerList.add(toolbarListener);
  }

  @Override
  public void fireToolbarButtonStateChange(ToolbarButtonState toolbarButtonState) {
    profileButtonStateListenerList.forEach(l -> l.fireToolbarButtonStateChange(toolbarButtonState));
  }

  @Override
  public void fireOnSelectProfileOnNavigator(int profileId) {
    workspaceListenerList.forEach(l -> l.fireOnSelectProfileOnNavigator(profileId));
  }

  @Override
  public void addConfigStateListener(ConfigListener configListener) {
    configListenerList.add(configListener);
  }

  @Override
  public void fireShowConfig(ConfigState configState) {
    configListenerList.forEach(l -> l.fireShowConfig(configState));
  }

  @Override
  public void addTemplateStateListener(TemplateListener templateListener) {
    templateListenerList.add(templateListener);
  }

  @Override
  public void fireShowTemplate(TemplateState templateState) {
    templateListenerList.forEach(l -> l.fireShowTemplate(templateState));
  }

  @Override
  public void addProgressbarListener(ProgressbarListener progressbarListener) {
    progressbarListenerList.add(progressbarListener);
  }

  @Override
  public void fireProgressbarVisible(ProgressbarState progressbarState) {
    progressbarListenerList.forEach(l -> l.fireProgressbarVisible(progressbarState));
  }

  @Override
  public void addProfileStartStopListener(ProfileStartStopListener profileStartStopListener) {
    profileStartStopListenerList.add(profileStartStopListener);
  }

  @Override
  public void fireOnStartOnWorkspaceProfileView(int profileId) {
    profileStartStopListenerList.forEach(l -> l.fireOnStartOnWorkspaceProfileView(profileId));
  }

  @Override
  public void fireOnStopOnWorkspaceProfileView(int profileId) {
    profileStartStopListenerList.forEach(l -> l.fireOnStopOnWorkspaceProfileView(profileId));
  }

  @Override
  public void addCollectStartStopWorkspaceListener(ProfileTaskQueryKey profileTaskQueryKey,
                                                   CollectStartStopListener collectStartStopListener) {
    collectStartStopListenerMap.put(profileTaskQueryKey, collectStartStopListener);
  }

  @Override
  public void addCollectStartStopPreviewListener(ProfileTaskQueryKey profileTaskQueryKey,
                                                 CollectStartStopListener collectStartStopListener) {
    collectStartStopPreviewListenerMap.put(profileTaskQueryKey, collectStartStopListener);
  }

  @Override
  public void addCollectStartStopDashboardListener(ProfileTaskQueryKey profileTaskQueryKey,
                                                   CollectStartStopListener collectStartStopListener) {
    collectStartStopDashboardListenerMap.put(profileTaskQueryKey, collectStartStopListener);
  }

  @Override
  public void addCollectStartStopZoomListener(ProfileTaskQueryKey profileTaskQueryKey,
                                              CollectStartStopListener collectStartStopListener) {
    collectStartStopZoomListenerMap.put(profileTaskQueryKey, collectStartStopListener);
  }

  @Override
  public void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    fireStartCollect(collectStartStopListenerMap.get(profileTaskQueryKey), profileTaskQueryKey);
    fireStartCollect(collectStartStopPreviewListenerMap.get(profileTaskQueryKey), profileTaskQueryKey);
    fireStartCollect(collectStartStopDashboardListenerMap.get(profileTaskQueryKey), profileTaskQueryKey);
    fireStartCollect(collectStartStopZoomListenerMap.get(profileTaskQueryKey), profileTaskQueryKey);
  }

  @Override
  public void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    fireStopCollect(collectStartStopListenerMap.get(profileTaskQueryKey), profileTaskQueryKey);
    fireStopCollect(collectStartStopPreviewListenerMap.get(profileTaskQueryKey), profileTaskQueryKey);
    fireStopCollect(collectStartStopDashboardListenerMap.get(profileTaskQueryKey), profileTaskQueryKey);
    fireStopCollect(collectStartStopZoomListenerMap.get(profileTaskQueryKey), profileTaskQueryKey);
  }

  private void fireStartCollect(CollectStartStopListener listener, ProfileTaskQueryKey key) {
    if (listener != null) {
      listener.fireOnStartCollect(key);
    }
  }

  private void fireStopCollect(CollectStartStopListener listener, ProfileTaskQueryKey key) {
    if (listener != null) {
      listener.fireOnStopCollect(key);
    }
  }

  @Override
  public void clearListenerWorkspaceByKey(ProfileTaskQueryKey profileTaskQueryKey) {
    collectStartStopListenerMap.remove(profileTaskQueryKey);
  }

  @Override
  public void clearListenerDashboardByKey(ProfileTaskQueryKey profileTaskQueryKey) {
    collectStartStopDashboardListenerMap.remove(profileTaskQueryKey);
  }

  @Override
  public void clearListenerZoomByKey(ProfileTaskQueryKey profileTaskQueryKey) {
    collectStartStopZoomListenerMap.remove(profileTaskQueryKey);
  }

  @Override
  public <T> void clearListenerPreviewByClass(Class<T> genericClass) {
    collectStartStopPreviewListenerMap.entrySet()
        .removeIf(entry -> genericClass.isInstance(entry.getValue()));
  }

  @Override
  public void fireShowReport(ReportState reportState) {
    reportListenerList.forEach(l -> l.fireShowReport(reportState));
  }

  @Override
  public void fireShowDashboard() {
    dashboardListenerList.forEach(DashboardListener::fireShowDashboard);
  }

  @Override
  public boolean isProfileOnDashboardRunning(int profileId) {
    return collectStartStopDashboardListenerMap.entrySet()
        .stream()
        .filter(entry -> entry.getKey().getProfileId() == profileId)
        .findAny()
        .map(entry -> entry.getValue() instanceof ChartsPresenter presenter
            && presenter.isRunning(profileId))
        .orElse(false);
  }

  @Override
  public void fireShowAdHoc() {
    adHocListenersList.forEach(AdHocListener::fireShowAdHoc);
  }
}