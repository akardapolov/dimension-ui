package ru.dimension.ui.component.module.model;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.RunStatus;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;

public class ModelPresenter {
  private final MessageBroker.Component component;
  private final ModelModel model;
  private final ModelView view;

  private final MessageBroker broker = MessageBroker.getInstance();

  public ModelPresenter(MessageBroker.Component component,
                        ModelModel model,
                        ModelView view) {
    this.component = component;
    this.model = model;
    this.view = view;
  }

  public void initializeModel() {
    List<ProfileInfo> profileInfoList = model.getProfileManager().getProfileInfoList();
    view.initializeProfileTable(profileInfoList);
    view.selectFirstProfileRow();
  }

  public void handleProfileSelection(int profileId) {
    ProfileInfo profileInfo = model.getProfileManager().getProfileInfoById(profileId);
    List<TaskInfo> taskInfoList = profileInfo.getTaskInfoList().stream()
        .map(taskId -> model.getProfileManager().getTaskInfoById(taskId))
        .collect(Collectors.toList());
    view.updateTaskTable(taskInfoList);
    view.selectFirstTaskRow();
  }

  public void handleTaskSelection(int taskId) {
    TaskInfo taskInfo = model.getProfileManager().getTaskInfoById(taskId);
    List<QueryInfo> queryInfoList = taskInfo.getQueryInfoList().stream()
        .map(queryId -> model.getProfileManager().getQueryInfoById(queryId))
        .collect(Collectors.toList());
    view.updateQueryTable(queryInfoList);
    view.selectFirstQueryRow();
  }

  public void handleQuerySelection(int profileId,
                                   int taskId,
                                   int queryId) {
    QueryInfo queryInfo = model.getProfileManager().getQueryInfoList(profileId, taskId).stream()
        .filter(q -> q.getId() == queryId)
        .findFirst()
        .orElseThrow();
    TableInfo tableInfo = model.getProfileManager().getTableInfoByTableName(queryInfo.getName());
    ProfileTaskQueryKey key = new ProfileTaskQueryKey(profileId, taskId, queryId);

    Map.Entry<List<Metric>, List<CProfile>> entry = model.getQueryKeyMap().get(key);
    List<Metric> selectedMetrics = entry == null ? new ArrayList<>() : entry.getKey();
    List<CProfile> selectedColumns = entry == null ? new ArrayList<>() : entry.getValue();

    view.updateColumnAndMetricTables(tableInfo, queryInfo.getMetricList());
    view.restoreSelections(selectedColumns, selectedMetrics);

    view.setupColumnEditors(
        getColumnStackChartPanelHandler(key),
        getMetricStackChartPanelHandler(key),
        tableInfo::getCProfiles,
        queryInfo::getMetricList
    );
    view.selectFirstRows();

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component, Module.MANAGE))
                           .action(Action.SET_PROFILE_TASK_QUERY_KEY)
                           .parameter("key", key)
                           .build());
  }

  private Supplier<ModelHandler<CProfile>> getColumnStackChartPanelHandler(ProfileTaskQueryKey key) {
    return () -> (cProfile, add) -> {
      ProfileInfo profileInfo = model.getProfileManager().getProfileInfoById(key.getProfileId());
      if (profileInfo.getStatus() != RunStatus.RUNNING) {
        view.showNotRunningMessage(profileInfo.getName());
      }

      updateColumnSelection(key, cProfile, add);

      QueryInfo queryInfo = model.getProfileManager().getQueryInfoById(key.getQueryId());
      TableInfo tableInfo = model.getProfileManager().getTableInfoByTableName(queryInfo.getName());
      Metric metric = new Metric(tableInfo, cProfile);

      if (add) {
        broker.sendMessage(Message.builder()
                               .destination(Destination.withDefault(component, Module.CHARTS))
                               .action(Action.ADD_CHART)
                               .parameter("key", key)
                               .parameter("metric", metric)
                               .build());
      } else {
        broker.sendMessage(Message.builder()
                               .destination(Destination.withDefault(component, Module.CHARTS))
                               .action(Action.REMOVE_CHART)
                               .parameter("key", key)
                               .parameter("metric", metric)
                               .build());
      }
    };
  }

  private void updateColumnSelection(ProfileTaskQueryKey key,
                                     CProfile cProfile,
                                     boolean add) {
    Map.Entry<List<Metric>, List<CProfile>> entry = model.getQueryKeyMap()
        .computeIfAbsent(key, k -> new AbstractMap.SimpleEntry<>(new ArrayList<>(), new ArrayList<>()));
    if (add) {
      entry.getValue().add(cProfile);
    } else {
      entry.getValue().remove(cProfile);
    }
  }

  private Supplier<ModelHandler<Metric>> getMetricStackChartPanelHandler(ProfileTaskQueryKey key) {
    return () -> (metric, add) -> {
      ProfileInfo profileInfo = model.getProfileManager().getProfileInfoById(key.getProfileId());
      if (profileInfo.getStatus() != RunStatus.RUNNING) {
        view.showNotRunningMessage(profileInfo.getName());
      }

      updateMetricSelection(key, metric, add);
    };
  }

  private void updateMetricSelection(ProfileTaskQueryKey key,
                                     Metric metric,
                                     boolean add) {
    Map.Entry<List<Metric>, List<CProfile>> entry = model.getQueryKeyMap()
        .computeIfAbsent(key, k -> new AbstractMap.SimpleEntry<>(new ArrayList<>(), new ArrayList<>()));
    if (add) {
      entry.getKey().add(metric);
    } else {
      entry.getKey().remove(metric);
    }
  }
}
