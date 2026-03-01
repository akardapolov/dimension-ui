package ru.dimension.ui.view.handler.profile;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.ProfileAddEvent;
import ru.dimension.ui.bus.event.ProfileRemoveEvent;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.RunStatus;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.handler.core.ButtonPanelBindings;
import ru.dimension.ui.view.handler.core.ConfigSelectionContext;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.view.panel.config.profile.ProfilePanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Singleton
public final class ProfileButtonPanelHandler implements ButtonPanelBindings.CrudActions {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;
  private final EventBus eventBus;
  private final ConfigSelectionContext context;
  private final ProfilePanel profilePanel;
  private final MultiSelectTaskPanel multiSelectPanel;
  private final ButtonPanel buttonPanel;
  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;
  private final ConfigTab configTab;
  private final JCheckBox checkboxConfig;
  private boolean isNew = false;

  @Inject
  public ProfileButtonPanelHandler(ProfileManager profileManager,
                                   TemplateManager templateManager,
                                   EventBus eventBus,
                                   @Named("configSelectionContext") ConfigSelectionContext context,
                                   @Named("profileConfigPanel") ProfilePanel profilePanel,
                                   @Named("multiSelectPanel") MultiSelectTaskPanel multiSelectPanel,
                                   @Named("profileButtonPanel") ButtonPanel buttonPanel,
                                   @Named("profileConfigCase") JXTableCase profileCase,
                                   @Named("taskConfigCase") JXTableCase taskCase,
                                   @Named("connectionConfigCase") JXTableCase connectionCase,
                                   @Named("queryConfigCase") JXTableCase queryCase,
                                   @Named("configTab") ConfigTab configTab,
                                   @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.profileManager = profileManager;
    this.templateManager = templateManager;
    this.eventBus = eventBus;
    this.context = context;
    this.profilePanel = profilePanel;
    this.multiSelectPanel = multiSelectPanel;
    this.buttonPanel = buttonPanel;
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.configTab = configTab;
    this.checkboxConfig = checkboxConfig;

    ButtonPanelBindings.bind(buttonPanel, this);
  }

  @Override
  public void onNew() {
    isNew = true;
    profilePanel.getJTextFieldProfile().setText("");
    profilePanel.getJTextFieldDescription().setText("");
    multiSelectPanel.getSelectedTaskCase().clearTable();
    setEditMode(true);
  }

  @Override
  public void onEdit() {
    isNew = false;
    setEditMode(true);
  }

  @Override
  public void onCopy() {
    Integer currentId = context.getSelectedProfileId();
    if (currentId == null) {
      return;
    }

    ProfileInfo currentProfile = profileManager.getProfileInfoById(currentId);
    if (currentProfile == null) {
      return;
    }

    isNew = true;

    profilePanel.getJTextFieldProfile().setText(currentProfile.getName() + "_copy");

    if (currentProfile.getDescription() != null) {
      profilePanel.getJTextFieldDescription().setText(currentProfile.getDescription() + "_copy");
    }

    setEditMode(true);
  }

  @Override
  public void onDelete() {
    Integer id = context.getSelectedProfileId();
    if (id == null) return;
    ProfileInfo info = profileManager.getProfileInfoById(id);
    if (info.getStatus() == RunStatus.RUNNING) {
      JOptionPane.showMessageDialog(null, "Profile is running!", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    if (JOptionPane.showConfirmDialog(null, "Delete " + info.getName() + "?") == JOptionPane.YES_OPTION) {
      profileManager.deleteProfile(id, info.getName());
      eventBus.publish(new ProfileRemoveEvent(id));
      selectFirstOrClear();
    }
  }

  @Override
  public void onSave() {
    String name = profilePanel.getJTextFieldProfile().getText().trim();
    if (name.isEmpty()) return;

    if (isNew) {
      boolean nameExists = profileManager.getProfileInfoList().stream()
          .anyMatch(p -> p.getName().equals(name));
      if (nameExists) {
        throw new NotFoundException(
            String.format("Name %s already exists, please enter another one.", name));
      }
    }

    ProfileInfo info = new ProfileInfo();

    int id = isNew
        ? profileManager.getProfileInfoList().stream()
        .map(ProfileInfo::getId)
        .max(Comparator.naturalOrder())
        .orElse(0) + 1
        : context.getSelectedProfileId();

    info.setId(id);
    info.setName(name);
    info.setDescription(profilePanel.getJTextFieldDescription().getText());
    info.setTaskInfoList(processSelectedTasks());

    if (isNew) profileManager.addProfile(info);
    else profileManager.updateProfile(info);

    context.setSelectedProfileId(info.getId());

    setEditMode(false);
    eventBus.publish(new ProfileAddEvent());

    restoreProfileSelection(info.getId());
  }

  @Override
  public void onCancel() {
    Integer currentId = context.getSelectedProfileId();
    setEditMode(false);
    if (currentId != null) {
      restoreProfileSelection(currentId);
    } else {
      selectFirstOrClear();
    }
  }

  private void restoreProfileSelection(int targetId) {
    JXTable table = profileCase.getJxTable();
    if (table == null) return;

    SwingUtilities.invokeLater(() -> {
      TTTable<ProfileRow, JXTable> tt = profileCase.getTypedTable();
      List<ProfileRow> rows = tt.model().items();
      for (int i = 0; i < rows.size(); i++) {
        if (Objects.equals(rows.get(i).getId(), targetId)) {
          table.setRowSelectionInterval(i, i);
          return;
        }
      }
      selectFirstOrClearInternal(table);
    });
  }

  private void selectFirstOrClear() {
    JXTable table = profileCase.getJxTable();
    if (table == null) return;
    SwingUtilities.invokeLater(() -> selectFirstOrClearInternal(table));
  }

  private void selectFirstOrClearInternal(JXTable table) {
    if (table.getRowCount() > 0) {
      table.setRowSelectionInterval(0, 0);
    } else {
      table.clearSelection();
      context.setSelectedProfileId(null);
      profilePanel.getJTextFieldProfile().setText("");
      profilePanel.getJTextFieldDescription().setText("");
      multiSelectPanel.getSelectedTaskCase().clearTable();
      multiSelectPanel.getTaskListCase().clearTable();
      ButtonPanelBindings.setViewMode(buttonPanel, false);
    }
  }

  private void setEditMode(boolean edit) {
    if (edit) ButtonPanelBindings.setEditMode(buttonPanel);
    else ButtonPanelBindings.setViewMode(buttonPanel, context.getSelectedProfileId() != null);

    profilePanel.getJTextFieldProfile().setEditable(edit);
    profilePanel.getJTextFieldDescription().setEditable(edit);
    multiSelectPanel.getPickBtn().setEnabled(edit);
    multiSelectPanel.getUnPickBtn().setEnabled(edit);

    boolean enable = !edit;
    profileCase.getJxTable().setEnabled(enable);
    taskCase.getJxTable().setEnabled(enable);
    connectionCase.getJxTable().setEnabled(enable);
    queryCase.getJxTable().setEnabled(enable);
    checkboxConfig.setEnabled(enable);

    configTab.setEnabledAt(0, true);
    configTab.setEnabledAt(1, enable);
    configTab.setEnabledAt(2, enable);
    configTab.setEnabledAt(3, enable);
  }

  private List<Integer> processSelectedTasks() {
    TTTable<TaskRow, JXTable> tt = multiSelectPanel.getSelectedTaskCase().getTypedTable();
    List<Integer> ids = new ArrayList<>();
    for (TaskRow row : tt.model().items()) {
      TaskInfo exist = profileManager.getTaskInfoList().stream().filter(t -> t.getName().equals(row.getName())).findFirst().orElse(null);
      if (exist != null) {
        ids.add(exist.getId());
      } else {
        ids.add(deepCopyFromTemplate(row.getId()));
      }
    }
    return ids;
  }

  private int deepCopyFromTemplate(int templateTaskId) {
    Task t = templateManager.getConfigList(Task.class).stream().filter(x -> x.getId() == templateTaskId).findFirst().orElseThrow();

    Connection conn = templateManager.getConfigList(Connection.class).stream().filter(c -> c.getId() == t.getConnectionId()).findFirst().orElseThrow();
    ConnectionInfo ci = new ConnectionInfo();
    ci.setId(profileManager.getConnectionInfoList().stream().map(ConnectionInfo::getId).max(Comparator.naturalOrder()).orElse(0) + 1);
    ci.setName(conn.getName());
    ci.setUserName(conn.getUserName());
    ci.setPassword(conn.getPassword());
    ci.setUrl(conn.getUrl());
    ci.setType(conn.getType());
    profileManager.addConnection(ci);

    List<Integer> qIds = new ArrayList<>();
    for (Integer qId : t.getQueryList()) {
      Query q = templateManager.getConfigList(Query.class).stream().filter(x -> x.getId() == qId).findFirst().orElseThrow();
      QueryInfo qi = new QueryInfo();
      qi.setId(profileManager.getQueryInfoList().stream().map(QueryInfo::getId).max(Comparator.naturalOrder()).orElse(0) + 1);
      qi.setName(q.getName());
      qi.setText(q.getText());
      profileManager.addQuery(qi);
      TableInfo tableInfo = new TableInfo();
      tableInfo.setTableName(q.getName());
      profileManager.addTable(tableInfo);
      qIds.add(qi.getId());
    }

    TaskInfo ti = new TaskInfo().setName(t.getName()).setDescription(t.getDescription()).setPullTimeout(t.getPullTimeout()).setConnectionId(ci.getId()).setQueryInfoList(qIds);
    ti.setId(profileManager.getTaskInfoList().stream().map(TaskInfo::getId).max(Comparator.naturalOrder()).orElse(0) + 1);
    profileManager.addTask(ti);
    return ti.getId();
  }
}