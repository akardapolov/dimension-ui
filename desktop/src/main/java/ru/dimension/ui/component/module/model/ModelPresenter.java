package ru.dimension.ui.component.module.model;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
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
import ru.dimension.ui.view.table.ModelRowMapper;
import ru.dimension.ui.view.table.row.Rows.ColumnRow;
import ru.dimension.ui.view.table.row.Rows.MetricRow;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;

@Log4j2
public class ModelPresenter {
  private final MessageBroker.Component component;
  private final ModelModel model;
  private final ModelView view;
  private final MessageBroker broker = MessageBroker.getInstance();

  private ProfileTaskQueryKey currentKey;

  public ModelPresenter(MessageBroker.Component component,
                        ModelModel model,
                        ModelView view) {
    this.component = component;
    this.model = model;
    this.view = view;

    this.view.setColumnToggleListener(this::handleColumnToggle);
    this.view.setMetricToggleListener(this::handleMetricToggle);
  }

  public void initializeModel() {
    List<ProfileInfo> profiles = model.getProfileManager().getProfileInfoList();
    view.getProfileTable().setItems(ModelRowMapper.mapProfiles(profiles));
    view.selectFirstProfileRow();
  }

  public void initializePartialModel() {
    List<ProfileInfo> profiles = model.getProfileManager().getProfileInfoList();
    Set<Integer> existingIds = view.getProfileTable().model().items().stream()
        .map(ProfileRow::getId)
        .collect(Collectors.toSet());

    List<ProfileRow> newRows = profiles.stream()
        .filter(p -> !existingIds.contains(p.getId()))
        .map(p -> new ProfileRow(p.getId(), p.getName()))
        .toList();

    if (!newRows.isEmpty()) {
      List<ProfileRow> allRows = new ArrayList<>(view.getProfileTable().model().items());
      allRows.addAll(newRows);
      view.getProfileTable().setItems(allRows);
    }
  }

  public void handleProfileRemoval(int profileId) {
    log.info("Handling profile removal: profileId={}", profileId);

    boolean wasSelected = view.getProfileTable().selectedItem()
        .map(p -> p.getId() == profileId).orElse(false);

    model.clearCacheForProfile(profileId);

    if (wasSelected) {
      log.info("Removed profile was selected, performing full rendering");
      currentKey = null;
      view.clearAllSelections();
      initializeModel();
    } else {
      List<ProfileRow> updated = view.getProfileTable().model().items().stream()
          .filter(r -> r.getId() != profileId)
          .collect(Collectors.toList());
      view.getProfileTable().setItems(updated);
    }
  }

  public void handleProfileSelection(int profileId) {
    ProfileInfo profileInfo = model.getProfileManager().getProfileInfoById(profileId);
    if (profileInfo == null) return;

    List<TaskInfo> tasks = profileInfo.getTaskInfoList().stream()
        .map(id -> model.getProfileManager().getTaskInfoById(id))
        .collect(Collectors.toList());

    view.getTaskTable().setItems(ModelRowMapper.mapTasks(tasks));
    view.selectFirstTaskRow();
  }

  public void handleTaskSelection(int taskId) {
    TaskInfo taskInfo = model.getProfileManager().getTaskInfoById(taskId);
    if (taskInfo == null) return;

    List<QueryInfo> queries = taskInfo.getQueryInfoList().stream()
        .map(id -> model.getProfileManager().getQueryInfoById(id))
        .collect(Collectors.toList());

    view.getQueryTable().setItems(ModelRowMapper.mapQueries(queries));
    view.selectFirstQueryRow();
  }

  public void handleQuerySelection(int profileId, int taskId, int queryId) {
    this.currentKey = new ProfileTaskQueryKey(profileId, taskId, queryId);

    QueryInfo queryInfo = getQueryInfo(profileId, taskId, queryId);
    if (queryInfo == null) {
      log.error("Query not found: {}", queryId);
      return;
    }

    TableInfo tableInfo = model.getProfileManager().getTableInfoByTableName(queryInfo.getName());

    var entry = model.getQueryKeyMap().getOrDefault(
        currentKey,
        new AbstractMap.SimpleEntry<>(new ArrayList<>(), new ArrayList<>())
    );

    List<Metric> selectedMetrics = entry.getKey();
    List<CProfile> selectedColumns = entry.getValue();

    view.setColumnItems(ModelRowMapper.mapColumns(tableInfo, selectedColumns));
    view.setMetricItems(ModelRowMapper.mapMetrics(queryInfo.getMetricList(), selectedMetrics));

    view.selectFirstDetailsRows();

    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component, Module.MANAGE))
                           .action(Action.SET_PROFILE_TASK_QUERY_KEY)
                           .parameter("key", currentKey)
                           .build());
  }

  public void handleMetadataColumnsUpdate(int queryId, String queryName, List<CProfile> columns) {
    log.info("Updating metadata columns for queryId={}, queryName={}, columnCount={}",
             queryId, queryName, columns.size());

    if (currentKey == null) {
      log.debug("No current key set, skipping column view refresh");
      return;
    }

    QueryInfo currentQueryInfo = getQueryInfo(
        currentKey.getProfileId(), currentKey.getTaskId(), currentKey.getQueryId());

    if (currentQueryInfo == null) {
      log.debug("Current query info is null, skipping");
      return;
    }

    if (currentQueryInfo.getId() != queryId && !currentQueryInfo.getName().equals(queryName)) {
      log.debug("Metadata update is for a different query (id={}, name={}), current is (id={}, name={})",
                queryId, queryName, currentQueryInfo.getId(), currentQueryInfo.getName());
      return;
    }

    TableInfo tableInfo = model.getProfileManager().getTableInfoByTableName(queryName);
    if (tableInfo == null) {
      log.warn("TableInfo not found for queryName={}", queryName);
      return;
    }

    var entry = model.getQueryKeyMap().getOrDefault(
        currentKey,
        new AbstractMap.SimpleEntry<>(new ArrayList<>(), new ArrayList<>())
    );

    List<CProfile> selectedColumns = entry.getValue();

    view.setColumnItems(ModelRowMapper.mapColumns(tableInfo, selectedColumns));
    view.selectFirstDetailsRows();

    log.info("Column view refreshed for queryName={}", queryName);
  }

  public void handleQueryListUpdate(int taskId) {
    view.getTaskTable().selectedItem()
        .filter(task -> task.getId() == taskId)
        .ifPresent(task -> handleTaskSelection(taskId));
  }

  public void handleExternalChartRemoval(ProfileTaskQueryKey key, CProfile cProfile) {
    log.info("External chart removal: key={}, cProfile={}", key, cProfile.getColName());

    var entry = model.getQueryKeyMap().get(key);
    if (entry != null) {
      entry.getValue().removeIf(c -> c.getColId() == cProfile.getColId());
      entry.getKey().removeIf(m -> m.getYAxis() != null && m.getYAxis().getColId() == cProfile.getColId());
    }

    if (key.equals(currentKey)) {
      view.uncheckColumn(cProfile);
      view.uncheckMetric(cProfile);
    }
  }

  private void handleColumnToggle(ColumnRow row, boolean isAdded) {
    if (!validateInteraction() || !row.hasOrigin()) return;

    CProfile cProfile = row.getOrigin();
    updateCache(currentKey, cProfile, isAdded);

    Metric chartMetric = createMetricFromColumn(currentKey, cProfile);
    if (chartMetric != null) {
      sendChartMessage(isAdded, currentKey, chartMetric);
    }
  }

  private void handleMetricToggle(MetricRow row, boolean isAdded) {
    if (!validateInteraction() || !row.hasOrigin()) return;

    Metric metric = row.getOrigin();
    updateCache(currentKey, metric, isAdded);
    sendChartMessage(isAdded, currentKey, metric);
  }

  private boolean validateInteraction() {
    if (currentKey == null) return false;

    ProfileInfo profileInfo = model.getProfileManager().getProfileInfoById(currentKey.getProfileId());
    if (profileInfo != null && profileInfo.getStatus() != RunStatus.RUNNING) {
      view.showNotRunningMessage(profileInfo.getName());
    }
    return true;
  }

  private void updateCache(ProfileTaskQueryKey key, Object item, boolean add) {
    var entry = model.getQueryKeyMap()
        .computeIfAbsent(key, k -> new AbstractMap.SimpleEntry<>(new ArrayList<>(), new ArrayList<>()));

    if (item instanceof CProfile cp) {
      List<CProfile> list = entry.getValue();
      if (add) {
        if (list.stream().noneMatch(c -> c.getColId() == cp.getColId())) list.add(cp);
      } else {
        list.removeIf(c -> c.getColId() == cp.getColId());
      }
    } else if (item instanceof Metric m) {
      List<Metric> list = entry.getKey();
      if (add) {
        if (list.stream().noneMatch(mx -> mx.getId() == m.getId())) list.add(m);
      } else {
        list.removeIf(mx -> mx.getId() == m.getId());
      }
    }
  }

  private Metric createMetricFromColumn(ProfileTaskQueryKey key, CProfile cProfile) {
    QueryInfo q = getQueryInfo(key.getProfileId(), key.getTaskId(), key.getQueryId());
    if (q == null) return null;

    TableInfo t = model.getProfileManager().getTableInfoByTableName(q.getName());
    if (t == null) return null;

    return new Metric(t, cProfile);
  }

  private QueryInfo getQueryInfo(int profileId, int taskId, int queryId) {
    return model.getProfileManager().getQueryInfoList(profileId, taskId).stream()
        .filter(q -> q.getId() == queryId)
        .findFirst()
        .orElse(null);
  }

  private void sendChartMessage(boolean add, ProfileTaskQueryKey key, Metric metric) {
    broker.sendMessage(Message.builder()
                           .destination(Destination.withDefault(component, Module.CHARTS))
                           .action(add ? Action.ADD_CHART : Action.REMOVE_CHART)
                           .parameter("key", key)
                           .parameter("metric", metric)
                           .build());
  }
}