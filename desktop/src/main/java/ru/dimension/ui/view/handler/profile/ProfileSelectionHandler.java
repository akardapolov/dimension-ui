package ru.dimension.ui.view.handler.profile;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.view.panel.config.profile.ProfilePanel;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.column.ProfileColumnNames;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.tab.ConfigEditTabPane;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.handler.MouseListenerImpl;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.tab.ConfigTab;

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
    this.multiSelectTaskPanel.getTaskListCase().getJxTable().getColumnExt(0).setVisible(false);
    this.multiSelectTaskPanel.getSelectedTaskCase().getJxTable().getColumnExt(0).setVisible(false);
    this.multiSelectTaskPanel.getTemplateListTaskCase().getJxTable().getColumnExt(0).setVisible(false);

    this.checkboxConfig = checkboxConfig;
    this.checkboxConfig.addItemListener(this);
    this.isSelected = false;
    this.profileId = -1;

    this.bundleDefault = Internationalization.getInternationalizationBundle();

    this.configTab.addChangeListener(this);
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

        templateManager.getConfigList(Task.class)
            .forEach(taskIn -> multiSelectTaskPanel.getTemplateListTaskCase().getDefaultTableModel()
                .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));
        profileManager.getTaskInfoList()
            .forEach(taskIn -> multiSelectTaskPanel.getTaskListCase().getDefaultTableModel()
                .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));

      } else {

        profileId = GUIHelper.getIdByColumnName(profileCase.getJxTable(),
                                                profileCase.getDefaultTableModel(),
                                                listSelectionModel, ProfileColumnNames.ID.getColName());

        ProfileInfo profile = profileManager.getProfileInfoById(profileId);

        if (Objects.isNull(profile)) {
          throw new NotFoundException("Not found profile: " + profileId);
        }

        profilePanel.getJTextFieldProfile().setText(profile.getName());
        profilePanel.getJTextFieldDescription().setText(profile.getDescription());

        List<Integer> taskListByProfile = profile.getTaskInfoList();

        clearMultiSelectionPanel();
        templateManager.getConfigList(Task.class)
            .forEach(taskIn -> multiSelectTaskPanel.getTemplateListTaskCase().getDefaultTableModel()
                .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));

        profileManager.getTaskInfoList().stream()
            .filter(f -> !taskListByProfile.contains(f.getId()))
            .forEach(taskIn -> multiSelectTaskPanel.getTaskListCase().getDefaultTableModel()
                .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));

        taskListByProfile.forEach(taskId -> {
          TaskInfo taskIn = profileManager.getTaskInfoById(taskId);
          if (Objects.isNull(taskIn)) {
            throw new NotFoundException("Not found task: " + taskId);
          }
          multiSelectTaskPanel.getSelectedTaskCase().getDefaultTableModel()
              .addRow(new Object[]{taskIn.getId(), taskIn.getName()});
        });

        fillTaskCheckboxIsSelected(isSelected);

        configTab.setSelectedTab(ConfigEditTabPane.PROFILE);
        GUIHelper.disableButton(profileButtonPanel, !isSelected);
      }
    }
  }

  private void clearMultiSelectionPanel() {
    multiSelectTaskPanel.getSelectedTaskCase().getDefaultTableModel().getDataVector().removeAllElements();
    multiSelectTaskPanel.getSelectedTaskCase().getDefaultTableModel().fireTableDataChanged();
    multiSelectTaskPanel.getTaskListCase().getDefaultTableModel().getDataVector().removeAllElements();
    multiSelectTaskPanel.getTaskListCase().getDefaultTableModel().fireTableDataChanged();
    multiSelectTaskPanel.getTemplateListTaskCase().getDefaultTableModel().getDataVector().removeAllElements();
    multiSelectTaskPanel.getTemplateListTaskCase().getDefaultTableModel().fireTableDataChanged();
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
    taskCase.getDefaultTableModel().getDataVector().removeAllElements();
    taskCase.getDefaultTableModel().fireTableDataChanged();

    if (isSelected) {
      if (profileId >= 0) {
        profileManager.getProfileInfoById(profileId).getTaskInfoList()
            .forEach(taskId -> {
              TaskInfo taskIn = profileManager.getTaskInfoById(taskId);
              if (Objects.isNull(taskIn)) {
                throw new NotFoundException("Not found profile: " + taskId);
              }
              taskCase.getDefaultTableModel().addRow(new Object[]{taskIn.getId(), taskIn.getName()});
            });

        if (taskCase.getDefaultTableModel().getRowCount() > 0) {
          taskCase.getJxTable().setRowSelectionInterval(0, 0);
        } else {
          connectionCase.getDefaultTableModel().getDataVector().removeAllElements();
          connectionCase.getDefaultTableModel().fireTableDataChanged();
          queryCase.getDefaultTableModel().getDataVector().removeAllElements();
          queryCase.getDefaultTableModel().fireTableDataChanged();
        }
      } else {
        JOptionPane.showMessageDialog(null, "Profile is not selected", "General Error",
                                      JOptionPane.ERROR_MESSAGE);
      }
    } else {
      profileManager.getTaskInfoList()
          .forEach(e -> taskCase.getDefaultTableModel().addRow(new Object[]{e.getId(), e.getName()}));

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
          List<Task> taskListTemplate = templateManager.getConfigList(Task.class);
          taskListTemplate.forEach(taskIn -> multiSelectTaskPanel.getTemplateListTaskCase().getDefaultTableModel()
              .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));
          profileManager.getTaskInfoList()
              .forEach(taskIn -> multiSelectTaskPanel.getTaskListCase().getDefaultTableModel()
                  .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));
        } else {

          ProfileInfo profile = profileManager.getProfileInfoById(profileId);

          if (Objects.isNull(profile)) {
            throw new NotFoundException("Not found profile: " + profileId);
          }

          profilePanel.getJTextFieldProfile().setText(profile.getName());
          profilePanel.getJTextFieldDescription().setText(profile.getDescription());

          List<Integer> taskListByProfile = profile.getTaskInfoList();

          clearMultiSelectionPanel();
          templateManager.getConfigList(Task.class)
              .forEach(taskIn -> multiSelectTaskPanel.getTemplateListTaskCase().getDefaultTableModel()
                  .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));

          profileManager.getTaskInfoList().stream()
              .filter(f -> !taskListByProfile.contains(f.getId()))
              .forEach(taskIn -> multiSelectTaskPanel.getTaskListCase().getDefaultTableModel()
                  .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));

          taskListByProfile.forEach(taskId -> {
            TaskInfo taskIn = profileManager.getTaskInfoById(taskId);
            if (Objects.isNull(taskIn)) {
              throw new NotFoundException("Not found task: " + taskId);
            }
            multiSelectTaskPanel.getSelectedTaskCase().getDefaultTableModel()
                .addRow(new Object[]{taskIn.getId(), taskIn.getName()});
          });

          fillTaskCheckboxIsSelected(isSelected);

          configTab.setSelectedTab(ConfigEditTabPane.PROFILE);
          GUIHelper.disableButton(profileButtonPanel, !isSelected);
        }

      }
    }
  }
}




