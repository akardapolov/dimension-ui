package ru.dimension.ui.view.handler.task;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.ProfileAddEvent;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Query;
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
    taskPanel.getJTextFieldTask().setText(taskPanel.getJTextFieldTask().getText() + "_copy");
    String currentDesc = taskPanel.getJTextFieldDescription().getText();
    if (currentDesc != null) {
      taskPanel.getJTextFieldDescription().setText(currentDesc + "_copy");
    }
    setEditMode(true);
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
  }

  @Override
  public void onSave() {
    String name = taskPanel.getJTextFieldTask().getText().trim();
    if (name.isEmpty()) return;

    if (isNew) {
      boolean nameExists = profileManager.getTaskInfoList().stream()
          .anyMatch(t -> t.getName().equals(name));
      if (nameExists) {
        throw new NotFoundException(
            String.format("Name %s already exists, please enter another one.", name));
      }
    }

    TaskInfo info = new TaskInfo();
    int id = isNew
        ? profileManager.getTaskInfoList().stream()
        .map(TaskInfo::getId).max(Comparator.naturalOrder()).orElse(0) + 1
        : context.getSelectedTaskId();

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
    eventBus.publish(new ProfileAddEvent());
  }

  @Override
  public void onCancel() {
    setEditMode(false);
  }

  private void setEditMode(boolean edit) {
    if (edit) ButtonPanelBindings.setEditMode(buttonPanel);
    else ButtonPanelBindings.setViewMode(buttonPanel, context.getSelectedTaskId() != null);

    taskPanel.getJTextFieldTask().setEditable(edit);
    taskPanel.getJTextFieldDescription().setEditable(edit);
    taskPanel.getTaskConnectionComboBox().setEnabled(edit);
    taskPanel.getRadioButtonPanel().setButtonView();
    if (!edit) taskPanel.getRadioButtonPanel().setButtonNotView();

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

  private int getPullTimeout() {
    if (taskPanel.getRadioButtonPanel().getJRadioButton1().isSelected()) return 1;
    if (taskPanel.getRadioButtonPanel().getJRadioButton3().isSelected()) return 3;
    if (taskPanel.getRadioButtonPanel().getJRadioButton5().isSelected()) return 5;
    if (taskPanel.getRadioButtonPanel().getJRadioButton10().isSelected()) return 10;
    return 30;
  }

  private int getConnectionId() {
    String name = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(0).toString();
    return profileManager.getConnectionInfoList().stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow().getId();
  }

  private List<Integer> processSelectedQueries() {
    TTTable<QueryTableRow, JXTable> tt = multiSelectPanel.getSelectedQueryCase().getTypedTable();
    List<Integer> ids = new ArrayList<>();
    for (QueryTableRow row : tt.model().items()) {
      QueryInfo exist = profileManager.getQueryInfoList().stream().filter(q -> q.getName().equals(row.getName())).findFirst().orElse(null);
      if (exist != null) ids.add(exist.getId());
      else ids.add(copyQueryFromTemplate(row.getId()));
    }
    return ids;
  }

  private int copyQueryFromTemplate(int templateQueryId) {
    Query q = templateManager.getConfigList(Query.class).stream().filter(x -> x.getId() == templateQueryId).findFirst().orElseThrow();
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
    return profileManager.getProfileInfoList().stream().anyMatch(p -> p.getTaskInfoList().contains(taskId));
  }
}