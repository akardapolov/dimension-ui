package ru.dimension.ui.view.handler.profile;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.swing.JCheckBox;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.view.panel.config.profile.ProfilePanel;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.tab.ConfigEditTabPane;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.handler.MouseListenerImpl;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Singleton
public class ProfileSelectionHandler extends MouseListenerImpl
    implements ListSelectionListener, ItemListener, ChangeListener {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;
  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;

  private final ConfigTab configTab;
  private final ProfilePanel profilePanel;
  private final MultiSelectTaskPanel multiSelectTaskPanel;

  private final ButtonPanel profileButtonPanel;
  private final JCheckBox checkboxConfig;

  private Boolean isSelected;
  private int profileId;
  private final ResourceBundle bundleDefault;

  @Inject
  public ProfileSelectionHandler(@Named("profileManager") ProfileManager profileManager,
                                 @Named("templateManager") TemplateManager templateManager,
                                 @Named("jTabbedPaneConfig") ConfigTab configTab,
                                 @Named("profileConfigCase") JXTableCase profileCase,
                                 @Named("taskConfigCase") JXTableCase taskCase,
                                 @Named("connectionConfigCase") JXTableCase connectionCase,
                                 @Named("queryConfigCase") JXTableCase queryCase,
                                 @Named("profileConfigPanel") ProfilePanel profilePanel,
                                 @Named("multiSelectPanel") MultiSelectTaskPanel multiSelectTaskPanel,
                                 @Named("profileButtonPanel") ButtonPanel profileButtonPanel,
                                 @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.profileManager = profileManager;
    this.templateManager = templateManager;

    this.configTab = configTab;
    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;
    this.profileCase.getJxTable().getSelectionModel().addListSelectionListener(this);
    this.profileCase.getJxTable().addMouseListener(this);

    this.profilePanel = profilePanel;
    this.multiSelectTaskPanel = multiSelectTaskPanel;
    this.profileButtonPanel = profileButtonPanel;

    this.checkboxConfig = checkboxConfig;
    this.checkboxConfig.addItemListener(this);
    this.isSelected = false;
    this.profileId = -1;

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.configTab.addChangeListener(this);

    initializeTaskTables();
  }

  private void initializeTaskTables() {
    log.info("Initializing task tables...");

    clearMultiSelectionPanel();

    TTTable<TaskRow, JXTable> ttTemplate = multiSelectTaskPanel.getTemplateListTaskCase().getTypedTable();
    TTTable<TaskRow, JXTable> ttTaskList = multiSelectTaskPanel.getTaskListCase().getTypedTable();

    List<Task> templateTasks = templateManager.getConfigList(Task.class);
    log.info("Template tasks count: {}", templateTasks.size());

    List<TaskRow> templateRows = templateTasks.stream()
        .map(t -> new TaskRow(t.getId(), t.getName()))
        .collect(Collectors.toList());
    ttTemplate.setItems(templateRows);

    List<TaskInfo> configTasks = profileManager.getTaskInfoList();
    log.info("Config tasks count: {}", configTasks.size());

    List<TaskRow> taskRows = configTasks.stream()
        .map(t -> new TaskRow(t.getId(), t.getName()))
        .collect(Collectors.toList());
    ttTaskList.setItems(taskRows);

    log.info("Task tables initialized. TaskList rows: {}, Template rows: {}",
             ttTaskList.model().getRowCount(), ttTemplate.model().getRowCount());
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    ListSelectionModel listSelectionModel = (ListSelectionModel) e.getSource();

    if (configTab.isEnabledAt(0)) {
      configTab.setSelectedTab(ConfigEditTabPane.PROFILE);
    }

    if (!e.getValueIsAdjusting()) {
      if (listSelectionModel.isSelectionEmpty()) {
        log.info("Clearing profile fields");

        multiSelectTaskPanel.getJTabbedPaneTask().setSelectedIndex(0);
        profileId = -1;
        profilePanel.getJTextFieldProfile().setEditable(false);
        profilePanel.getJTextFieldDescription().setEditable(false);
        multiSelectTaskPanel.getUnPickBtn().setEnabled(false);
        multiSelectTaskPanel.getPickBtn().setEnabled(false);
        multiSelectTaskPanel.getUnPickAllBtn().setEnabled(false);
        multiSelectTaskPanel.getPickAllBtn().setEnabled(false);

        profilePanel.getJTextFieldProfile().setText("");
        profilePanel.getJTextFieldProfile().setPrompt(bundleDefault.getString("pName"));
        profilePanel.getJTextFieldDescription().setText("");
        profilePanel.getJTextFieldDescription().setPrompt(bundleDefault.getString("pDesc"));

        clearMultiSelectionPanel();

        TTTable<TaskRow, JXTable> ttTemplate = multiSelectTaskPanel.getTemplateListTaskCase().getTypedTable();
        TTTable<TaskRow, JXTable> ttTaskList = multiSelectTaskPanel.getTaskListCase().getTypedTable();

        List<TaskRow> templateRows = templateManager.getConfigList(Task.class).stream()
            .map(t -> new TaskRow(t.getId(), t.getName()))
            .collect(Collectors.toList());
        ttTemplate.setItems(templateRows);
        log.info("Loaded {} template tasks", templateRows.size());

        List<TaskRow> taskRows = profileManager.getTaskInfoList().stream()
            .map(t -> new TaskRow(t.getId(), t.getName()))
            .collect(Collectors.toList());
        ttTaskList.setItems(taskRows);
        log.info("Loaded {} config tasks", taskRows.size());

      } else {

        profileId = getSelectedProfileId();
        log.info("Selected profile ID: {}", profileId);

        ProfileInfo profile = profileManager.getProfileInfoById(profileId);

        if (Objects.isNull(profile)) {
          throw new NotFoundException("Not found profile: " + profileId);
        }

        profilePanel.getJTextFieldProfile().setText(profile.getName());
        profilePanel.getJTextFieldDescription().setText(profile.getDescription());

        List<Integer> taskListByProfile = profile.getTaskInfoList();
        log.info("Profile {} has {} tasks", profile.getName(), taskListByProfile.size());

        clearMultiSelectionPanel();

        TTTable<TaskRow, JXTable> ttTemplate = multiSelectTaskPanel.getTemplateListTaskCase().getTypedTable();
        TTTable<TaskRow, JXTable> ttTaskList = multiSelectTaskPanel.getTaskListCase().getTypedTable();
        TTTable<TaskRow, JXTable> ttSelected = multiSelectTaskPanel.getSelectedTaskCase().getTypedTable();

        List<TaskRow> templateRows = templateManager.getConfigList(Task.class).stream()
            .map(t -> new TaskRow(t.getId(), t.getName()))
            .collect(Collectors.toList());
        ttTemplate.setItems(templateRows);

        List<TaskRow> unselectedRows = profileManager.getTaskInfoList().stream()
            .filter(f -> !taskListByProfile.contains(f.getId()))
            .map(t -> new TaskRow(t.getId(), t.getName()))
            .collect(Collectors.toList());
        ttTaskList.setItems(unselectedRows);

        List<TaskRow> selectedRows = taskListByProfile.stream()
            .map(taskId -> {
              TaskInfo taskIn = profileManager.getTaskInfoById(taskId);
              if (Objects.isNull(taskIn)) {
                throw new NotFoundException("Not found task: " + taskId);
              }
              return new TaskRow(taskIn.getId(), taskIn.getName());
            })
            .collect(Collectors.toList());
        ttSelected.setItems(selectedRows);

        log.info("Loaded tables - Template: {}, TaskList: {}, Selected: {}",
                 templateRows.size(), unselectedRows.size(), selectedRows.size());

        fillTaskCheckboxIsSelected(isSelected);

        configTab.setSelectedTab(ConfigEditTabPane.PROFILE);
        GUIHelper.disableButton(profileButtonPanel, !isSelected);
      }
    }
  }

  private int getSelectedProfileId() {
    int selectedRow = profileCase.getJxTable().getSelectedRow();
    if (selectedRow < 0) {
      return -1;
    }
    TTTable<ProfileRow, JXTable> tt = profileCase.getTypedTable();
    ProfileRow row = tt.model().itemAt(selectedRow);
    return row != null ? row.getId() : -1;
  }

  private void clearMultiSelectionPanel() {
    multiSelectTaskPanel.getSelectedTaskCase().clearTable();
    multiSelectTaskPanel.getTaskListCase().clearTable();
    multiSelectTaskPanel.getTemplateListTaskCase().clearTable();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (configTab.isEnabledAt(0)) {
      configTab.setSelectedTab(ConfigEditTabPane.PROFILE);
    }
  }

  @Override
  public void itemStateChanged(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {
      GUIHelper.disableButton(profileButtonPanel, false);
      isSelected = true;
      fillTaskCheckboxIsSelected(true);
    } else {
      GUIHelper.disableButton(profileButtonPanel, true);
      isSelected = false;
      fillTaskCheckboxIsSelected(false);
    }

    configTab.setSelectedTab(ConfigEditTabPane.PROFILE);
  }

  public void fillTaskCheckboxIsSelected(Boolean isSelected) {
    taskCase.clearTable();

    if (isSelected) {
      if (profileId == -1) {
        if (profileCase.getJxTable().getRowCount() > 0) {
          profileCase.getJxTable().setRowSelectionInterval(0, 0);
        } else {
          checkboxConfig.setSelected(false);
          checkboxConfig.setEnabled(false);
        }
        return;
      }

      if (profileId >= 0) {
        TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();

        ProfileInfo profileInfo = profileManager.getProfileInfoById(profileId);
        if (profileInfo == null) {
          log.warn("Profile not found: {}", profileId);
          return;
        }

        List<TaskRow> rows = profileInfo.getTaskInfoList().stream()
            .map(taskId -> {
              TaskInfo taskIn = profileManager.getTaskInfoById(taskId);
              if (Objects.isNull(taskIn)) {
                throw new NotFoundException("Not found task: " + taskId);
              }
              return new TaskRow(taskIn.getId(), taskIn.getName());
            })
            .collect(Collectors.toList());

        tt.setItems(rows);

        if (taskCase.getJxTable().getRowCount() > 0) {
          taskCase.getJxTable().setRowSelectionInterval(0, 0);
        } else {
          connectionCase.clearTable();
          queryCase.clearTable();
        }
      }
    } else {
      TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();
      List<TaskRow> rows = profileManager.getTaskInfoList().stream()
          .map(t -> new TaskRow(t.getId(), t.getName()))
          .collect(Collectors.toList());
      tt.setItems(rows);
    }
  }

  @Override
  public void stateChanged(ChangeEvent changeEvent) {
    if (!isSelected) {
      if (configTab.getSelectedIndex() == 0) {

        if (profileId == -1) {
          multiSelectTaskPanel.getJTabbedPaneTask().setSelectedIndex(0);

          profilePanel.getJTextFieldProfile().setEditable(false);
          profilePanel.getJTextFieldDescription().setEditable(false);
          multiSelectTaskPanel.getUnPickBtn().setEnabled(false);
          multiSelectTaskPanel.getPickBtn().setEnabled(false);
          multiSelectTaskPanel.getUnPickAllBtn().setEnabled(false);
          multiSelectTaskPanel.getPickAllBtn().setEnabled(false);

          profilePanel.getJTextFieldProfile().setText("");
          profilePanel.getJTextFieldProfile().setPrompt(bundleDefault.getString("pName"));
          profilePanel.getJTextFieldDescription().setText("");
          profilePanel.getJTextFieldDescription().setPrompt(bundleDefault.getString("pDesc"));

          clearMultiSelectionPanel();

          TTTable<TaskRow, JXTable> ttTemplate = multiSelectTaskPanel.getTemplateListTaskCase().getTypedTable();
          TTTable<TaskRow, JXTable> ttTaskList = multiSelectTaskPanel.getTaskListCase().getTypedTable();

          List<TaskRow> templateRows = templateManager.getConfigList(Task.class).stream()
              .map(t -> new TaskRow(t.getId(), t.getName()))
              .collect(Collectors.toList());
          ttTemplate.setItems(templateRows);

          List<TaskRow> taskRows = profileManager.getTaskInfoList().stream()
              .map(t -> new TaskRow(t.getId(), t.getName()))
              .collect(Collectors.toList());
          ttTaskList.setItems(taskRows);

        } else {

          ProfileInfo profile = profileManager.getProfileInfoById(profileId);

          if (Objects.isNull(profile)) {
            throw new NotFoundException("Not found profile: " + profileId);
          }

          profilePanel.getJTextFieldProfile().setText(profile.getName());
          profilePanel.getJTextFieldDescription().setText(profile.getDescription());

          List<Integer> taskListByProfile = profile.getTaskInfoList();

          clearMultiSelectionPanel();

          TTTable<TaskRow, JXTable> ttTemplate = multiSelectTaskPanel.getTemplateListTaskCase().getTypedTable();
          TTTable<TaskRow, JXTable> ttTaskList = multiSelectTaskPanel.getTaskListCase().getTypedTable();
          TTTable<TaskRow, JXTable> ttSelected = multiSelectTaskPanel.getSelectedTaskCase().getTypedTable();

          List<TaskRow> templateRows = templateManager.getConfigList(Task.class).stream()
              .map(t -> new TaskRow(t.getId(), t.getName()))
              .collect(Collectors.toList());
          ttTemplate.setItems(templateRows);

          List<TaskRow> unselectedRows = profileManager.getTaskInfoList().stream()
              .filter(f -> !taskListByProfile.contains(f.getId()))
              .map(t -> new TaskRow(t.getId(), t.getName()))
              .collect(Collectors.toList());
          ttTaskList.setItems(unselectedRows);

          List<TaskRow> selectedRows = taskListByProfile.stream()
              .map(taskId -> {
                TaskInfo taskIn = profileManager.getTaskInfoById(taskId);
                if (Objects.isNull(taskIn)) {
                  throw new NotFoundException("Not found task: " + taskId);
                }
                return new TaskRow(taskIn.getId(), taskIn.getName());
              })
              .collect(Collectors.toList());
          ttSelected.setItems(selectedRows);

          fillTaskCheckboxIsSelected(isSelected);

          configTab.setSelectedTab(ConfigEditTabPane.PROFILE);
          GUIHelper.disableButton(profileButtonPanel, !isSelected);
        }

      }
    }
  }
}