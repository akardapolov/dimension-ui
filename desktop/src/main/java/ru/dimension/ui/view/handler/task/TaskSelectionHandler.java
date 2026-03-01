package ru.dimension.ui.view.handler.task;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.UpdateQueryList;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.helper.event.EventRouteRegistry;
import ru.dimension.ui.helper.event.EventUtils;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.sql.GatherDataMode;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.view.handler.core.AbstractTableSelectionHandler;
import ru.dimension.ui.view.handler.core.ButtonPanelBindings;
import ru.dimension.ui.view.handler.core.ConfigSelectionContext;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.task.MultiSelectQueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;
import ru.dimension.ui.view.table.row.Rows.QueryTableRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Singleton
public final class TaskSelectionHandler extends AbstractTableSelectionHandler<TaskRow> {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;
  private final ConfigSelectionContext context;
  private final TaskPanel taskPanel;
  private final MultiSelectQueryPanel multiSelectPanel;
  private final ButtonPanel buttonPanel;
  private final JCheckBox checkboxConfig;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final EventBus eventBus;

  @SuppressWarnings("FieldCanBeLocal")
  private final EventRouteRegistry eventRegistry;

  @Inject
  public TaskSelectionHandler(@Named("taskConfigCase") JXTableCase taskCase,
                              @Named("connectionConfigCase") JXTableCase connectionCase,
                              @Named("queryConfigCase") JXTableCase queryCase,
                              @Named("profileManager") ProfileManager profileManager,
                              @Named("templateManager") TemplateManager templateManager,
                              @Named("configSelectionContext") ConfigSelectionContext context,
                              @Named("taskConfigPanel") TaskPanel taskPanel,
                              @Named("multiSelectQueryPanel") MultiSelectQueryPanel multiSelectPanel,
                              @Named("taskButtonPanel") ButtonPanel buttonPanel,
                              @Named("checkboxConfig") JCheckBox checkboxConfig,
                              @Named("eventBus") EventBus eventBus) {
    super(taskCase);
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.profileManager = profileManager;
    this.templateManager = templateManager;
    this.context = context;
    this.taskPanel = taskPanel;
    this.multiSelectPanel = multiSelectPanel;
    this.buttonPanel = buttonPanel;
    this.checkboxConfig = checkboxConfig;
    this.eventBus = eventBus;

    this.eventRegistry = EventRouteRegistry.forComponent(Component.CONFIGURATION, EventUtils::getComponent)
        .routeGlobal(UpdateQueryList.class, this::handleUpdateQueryList)
        .register(eventBus);

    this.checkboxConfig.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        applyHierarchyMode();
      } else {
        applyFullMode();
      }
    });

    taskPanel.getTaskConnectionComboBox().addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        updateQueryMultiSelect(context.getSelectedTaskId());
      }
    });

    bind();
  }

  @Override
  protected void onSelection(Optional<TaskRow> item) {
    Integer id = item.map(TaskRow::getId).orElse(null);
    context.setSelectedTaskId(id);
    ButtonPanelBindings.setViewMode(buttonPanel, id != null);

    updateFields(id);
    updateQueryMultiSelect(id);

    if (checkboxConfig.isSelected()) {
      applyHierarchyMode();
    }
  }

  private void handleUpdateQueryList(UpdateQueryList event) {
    if (Objects.equals(event.taskId(), context.getSelectedTaskId())) {
      updateQueryMultiSelect(event.taskId());
    }
  }

  private void updateFields(Integer id) {
    if (id == null) {
      taskPanel.getJTextFieldTask().setText("");
      taskPanel.getJTextFieldDescription().setText("");
      return;
    }
    TaskInfo info = profileManager.getTaskInfoById(id);
    if (info != null) {
      taskPanel.getJTextFieldTask().setText(info.getName());
      taskPanel.getJTextFieldDescription().setText(info.getDescription());
      taskPanel.getRadioButtonPanel().setSelectedRadioButton(info.getPullTimeout() + " sec");
      syncConnectionCombo(info.getConnectionId());
    }
  }

  private void syncConnectionCombo(int connectionId) {
    List<ConnectionInfo> all = profileManager.getConnectionInfoList();
    List<List<?>> data = new ArrayList<>();

    all.stream().filter(c -> c.getId() == connectionId).forEach(c -> data.add(toComboRow(c)));
    all.stream().filter(c -> c.getId() != connectionId).forEach(c -> data.add(toComboRow(c)));

    taskPanel.getTaskConnectionComboBox().setTableData(data);

    if (!data.isEmpty()) {
      taskPanel.getTaskConnectionComboBox().setSelectedIndex(0);
    }
  }

  private List<?> toComboRow(ConnectionInfo c) {
    return Arrays.asList(c.getName(), c.getUserName(), c.getUrl(), c.getJar(), c.getDriver(),
                         c.getType() != null ? c.getType() : ConnectionType.JDBC);
  }

  private void updateQueryMultiSelect(Integer id) {
    TTTable<QueryTableRow, JXTable> ttSelected = multiSelectPanel.getSelectedQueryCase().getTypedTable();
    TTTable<QueryTableRow, JXTable> ttAvailable = multiSelectPanel.getQueryListCase().getTypedTable();
    TTTable<QueryTableRow, JXTable> ttTemplate = multiSelectPanel.getTemplateListQueryCase().getTypedTable();

    List<Integer> selectedIds = (id == null) ? List.of() :
        Optional.ofNullable(profileManager.getTaskInfoById(id))
            .map(TaskInfo::getQueryInfoList).orElse(List.of());

    List<QueryInfo> allQueries = profileManager.getQueryInfoList();

    ttSelected.setItems(allQueries.stream()
                            .filter(q -> selectedIds.contains(q.getId()))
                            .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
                            .collect(Collectors.toList()));

    List<QueryTableRow> availableRows = buildAvailableQueryList(id, selectedIds);
    ttAvailable.setItems(availableRows);

    String driver = getSelectedDriver();
    if (driver != null) {
      ttTemplate.setItems(templateManager.getQueryListByConnDriver(driver).stream()
                              .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
                              .collect(Collectors.toList()));
    } else {
      ttTemplate.setItems(List.of());
    }
  }

  private List<QueryTableRow> buildAvailableQueryList(Integer taskId, List<Integer> selectedIds) {
    List<QueryTableRow> result = new ArrayList<>();
    Set<Integer> addedIds = new HashSet<>(selectedIds);

    String driver = getSelectedDriver();
    ConnectionType connectionType = getSelectedConnectionType();
    boolean hasConnection = hasSelectedConnection();

    if (hasConnection && connectionType == ConnectionType.HTTP) {
      profileManager.getQueryInfoList().stream()
          .filter(q -> q.getGatherDataMode() == GatherDataMode.BY_CLIENT_HTTP)
          .filter(q -> !addedIds.contains(q.getId()))
          .forEach(q -> {
            result.add(new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()));
            addedIds.add(q.getId());
          });

      return result;
    }

    if (driver != null) {
      profileManager.getQueryInfoListByConnDriver(driver).stream()
          .filter(q -> !addedIds.contains(q.getId()))
          .forEach(q -> {
            result.add(new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()));
            addedIds.add(q.getId());
          });
    }

    profileManager.getOrphanQueryInfoList().stream()
        .filter(q -> !addedIds.contains(q.getId()))
        .forEach(q -> {
          result.add(new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()));
          addedIds.add(q.getId());
        });

    return result;
  }

  private ConnectionType getSelectedConnectionType() {
    try {
      Object typeObj = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(5);
      if (typeObj instanceof ConnectionType) {
        return (ConnectionType) typeObj;
      }
      return ConnectionType.valueOf(typeObj.toString());
    } catch (Exception e) {
      return null;
    }
  }

  private boolean hasSelectedConnection() {
    try {
      List<?> row = taskPanel.getTaskConnectionComboBox().getSelectedRow();
      return row != null && !row.isEmpty() && row.get(0) != null;
    } catch (Exception e) {
      return false;
    }
  }

  private String getSelectedDriver() {
    try {
      Object v = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4);
      return v == null ? null : v.toString();
    } catch (Exception e) {
      return null;
    }
  }

  private void applyHierarchyMode() {
    Integer tId = context.getSelectedTaskId();
    TTTable<ConnectionRow, JXTable> ttConn = connectionCase.getTypedTable();
    TTTable<QueryRow, JXTable> ttQuery = queryCase.getTypedTable();

    if (tId == null) {
      connectionCase.clearTable();
      queryCase.clearTable();
      return;
    }

    TaskInfo info = profileManager.getTaskInfoById(tId);
    if (info == null) {
      connectionCase.clearTable();
      queryCase.clearTable();
      return;
    }

    ConnectionInfo ci = profileManager.getConnectionInfoById(info.getConnectionId());
    if (ci != null) {
      ttConn.setItems(List.of(new ConnectionRow(ci.getId(), ci.getName(), ci.getType(), ci.getDbType())));
    } else {
      ttConn.setItems(List.of());
    }

    ttQuery.setItems(info.getQueryInfoList().stream()
                         .map(profileManager::getQueryInfoById)
                         .filter(Objects::nonNull)
                         .map(q -> new QueryRow(q.getId(), q.getName()))
                         .collect(Collectors.toList()));

    selectFirstRow(connectionCase);
    selectFirstRow(queryCase);
  }

  private void applyFullMode() {
    Integer currentConnId = context.getSelectedConnectionId();
    Integer currentQueryId = context.getSelectedQueryId();

    TTTable<ConnectionRow, JXTable> ttConn = connectionCase.getTypedTable();
    TTTable<QueryRow, JXTable> ttQuery = queryCase.getTypedTable();

    List<ConnectionRow> connRows = profileManager.getConnectionInfoList().stream()
        .map(c -> {
          ConnectionInfo ci = profileManager.getConnectionInfoById(c.getId());
          return new ConnectionRow(c.getId(), c.getName(), c.getType(), ci != null ? ci.getDbType() : null);
        })
        .collect(Collectors.toList());
    ttConn.setItems(connRows);

    List<QueryRow> queryRows = profileManager.getQueryInfoList().stream()
        .map(q -> new QueryRow(q.getId(), q.getName()))
        .collect(Collectors.toList());
    ttQuery.setItems(queryRows);

    restoreConnectionSelection(connRows, currentConnId);
    restoreQuerySelection(queryRows, currentQueryId);
  }

  private void restoreConnectionSelection(List<ConnectionRow> rows, Integer connId) {
    if (connId == null || rows.isEmpty()) {
      return;
    }

    JXTable table = connectionCase.getJxTable();
    if (table == null) {
      return;
    }

    SwingUtilities.invokeLater(() -> {
      for (int i = 0; i < rows.size(); i++) {
        if (Objects.equals(rows.get(i).getId(), connId)) {
          table.setRowSelectionInterval(i, i);
          return;
        }
      }
    });
  }

  private void restoreQuerySelection(List<QueryRow> rows, Integer queryId) {
    if (queryId == null || rows.isEmpty()) {
      return;
    }

    JXTable table = queryCase.getJxTable();
    if (table == null) {
      return;
    }

    SwingUtilities.invokeLater(() -> {
      for (int i = 0; i < rows.size(); i++) {
        if (Objects.equals(rows.get(i).getId(), queryId)) {
          table.setRowSelectionInterval(i, i);
          return;
        }
      }
    });
  }

  private void selectFirstRow(JXTableCase tableCase) {
    JXTable table = tableCase.getJxTable();
    if (table == null) return;

    SwingUtilities.invokeLater(() -> {
      if (table.getRowCount() > 0) {
        table.setRowSelectionInterval(0, 0);
      } else {
        table.clearSelection();
      }
    });
  }
}