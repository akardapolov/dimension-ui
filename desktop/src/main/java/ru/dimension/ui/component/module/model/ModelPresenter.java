package ru.dimension.ui.component.module.model;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.table.DefaultTableModel;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.RunStatus;
import ru.dimension.ui.model.column.ColumnNames;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;

@Log4j2
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

  public void initializePartialModel() {
    List<ProfileInfo> profileInfoList = model.getProfileManager().getProfileInfoList();

    DefaultTableModel modelTable = view.getProfileTableCase().getDefaultTableModel();

    // Collect existing profile IDs from the table
    List<Integer> existingIds = new ArrayList<>();
    for (int row = 0; row < modelTable.getRowCount(); row++) {
      Object idObj = modelTable.getValueAt(row, ColumnNames.ID.ordinal());
      if (idObj instanceof Integer) {
        existingIds.add((Integer) idObj);
      } else {
        existingIds.add(Integer.parseInt(idObj.toString()));
      }
    }

    // Append only new profiles
    profileInfoList.stream()
        .filter(p -> !existingIds.contains(p.getId()))
        .forEach(p -> modelTable.addRow(new Object[]{p.getId(), p.getName()}));
  }

  public void handleProfileRemoval(int profileId) {
    log.info("Handling profile removal: profileId={}", profileId);

    // Check if the removed profile is currently selected
    int selectedProfileId = view.getSelectedProfileId();
    boolean wasSelected = (selectedProfileId == profileId);

    // Clear cached data for this profile
    model.clearCacheForProfile(profileId);

    if (wasSelected) {
      // Profile was selected - clear selection and do full rendering
      log.info("Removed profile was selected, performing full rendering");
      view.clearAllSelections();
      initializeModel();
    } else {
      // Profile was not selected - do partial rendering
      log.info("Removed profile was not selected, performing partial rendering");
      view.removeProfileFromTable(profileId);
    }
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