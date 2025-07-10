package ru.dimension.ui.router.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.module.charts.ChartsPresenter;
import ru.dimension.ui.router.listener.AdHocListener;
import ru.dimension.ui.router.listener.AppCacheAddListener;
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
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.view.ConfigState;
import ru.dimension.ui.model.view.ProgressbarState;
import ru.dimension.ui.model.view.ReportState;
import ru.dimension.ui.model.view.TemplateState;
import ru.dimension.ui.model.view.ToolbarButtonState;

@Log4j2
@Singleton
public class EventListenerImpl implements EventListener {

  private List<ToolbarListener> profileButtonStateListenerList = new ArrayList<>();
  private List<ConfigListener> configListenerList = new ArrayList<>();
  private List<TemplateListener> templateListenerList = new ArrayList<>();
  private List<ReportListener> reportListenerList = new ArrayList<>();
  private List<DashboardListener> dashboardListenerList = new ArrayList<>();
  private List<AdHocListener> adHocListenersList = new ArrayList<>();
  private List<ProgressbarListener> progressbarListenerList = new ArrayList<>();
  private List<WorkspaceListener> workspaceListenerList = new ArrayList<>();
  private List<ProfileStartStopListener> profileStartStopListenerList = new ArrayList<>();
  private Map<ProfileTaskQueryKey, CollectStartStopListener> collectStartStopListenerMap = new ConcurrentHashMap<>();
  private Map<ProfileTaskQueryKey, CollectStartStopListener> collectStartStopAnalyzeListenerMap = new ConcurrentHashMap<>();
  private Map<ProfileTaskQueryKey, CollectStartStopListener> collectStartStopDashboardListenerMap = new ConcurrentHashMap<>();
  private Map<ProfileTaskQueryKey, AppCacheAddListener> appCacheAddListenerMap = new ConcurrentHashMap<>();

  private List<ProfileAddListener> profileAddListeners = new ArrayList<>();

  @Inject
  public EventListenerImpl() {
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
  public void addProfileButtonStateListener(ToolbarListener toolbarListener) {
    profileButtonStateListenerList.add(toolbarListener);
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
  public void addTemplateStateListener(TemplateListener configListener) {
    templateListenerList.add(configListener);
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
  public void addProfileStartStopListener(ProfileStartStopListener profileStartStopListener) {
    profileStartStopListenerList.add(profileStartStopListener);
  }

  @Override
  public void addCollectStartStopListener(ProfileTaskQueryKey profileTaskQueryKey,
                                          CollectStartStopListener collectStartStopListener) {
    collectStartStopListenerMap.put(profileTaskQueryKey, collectStartStopListener);
  }

  @Override
  public void addCollectStartStopAnalyzeListener(ProfileTaskQueryKey profileTaskQueryKey,
                                                 CollectStartStopListener collectStartStopListener) {
    collectStartStopAnalyzeListenerMap.put(profileTaskQueryKey, collectStartStopListener);
  }

  @Override
  public void addCollectStartStopDashboardListener(ProfileTaskQueryKey profileTaskQueryKey,
                                                   CollectStartStopListener collectStartStopListener) {
    collectStartStopDashboardListenerMap.put(profileTaskQueryKey, collectStartStopListener);
  }

  @Override
  public void addAppCacheAddListener(ProfileTaskQueryKey profileTaskQueryKey,
                                     AppCacheAddListener appCacheAddListener) {
    appCacheAddListenerMap.put(profileTaskQueryKey, appCacheAddListener);
  }

  @Override
  public void addProfileAddListener(ProfileAddListener profileAddListener) {
    profileAddListeners.add(profileAddListener);
  }

  @Override
  public <T> void clearListener(Class<T> genericClass) {
    profileStartStopListenerList.removeIf(genericClass::isInstance);
    collectStartStopListenerMap.entrySet().removeIf(map -> genericClass.isInstance(map.getValue()));
    collectStartStopAnalyzeListenerMap.entrySet().removeIf(map -> genericClass.isInstance(map.getValue()));
  }

  @Override
  public void clearListenerByKey(ProfileTaskQueryKey profileTaskQueryKey) {
    collectStartStopListenerMap.entrySet().removeIf(map -> profileTaskQueryKey.equals(map.getKey()));
    collectStartStopAnalyzeListenerMap.entrySet().removeIf(map -> profileTaskQueryKey.equals(map.getKey()));
  }

  @Override
  public void clearListenerAppCacheByKey(ProfileTaskQueryKey profileTaskQueryKey) {
    appCacheAddListenerMap.entrySet().removeIf(map -> profileTaskQueryKey.equals(map.getKey()));
  }

  @Override
  public void clearListenerDashboardByKey(ProfileTaskQueryKey profileTaskQueryKey) {
    collectStartStopDashboardListenerMap.entrySet().removeIf(map -> profileTaskQueryKey.equals(map.getKey()));
  }

  @Override
  public void addProfileSelectOnNavigator(WorkspaceListener workspaceListener) {
    workspaceListenerList.add(workspaceListener);
  }

  @Override
  public void fireProgressbarVisible(ProgressbarState progressbarState) {
    progressbarListenerList.forEach(l -> l.fireProgressbarVisible(progressbarState));
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
  public void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    collectStartStopListenerMap.entrySet().stream()
        .filter(f -> f.getKey().equals(profileTaskQueryKey))
        .forEach(l -> l.getValue().fireOnStartCollect(profileTaskQueryKey));
    collectStartStopAnalyzeListenerMap.entrySet().stream()
        .filter(f -> f.getKey().equals(profileTaskQueryKey))
        .forEach(l -> l.getValue().fireOnStartCollect(profileTaskQueryKey));
    collectStartStopDashboardListenerMap.entrySet().stream()
        .filter(f -> f.getKey().equals(profileTaskQueryKey))
        .forEach(l -> l.getValue().fireOnStartCollect(profileTaskQueryKey));
  }

  @Override
  public void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey) {
    collectStartStopListenerMap.entrySet().stream()
        .filter(f -> f.getKey().equals(profileTaskQueryKey))
        .forEach(l -> l.getValue().fireOnStopCollect(profileTaskQueryKey));
    collectStartStopAnalyzeListenerMap.entrySet().stream()
        .filter(f -> f.getKey().equals(profileTaskQueryKey))
        .forEach(l -> l.getValue().fireOnStopCollect(profileTaskQueryKey));
    collectStartStopDashboardListenerMap.entrySet().stream()
        .filter(f -> f.getKey().equals(profileTaskQueryKey))
        .forEach(l -> l.getValue().fireOnStopCollect(profileTaskQueryKey));
  }

  @Override
  public void fireOnAddToAppCache(ProfileTaskQueryKey profileTaskQueryKey) {
    AppCacheAddListener listener = appCacheAddListenerMap.get(profileTaskQueryKey);
    if (listener != null) {
      listener.fireOnAddToAppCache(profileTaskQueryKey);
    }
  }

  @Override
  public void fireProfileAdd() {
    profileAddListeners.forEach(ProfileAddListener::fireProfileAdd);
  }

  @Override
  public void fireShowReport(ReportState reportState) {
    reportListenerList.forEach(l -> l.fireShowReport(reportState));
  }

  @Override
  public void addReportStateListener(ReportListener reportListener) {
    reportListenerList.add(reportListener);
  }

  @Override
  public void fireShowDashboard() {
    dashboardListenerList.forEach(l -> l.fireShowDashboard());
  }

  @Override
  public void addDashboardStateListener(DashboardListener dashboardListener) {
    dashboardListenerList.add(dashboardListener);
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
    adHocListenersList.forEach(l -> l.fireShowAdHoc());
  }

  @Override
  public void addAdHocStateListener(AdHocListener adHocListener) {
    adHocListenersList.add(adHocListener);
  }
}
