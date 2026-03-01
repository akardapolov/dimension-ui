package ru.dimension.ui.view.handler.task;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.ProfileAddEvent;
import ru.dimension.ui.bus.event.UpdateQueryList;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.handler.core.ButtonPanelBindings;
import ru.dimension.ui.view.handler.core.ConfigSelectionContext;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.task.MultiSelectQueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows.QueryTableRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Singleton
public final class TaskButtonPanelHandler implements ButtonPanelBindings.CrudActions {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;
  private final EventBus eventBus;
  private final ConfigSelectionContext context;
  private final TaskPanel taskPanel;
  private final MultiSelectQueryPanel multiSelectPanel;
  private final ButtonPanel buttonPanel;
  private final JXTableCase taskCase;
  private final JXTableCase profileCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final ConfigTab configTab;
  private final JCheckBox checkboxConfig;
  private boolean isNew = false;

  @Inject
  public TaskButtonPanelHandler(ProfileManager profileManager,
                                TemplateManager templateManager,
                                EventBus eventBus,
                                @Named("configSelectionContext") ConfigSelectionContext context,
                                @Named("taskConfigPanel") TaskPanel taskPanel,
                                @Named("multiSelectQueryPanel") MultiSelectQueryPanel multiSelectPanel,
                                @Named("taskButtonPanel") ButtonPanel buttonPanel,
                                @Named("taskConfigCase") JXTableCase taskCase,
                                @Named("profileConfigCase") JXTableCase profileCase,
                                @Named("connectionConfigCase") JXTableCase connectionCase,
                                @Named("queryConfigCase") JXTableCase queryCase,
                                @Named("configTab") ConfigTab configTab,
                                @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.profileManager = profileManager;
    this.templateManager = templateManager;
    this.eventBus = eventBus;
    this.context = context;
    this.taskPanel = taskPanel;
    this.multiSelectPanel = multiSelectPanel;
    this.buttonPanel = buttonPanel;
    this.taskCase = taskCase;
    this.profileCase = profileCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.configTab = configTab;
    this.checkboxConfig = checkboxConfig;

    ButtonPanelBindings.bind(buttonPanel, this);
  }

  @Override
  public void onNew() {
    isNew = true;
    taskPanel.getJTextFieldTask().setText("");
    taskPanel.getJTextFieldDescription().setText("");
    multiSelectPanel.getSelectedQueryCase().clearTable();
    setEditMode(true);
  }

  @Override
  public void onEdit() {
    isNew = false;
    setEditMode(true);
  }

  @Override
  public void onCopy() {
    isNew = true;
    Integer selectedId = context.getSelectedTaskId();

    if (selectedId == null) {
      selectedId = profileManager.getTaskInfoList().stream()
          .map(TaskInfo::getId).max(Comparator.naturalOrder()).orElse(null);
    }

    if (selectedId != null) {
      TaskInfo currentTask = profileManager.getTaskInfoById(selectedId);
      if (currentTask != null) {
        taskPanel.getJTextFieldTask().setText(currentTask.getName() + "_copy");
        String currentDesc = currentTask.getDescription();
        if (currentDesc != null && !currentDesc.isEmpty()) {
          taskPanel.getJTextFieldDescription().setText(currentDesc + "_copy");
        } else {
          taskPanel.getJTextFieldDescription().setText("");
        }
      } else {
        appendCopySuffix();
      }
    } else {
      appendCopySuffix();
    }

    setEditMode(true);
  }

  private void appendCopySuffix() {
    String currentTask = taskPanel.getJTextFieldTask().getText();
    taskPanel.getJTextFieldTask().setText((currentTask == null ? "" : currentTask) + "_copy");

    String currentDesc = taskPanel.getJTextFieldDescription().getText();
    if (currentDesc != null && !currentDesc.isEmpty()) {
      taskPanel.getJTextFieldDescription().setText(currentDesc + "_copy");
    }
  }

  @Override
  public void onDelete() {
    Integer id = context.getSelectedTaskId();
    if (id == null) return;
    if (isUsedInProfiles(id)) {
      JOptionPane.showMessageDialog(null, "Task is used in profiles!", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    profileManager.deleteTask(id, profileManager.getTaskInfoById(id).getName());
    eventBus.publish(new ProfileAddEvent());
    selectFirstOrClear();
  }

  @Override
  public void onSave() {
    String name = taskPanel.getJTextFieldTask().getText();
    if (name == null || name.trim().isEmpty()) return;
    name = name.trim();

    if (isNew) {
      final String checkName = name;
      boolean nameExists = profileManager.getTaskInfoList().stream()
          .anyMatch(t -> t.getName().equals(checkName));
      if (nameExists) {
        throw new NotFoundException(
            String.format("Name %s already exists, please enter another one.", name));
      }
    }

    TaskInfo info = new TaskInfo();
    int id = isNew
        ? profileManager.getTaskInfoList().stream()
        .map(TaskInfo::getId).max(Comparator.naturalOrder()).orElse(0) + 1
        : (context.getSelectedTaskId() != null ? context.getSelectedTaskId() : 1);

    info.setId(id);
    info.setName(name);
    info.setDescription(taskPanel.getJTextFieldDescription().getText());
    info.setPullTimeout(getPullTimeout());
    info.setConnectionId(getConnectionId());
    info.setQueryInfoList(processSelectedQueries());

    if (isNew) {
      profileManager.addTask(info);
    } else {
      profileManager.updateTask(info);
    }

    context.setSelectedTaskId(info.getId());

    setEditMode(false);
    isNew = false;

    eventBus.publish(new ProfileAddEvent());
    eventBus.publish(new UpdateQueryList(info.getId()));

    restoreTaskSelection(info.getId());
  }

  @Override
  public void onCancel() {
    Integer currentId = context.getSelectedTaskId();
    setEditMode(false);
    isNew = false;
    if (currentId != null) {
      restoreTaskSelection(currentId);
    } else {
      selectFirstOrClear();
    }
  }

  private void restoreTaskSelection(int targetId) {
    JXTable table = taskCase.getJxTable();
    if (table == null) return;

    TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();
    if (tt != null && tt.model() != null) {
      List<TaskRow> rows = tt.model().items();
      for (int i = 0; i < rows.size(); i++) {
        if (Objects.equals(rows.get(i).getId(), targetId)) {
          table.setRowSelectionInterval(i, i);
          return;
        }
      }
    }
    selectFirstOrClearInternal(table);
  }

  private void selectFirstOrClear() {
    JXTable table = taskCase.getJxTable();
    if (table == null) return;
    selectFirstOrClearInternal(table);
  }

  private void selectFirstOrClearInternal(JXTable table) {
    if (table.getRowCount() > 0) {
      table.setRowSelectionInterval(0, 0);
    } else {
      table.clearSelection();
      context.setSelectedTaskId(null);
      taskPanel.getJTextFieldTask().setText("");
      taskPanel.getJTextFieldDescription().setText("");
      multiSelectPanel.getSelectedQueryCase().clearTable();
      multiSelectPanel.getQueryListCase().clearTable();
      ButtonPanelBindings.setViewMode(buttonPanel, false);
    }
  }

  private void setEditMode(boolean edit) {
    if (edit) ButtonPanelBindings.setEditMode(buttonPanel);
    else ButtonPanelBindings.setViewMode(buttonPanel, context.getSelectedTaskId() != null);

    taskPanel.getJTextFieldTask().setEditable(edit);
    taskPanel.getJTextFieldDescription().setEditable(edit);
    taskPanel.getTaskConnectionComboBox().setEnabled(edit);
    taskPanel.getRadioButtonPanel().setButtonView();
    if (!edit) taskPanel.getRadioButtonPanel().setButtonNotView();

    setMultiSelectEnabled(edit);

    boolean enable = !edit;
    taskCase.getJxTable().setEnabled(enable);
    profileCase.getJxTable().setEnabled(enable);
    connectionCase.getJxTable().setEnabled(enable);
    queryCase.getJxTable().setEnabled(enable);
    checkboxConfig.setEnabled(enable);

    configTab.setEnabledAt(0, enable);
    configTab.setEnabledAt(1, true);
    configTab.setEnabledAt(2, enable);
    configTab.setEnabledAt(3, enable);
  }

  private void setMultiSelectEnabled(boolean edit) {
    if (edit) {
      multiSelectPanel.getPickAllBtn().setEnabled(true);
      multiSelectPanel.getUnPickAllBtn().setEnabled(true);

      boolean hasSource = multiSelectPanel.getQueryListCase().getJxTable().getRowCount() > 0
          || multiSelectPanel.getTemplateListQueryCase().getJxTable().getRowCount() > 0;
      boolean hasSelected = multiSelectPanel.getSelectedQueryCase().getJxTable().getRowCount() > 0;

      multiSelectPanel.getPickBtn().setEnabled(hasSource);
      multiSelectPanel.getUnPickBtn().setEnabled(hasSelected);
    } else {
      multiSelectPanel.getPickBtn().setEnabled(false);
      multiSelectPanel.getUnPickBtn().setEnabled(false);
      multiSelectPanel.getPickAllBtn().setEnabled(false);
      multiSelectPanel.getUnPickAllBtn().setEnabled(false);
    }
  }

  private int getPullTimeout() {
    try {
      if (taskPanel.getRadioButtonPanel().getJRadioButton1().isSelected()) return 1;
      if (taskPanel.getRadioButtonPanel().getJRadioButton3().isSelected()) return 3;
      if (taskPanel.getRadioButtonPanel().getJRadioButton5().isSelected()) return 5;
      if (taskPanel.getRadioButtonPanel().getJRadioButton10().isSelected()) return 10;
    } catch (Exception ignored) { }
    return 30;
  }

  private int getConnectionId() {
    try {
      List<?> selectedRow = taskPanel.getTaskConnectionComboBox().getSelectedRow();
      if (selectedRow != null && !selectedRow.isEmpty()) {
        String name = selectedRow.get(0).toString();
        return profileManager.getConnectionInfoList().stream()
            .filter(c -> c.getName().equals(name))
            .findFirst()
            .map(ConnectionInfo::getId)
            .orElse(0);
      }
    } catch (Exception ignored) { }

    return profileManager.getConnectionInfoList().stream()
        .findFirst()
        .map(ConnectionInfo::getId)
        .orElse(0);
  }

  private List<Integer> processSelectedQueries() {
    List<Integer> ids = new ArrayList<>();
    try {
      TTTable<QueryTableRow, JXTable> tt = multiSelectPanel.getSelectedQueryCase().getTypedTable();
      if (tt == null || tt.model() == null) return ids;

      for (QueryTableRow row : tt.model().items()) {
        QueryInfo exist = profileManager.getQueryInfoList().stream()
            .filter(q -> q.getName().equals(row.getName()))
            .findFirst()
            .orElse(null);

        if (exist != null) {
          ids.add(exist.getId());
        } else {
          int templateId = copyQueryFromTemplate(row.getId());
          if (templateId > 0) {
            ids.add(templateId);
          }
        }
      }
    } catch (Exception ignored) { }
    return ids;
  }

  private int copyQueryFromTemplate(int templateQueryId) {
    Query q = templateManager.getConfigList(Query.class).stream()
        .filter(x -> x.getId() == templateQueryId)
        .findFirst()
        .orElse(null);

    if (q == null) return 0;

    QueryInfo qi = new QueryInfo().setName(q.getName()).setText(q.getText()).setDescription(q.getDescription())
        .setGatherDataMode(q.getGatherDataMode()).setMetricList(q.getMetricList());
    qi.setId(profileManager.getQueryInfoList().stream().map(QueryInfo::getId).max(Comparator.naturalOrder()).orElse(0) + 1);
    profileManager.addQuery(qi);

    TableInfo tableInfo = new TableInfo();
    tableInfo.setTableName(qi.getName());
    profileManager.addTable(tableInfo);

    return qi.getId();
  }

  private boolean isUsedInProfiles(int taskId) {
    return profileManager.getProfileInfoList().stream()
        .filter(p -> p.getTaskInfoList() != null)
        .anyMatch(p -> p.getTaskInfoList().contains(taskId));
  }
}