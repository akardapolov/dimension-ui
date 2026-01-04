package ru.dimension.ui.view.handler.profile;

import static ru.dimension.ui.model.view.handler.LifeCycleStatus.COPY;
import static ru.dimension.ui.model.view.handler.LifeCycleStatus.EDIT;
import static ru.dimension.ui.model.view.handler.LifeCycleStatus.NEW;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.ProfileAddEvent;
import ru.dimension.ui.bus.event.ProfileRemoveEvent;
import ru.dimension.ui.exception.EmptyNameException;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.view.panel.config.profile.ProfilePanel;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.model.view.handler.LifeCycleStatus;
import ru.dimension.ui.model.RunStatus;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows.ConnectionRow;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Singleton
public class ProfileButtonPanelHandler implements ActionListener {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;
  private final EventBus eventBus;

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;

  private final ConfigTab configTab;
  private final ProfilePanel profilePanel;
  private final MultiSelectTaskPanel multiSelectTaskPanel;
  private final ButtonPanel profileButtonPanel;
  private final JCheckBox checkboxConfig;
  private LifeCycleStatus status;
  private final ResourceBundle bundleDefault;

  @Inject
  public ProfileButtonPanelHandler(@Named("profileManager") ProfileManager profileManager,
                                   @Named("templateManager") TemplateManager templateManager,
                                   @Named("eventBus") EventBus eventBus,
                                   @Named("profileConfigCase") JXTableCase profileCase,
                                   @Named("taskConfigCase") JXTableCase taskCase,
                                   @Named("connectionConfigCase") JXTableCase connectionCase,
                                   @Named("queryConfigCase") JXTableCase queryCase,
                                   @Named("jTabbedPaneConfig") ConfigTab configTab,
                                   @Named("profileConfigPanel") ProfilePanel profilePanel,
                                   @Named("multiSelectPanel") MultiSelectTaskPanel multiSelectTaskPanel,
                                   @Named("profileButtonPanel") ButtonPanel profileButtonPanel,
                                   @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.profileManager = profileManager;
    this.templateManager = templateManager;
    this.eventBus = eventBus;

    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;

    this.configTab = configTab;
    this.profilePanel = profilePanel;
    this.multiSelectTaskPanel = multiSelectTaskPanel;
    this.profileButtonPanel = profileButtonPanel;
    this.checkboxConfig = checkboxConfig;

    this.profileButtonPanel.getBtnNew().addActionListener(this);
    this.profileButtonPanel.getBtnCopy().addActionListener(this);
    this.profileButtonPanel.getBtnDel().addActionListener(this);
    this.profileButtonPanel.getBtnEdit().addActionListener(this);
    this.profileButtonPanel.getBtnSave().addActionListener(this);
    this.profileButtonPanel.getBtnCancel().addActionListener(this);

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.profileButtonPanel.getBtnDel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    this.profileButtonPanel.getBtnDel().getActionMap().put("delete", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        profileButtonPanel.getBtnDel().doClick();
      }
    });

    this.profileButtonPanel.getBtnCancel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
    this.profileButtonPanel.getBtnCancel().getActionMap().put("cancel", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        profileButtonPanel.getBtnCancel().doClick();
      }
    });

    this.status = LifeCycleStatus.NONE;

    checkProfilesExistAndToggleCheckbox();
  }

  private void checkProfilesExistAndToggleCheckbox() {
    if (profileManager.getProfileInfoList().isEmpty()) {
      checkboxConfig.setSelected(false);
      checkboxConfig.setEnabled(false);
    } else {
      checkboxConfig.setEnabled(true);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == profileButtonPanel.getBtnNew()) {
      status = NEW;

      newEmptyPanel();
      setPanelView(false);
      clearMultiSelectPanels();

      TTTable<TaskRow, JXTable> ttTaskList = multiSelectTaskPanel.getTaskListCase().getTypedTable();
      TTTable<TaskRow, JXTable> ttTemplate = multiSelectTaskPanel.getTemplateListTaskCase().getTypedTable();

      List<TaskRow> taskRows = profileManager.getTaskInfoList().stream()
          .map(t -> new TaskRow(t.getId(), t.getName()))
          .collect(Collectors.toList());
      ttTaskList.setItems(taskRows);

      List<TaskRow> templateRows = templateManager.getConfigList(Task.class).stream()
          .map(t -> new TaskRow(t.getId(), t.getName()))
          .collect(Collectors.toList());
      ttTemplate.setItems(templateRows);

    } else if (e.getSource() == profileButtonPanel.getBtnCopy()) {
      status = COPY;

      if (profileCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("The profile to copy is not selected. Please select and try again!");
      } else {
        int profileId = getSelectedProfileId();
        ProfileInfo profile = profileManager.getProfileInfoById(profileId);
        if (Objects.isNull(profile)) {
          throw new NotFoundException("Not found profile: " + profileId);
        }
        setPanelView(false);

        profilePanel.getJTextFieldProfile().setText(profile.getName() + "_copy");
        profilePanel.getJTextFieldDescription().setText(profile.getDescription() + "_copy");
      }

    } else if (e.getSource() == profileButtonPanel.getBtnDel()) {

      if (profileCase.getJxTable().getSelectedRow() == -1) {
        JOptionPane.showMessageDialog(null, "Not selected profile. Please select and try again!",
                                      "General Error", JOptionPane.ERROR_MESSAGE);
      } else {
        int profileId = getSelectedProfileId();
        ProfileInfo profile = profileManager.getProfileInfoById(profileId);
        if (Objects.isNull(profile)) {
          throw new NotFoundException("Not found profile: " + profileId);
        }

        if (isProfileRunning(profile)) {
          JOptionPane.showMessageDialog(null,
                                        "Cannot delete profile '" + profile.getName() + "' while it is running. Please stop the profile first.",
                                        "Deletion Not Allowed", JOptionPane.WARNING_MESSAGE);
          return;
        }

        int input = JOptionPane.showConfirmDialog(new JDialog(),
                                                  "Do you want to delete configuration: "
                                                      + profile.getName() + "?");
        if (input == 0) {
          profileManager.deleteProfile(profile.getId(), profile.getName());

          clearProfileCase();
          refillProfileTable();

          if (profileCase.getJxTable().getRowCount() > 0) {
            profileCase.getJxTable().setRowSelectionInterval(0, 0);
          } else {
            checkboxConfig.setSelected(false);
            checkboxConfig.setEnabled(false);
          }

          eventBus.publish(new ProfileRemoveEvent(profileId));
        }
      }
    } else if (e.getSource() == profileButtonPanel.getBtnEdit()) {
      if (profileCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("Not selected task. Please select and try again!");
      }
      status = EDIT;
      setPanelView(false);

    } else if (e.getSource() == profileButtonPanel.getBtnSave()) {

      if (NEW.equals(status) || COPY.equals(status)) {

        AtomicInteger profileIdNext = new AtomicInteger();

        profileManager.getProfileInfoList().stream()
            .max(Comparator.comparing(ProfileInfo::getId))
            .ifPresentOrElse(profile -> profileIdNext.set(profile.getId()),
                             () -> {
                               log.info("Not found Profiles");
                               profileIdNext.set(0);
                             });

        if (!profilePanel.getJTextFieldProfile().getText().trim().isEmpty()) {
          int profileId = profileIdNext.incrementAndGet();
          String newProfileName = profilePanel.getJTextFieldProfile().getText();
          checkProfileNameIsBusy(profileId, newProfileName);

          ProfileInfo saveProfile = getProfileInfo(profileId);

          profileManager.addProfile(saveProfile);

          clearProfileCase();

          List<ProfileInfo> allProfiles = profileManager.getProfileInfoList();
          List<ProfileRow> rows = allProfiles.stream()
              .map(p -> new ProfileRow(p.getId(), p.getName()))
              .collect(Collectors.toList());

          TTTable<ProfileRow, JXTable> tt = profileCase.getTypedTable();
          tt.setItems(rows);

          int selection = 0;
          for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).getId() == saveProfile.getId()) {
              selection = i;
              break;
            }
          }

          setPanelView(true);
          profileCase.getJxTable().setRowSelectionInterval(selection, selection);
          multiSelectTaskPanel.getJTabbedPaneTask().setSelectedIndex(0);

          checkboxConfig.setEnabled(true);
          eventBus.publish(new ProfileAddEvent());

        } else {
          throw new EmptyNameException("The name field is empty");
        }
      } else if (EDIT.equals(status)) {
        int selectedIndex = profileCase.getJxTable().getSelectedRow();
        int profileId = getSelectedProfileId();

        if (!profilePanel.getJTextFieldProfile().getText().trim().isEmpty()) {
          String newProfileName = profilePanel.getJTextFieldProfile().getText();
          checkProfileNameIsBusy(profileId, newProfileName);

          ProfileInfo oldProfile = profileManager.getProfileInfoById(profileId);

          ProfileInfo editProfile = getProfileInfo(profileId);

          if (!oldProfile.getName().equals(newProfileName)) {
            deleteProfileById(profileId);
            profileManager.addProfile(editProfile);
          } else {
            profileManager.updateProfile(editProfile);
          }

          clearProfileCase();
          refillProfileTable();

          setPanelView(true);
          profileCase.getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);
          multiSelectTaskPanel.getJTabbedPaneTask().setSelectedIndex(0);
        } else {
          throw new EmptyNameException("The name field is empty");
        }
      }

    } else if (e.getSource() == profileButtonPanel.getBtnCancel()) {
      if (profileCase.getJxTable().getSelectedRowCount() > 0) {
        int profileId = getSelectedProfileId();
        ProfileInfo profile = profileManager.getProfileInfoById(profileId);
        if (Objects.isNull(profile)) {
          throw new NotFoundException("Not found profile: " + profileId);
        }
        profilePanel.getJTextFieldProfile().setText(profile.getName());
        profilePanel.getJTextFieldDescription().setText(profile.getDescription());
        clearMultiSelectPanels();

        int selectedId = getSelectedProfileId();

        List<Integer> taskListAll = profileManager.getProfileInfoList()
            .stream()
            .filter(f -> f.getId() == selectedId)
            .findAny()
            .orElseThrow(() -> new NotFoundException("Not found profile: " + selectedId))
            .getTaskInfoList();

        List<TaskInfo> taskList = profileManager.getTaskInfoList();

        TTTable<TaskRow, JXTable> ttTaskList = multiSelectTaskPanel.getTaskListCase().getTypedTable();
        TTTable<TaskRow, JXTable> ttSelected = multiSelectTaskPanel.getSelectedTaskCase().getTypedTable();

        List<TaskRow> unselectedRows = taskList.stream()
            .filter(f -> !taskListAll.contains(f.getId()))
            .map(t -> new TaskRow(t.getId(), t.getName()))
            .collect(Collectors.toList());
        ttTaskList.setItems(unselectedRows);

        List<TaskRow> selectedRows = taskListAll.stream()
            .flatMap(taskId -> taskList.stream()
                .filter(t -> t.getId() == taskId)
                .map(t -> new TaskRow(t.getId(), t.getName())))
            .collect(Collectors.toList());
        ttSelected.setItems(selectedRows);

        setPanelView(true);
        multiSelectTaskPanel.getJTabbedPaneTask().setSelectedIndex(0);
      } else {
        newEmptyPanel();
        setPanelView(true);
        clearMultiSelectPanels();

        TTTable<TaskRow, JXTable> ttTaskList = multiSelectTaskPanel.getTaskListCase().getTypedTable();
        List<TaskRow> taskRows = profileManager.getTaskInfoList().stream()
            .map(t -> new TaskRow(t.getId(), t.getName()))
            .collect(Collectors.toList());
        ttTaskList.setItems(taskRows);
      }
    }
  }

  private boolean isProfileRunning(ProfileInfo profile) {
    return profile.getStatus() == RunStatus.RUNNING;
  }

  private void newEmptyPanel() {
    profilePanel.getJTextFieldProfile().setText("");
    profilePanel.getJTextFieldProfile().setPrompt(bundleDefault.getString("pName"));
    profilePanel.getJTextFieldDescription().setText("");
    profilePanel.getJTextFieldDescription().setPrompt(bundleDefault.getString("pDesc"));
  }

  public void checkProfileNameIsBusy(int id, String newProfileName) {
    List<ProfileInfo> profileList = profileManager.getProfileInfoList();
    for (ProfileInfo profile : profileList) {
      if (profile.getName().equals(newProfileName) && profile.getId() != id) {
        throw new NotFoundException("Name " + newProfileName
                                        + " already exists, please enter another one.");
      }
    }
  }

  public void deleteProfileById(int id) {
    ProfileInfo profileDel = profileManager.getProfileInfoById(id);
    if (Objects.isNull(profileDel)) {
      throw new NotFoundException("Not found profile by id: " + id);
    }
    profileManager.deleteProfile(profileDel.getId(), profileDel.getName());
  }

  private int getSelectedProfileId() {
    int selectedRow = profileCase.getJxTable().getSelectedRow();
    TTTable<ProfileRow, JXTable> tt = profileCase.getTypedTable();
    ProfileRow row = tt.model().itemAt(selectedRow);
    return row.getId();
  }

  private void clearMultiSelectPanels() {
    multiSelectTaskPanel.getSelectedTaskCase().clearTable();
    multiSelectTaskPanel.getTaskListCase().clearTable();
    multiSelectTaskPanel.getTemplateListTaskCase().clearTable();
  }

  private void clearProfileCase() {
    profileCase.clearTable();
  }

  private void refillProfileTable() {
    TTTable<ProfileRow, JXTable> tt = profileCase.getTypedTable();
    List<ProfileRow> rows = profileManager.getProfileInfoList().stream()
        .map(p -> new ProfileRow(p.getId(), p.getName()))
        .collect(Collectors.toList());
    tt.setItems(rows);
  }

  private void refillConnectionTable() {
    TTTable<ConnectionRow, JXTable> tt = connectionCase.getTypedTable();
    List<ConnectionRow> rows = profileManager.getConnectionInfoList().stream()
        .map(c -> {
          ConnectionInfo connectionInfo = profileManager.getConnectionInfoById(c.getId());
          return new ConnectionRow(c.getId(), c.getName(), c.getType(), connectionInfo.getDbType());
        })
        .collect(Collectors.toList());
    tt.setItems(rows);
  }

  private void refillQueryTable() {
    TTTable<QueryRow, JXTable> tt = queryCase.getTypedTable();
    List<QueryRow> rows = profileManager.getQueryInfoList().stream()
        .map(q -> new QueryRow(q.getId(), q.getName()))
        .collect(Collectors.toList());
    tt.setItems(rows);
  }

  private ProfileInfo getProfileInfo(int profileId) {
    ProfileInfo profile = new ProfileInfo();
    profile.setId(profileId);
    profile.setName(profilePanel.getJTextFieldProfile().getText());
    profile.setDescription(profilePanel.getJTextFieldDescription().getText());

    TTTable<TaskRow, JXTable> ttSelected = multiSelectTaskPanel.getSelectedTaskCase().getTypedTable();

    if (ttSelected.model().getRowCount() > 0) {
      List<Integer> taskListId = new ArrayList<>();

      for (int i = 0; i < ttSelected.model().getRowCount(); i++) {
        TaskRow taskRow = ttSelected.model().itemAt(i);
        Integer selectedDataTaskId = taskRow.getId();
        String selectedTaskName = taskRow.getName();

        AtomicInteger taskIdNext = new AtomicInteger();

        profileManager.getTaskInfoList().stream()
            .max(Comparator.comparing(TaskInfo::getId))
            .ifPresentOrElse(task -> taskIdNext.set(task.getId()),
                             () -> {
                               log.info("Not found Task");
                               taskIdNext.set(0);
                             });
        int newIdTask = taskIdNext.incrementAndGet();
        int newIdConnection = 0;
        int newIdQuery = 0;

        if (!isExistTaskName(selectedTaskName)) {

          List<Task> taskList = templateManager.getConfigList(Task.class);
          Task saveTask = taskList.stream()
              .filter(s -> s.getId() == selectedDataTaskId)
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found task by id: " + selectedDataTaskId));
          TaskInfo taskInfo = new TaskInfo()
              .setName(saveTask.getName())
              .setDescription(saveTask.getDescription())
              .setPullTimeout(saveTask.getPullTimeout())
              .setConnectionId(saveTask.getConnectionId())
              .setQueryInfoList(saveTask.getQueryList())
              .setChartInfoList(saveTask.getQueryList());

          Connection connectionTask = templateManager.getConfigList(Connection.class)
              .stream()
              .filter(f -> f.getId() == saveTask.getConnectionId())
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found connection by id: " + saveTask.getConnectionId()));

          AtomicInteger connectionIdNext = new AtomicInteger();

          profileManager.getConnectionInfoList().stream()
              .max(Comparator.comparing(ConnectionInfo::getId))
              .ifPresentOrElse(connection -> connectionIdNext.set(connection.getId()),
                               () -> {
                                 log.info("Not found Connection");
                                 connectionIdNext.set(0);
                               });
          newIdConnection = connectionIdNext.incrementAndGet();

          if (existConnectionName(connectionTask.getName()) == -1) {

            List<Connection> connectionList = templateManager.getConfigList(Connection.class);

            Connection saveConnection = connectionList.stream()
                .filter(s -> s.getId() == saveTask.getConnectionId())
                .findAny()
                .orElseThrow(() -> new NotFoundException("Not found connection by id: "
                                                             + saveTask.getConnectionId()));

            ConnectionInfo connectionInfo = new ConnectionInfo();
            connectionInfo.setId(newIdConnection);
            connectionInfo.setName(saveConnection.getName());
            connectionInfo.setUserName(saveConnection.getUserName());
            connectionInfo.setPassword(saveConnection.getPassword());
            connectionInfo.setUrl(saveConnection.getUrl());
            connectionInfo.setJar(saveConnection.getJar());
            connectionInfo.setDriver(saveConnection.getDriver());

            connectionInfo.setType(saveConnection.getType());

            if (ConnectionType.HTTP.equals(saveConnection.getType())) {
              connectionInfo.setHttpMethod(saveConnection.getHttpMethod());
              connectionInfo.setParseType(saveConnection.getParseType());
            }

            profileManager.addConnection(connectionInfo);

            connectionCase.clearTable();
            refillConnectionTable();

            taskInfo.setConnectionId(newIdConnection);
          } else {
            taskInfo.setConnectionId(existConnectionName(connectionTask.getName()));
          }

          for (int j = 0; j < saveTask.getQueryList().size(); j++) {
            int queryId = saveTask.getQueryList().get(j);

            Query queryTask = templateManager.getConfigList(Query.class)
                .stream()
                .filter(f -> f.getId() == queryId)
                .findAny()
                .orElseThrow(() -> new NotFoundException("Not found query by id: " + queryId));

            AtomicInteger queryIdNext = new AtomicInteger();

            profileManager.getQueryInfoList().stream()
                .max(Comparator.comparing(QueryInfo::getId))
                .ifPresentOrElse(query -> queryIdNext.set(query.getId()),
                                 () -> log.info("Not found Query"));
            newIdQuery = queryIdNext.incrementAndGet();

            if (existQueryId(queryTask.getName()) == -1) {

              List<Query> queryList = templateManager.getConfigList(Query.class);
              Query saveQuery = queryList.stream()
                  .filter(s -> s.getId() == queryId)
                  .findAny()
                  .orElseThrow(() -> new NotFoundException("Not found profile by id: " + queryId));

              taskInfo.getQueryInfoList().set(j, newIdQuery);

              QueryInfo queryInfo = new QueryInfo();
              queryInfo.setId(newIdQuery);
              queryInfo.setName(saveQuery.getName());
              queryInfo.setText(saveQuery.getText());
              queryInfo.setDescription(saveQuery.getDescription());
              queryInfo.setGatherDataMode(saveQuery.getGatherDataMode());
              queryInfo.setMetricList(saveQuery.getMetricList());
              profileManager.addQuery(queryInfo);
              TableInfo tableInfo = new TableInfo();
              tableInfo.setTableName(saveQuery.getName());
              profileManager.addTable(tableInfo);

              queryCase.clearTable();
              refillQueryTable();

            } else {
              taskInfo.getQueryInfoList().set(j, existQueryId(queryTask.getName()));
            }
          }
          taskInfo.setId(newIdTask);
          profileManager.addTask(taskInfo);
        } else {
          TaskInfo task = profileManager.getTaskInfoList().stream()
              .filter(s -> s.getName().equals(selectedTaskName))
              .findAny()
              .orElseThrow(() -> new NotFoundException("Not found query by id: " + selectedTaskName));
          newIdTask = task.getId();
        }
        taskListId.add(newIdTask);
      }
      profile.setTaskInfoList(taskListId);
    } else {
      profile.setTaskInfoList(Collections.emptyList());
    }

    return profile;
  }

  private void setPanelView(Boolean isSelected) {
    profileButtonPanel.setButtonView(isSelected);
    profilePanel.getJTextFieldProfile().setEditable(!isSelected);
    profilePanel.getJTextFieldDescription().setEditable(!isSelected);
    multiSelectTaskPanel.getUnPickBtn().setEnabled(!isSelected);
    multiSelectTaskPanel.getPickBtn().setEnabled(!isSelected);
    multiSelectTaskPanel.getUnPickAllBtn().setEnabled(!isSelected);
    multiSelectTaskPanel.getPickAllBtn().setEnabled(!isSelected);
    configTab.setEnabledAt(1, isSelected);
    configTab.setEnabledAt(2, isSelected);
    configTab.setEnabledAt(3, isSelected);
    profileCase.getJxTable().setEnabled(isSelected);
    taskCase.getJxTable().setEnabled(isSelected);
    connectionCase.getJxTable().setEnabled(isSelected);
    queryCase.getJxTable().setEnabled(isSelected);
    checkboxConfig.setEnabled(isSelected);
  }

  private int existQueryId(String selectedName) {
    int queryId = -1;
    List<QueryInfo> queryList = profileManager.getQueryInfoList();

    for (QueryInfo query : queryList) {
      if (query.getName().equals(selectedName)) {
        queryId = query.getId();
        break;
      }
    }
    return queryId;
  }

  private int existConnectionName(String selectedName) {
    int connectionId = -1;
    List<ConnectionInfo> connectionList = profileManager.getConnectionInfoList();
    for (ConnectionInfo connection : connectionList) {
      if (connection.getName().equals(selectedName)) {
        connectionId = connection.getId();
        break;
      }
    }
    return connectionId;
  }

  private boolean isExistTaskName(String selectedName) {
    boolean isExist = false;
    List<TaskInfo> taskList = profileManager.getTaskInfoList();

    for (TaskInfo task : taskList) {
      if (task.getName().equals(selectedName)) {
        isExist = true;
        break;
      }
    }
    return isExist;
  }
}