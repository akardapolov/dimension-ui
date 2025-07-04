package ru.dimension.ui.view.handler.profile;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;

@Log4j2
@Singleton
public class MultiSelectTaskHandler implements ActionListener, ChangeListener {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;
  private final MultiSelectTaskPanel multiSelectPanel;
  private JXTableCase jxTableCase;
  private final JXTableCase jxTableCaseSelected;

  @Inject
  public MultiSelectTaskHandler(@Named("multiSelectPanel") MultiSelectTaskPanel multiSelectPanel,
                                @Named("templateManager") TemplateManager templateManager,
                                @Named("profileManager") ProfileManager profileManager) {
    this.multiSelectPanel = multiSelectPanel;
    this.multiSelectPanel.getPickBtn().addActionListener(this);
    this.multiSelectPanel.getUnPickAllBtn().addActionListener(this);
    this.multiSelectPanel.getUnPickBtn().addActionListener(this);
    this.multiSelectPanel.getPickAllBtn().addActionListener(this);
    this.jxTableCaseSelected = multiSelectPanel.getSelectedTaskCase();

    this.multiSelectPanel.getJTabbedPaneTask().addChangeListener(this);
    this.jxTableCase = multiSelectPanel.getTaskListCase();

    this.profileManager = profileManager;
    this.templateManager = templateManager;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == multiSelectPanel.getPickBtn()) {
      if (jxTableCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("Not selected row in table. Please select and try again!");
      }
      String selectedData = (String) jxTableCase.getDefaultTableModel()
          .getValueAt(jxTableCase.getJxTable().getSelectedRow(), 1);
      int selectedID = (Integer) jxTableCase.getDefaultTableModel()
          .getValueAt(jxTableCase.getJxTable().getSelectedRow(), 0);

      if (isExistName(selectedData)) {
        if (selectedData != null) {
          jxTableCaseSelected.getDefaultTableModel()
              .addRow(new Object[]{selectedID, selectedData});
          jxTableCaseSelected.getJxTable().setRowSelectionInterval(jxTableCaseSelected.getJxTable().getRowCount() - 1,
                                                                   jxTableCaseSelected.getJxTable().getRowCount() - 1);
          if (jxTableCase == multiSelectPanel.getTaskListCase()) {
            int index = jxTableCase.getJxTable().getSelectedRow();
            if (jxTableCase.getDefaultTableModel().getRowCount() > 0) {
              DefaultTableModel defaultTableModel = (DefaultTableModel) jxTableCase.getJxTable().getModel();
              defaultTableModel.removeRow(index);
              if (jxTableCase.getJxTable().getRowCount() > 0) {
                jxTableCase.getJxTable().setRowSelectionInterval(jxTableCase.getJxTable().getRowCount() - 1,
                                                                 jxTableCase.getJxTable().getRowCount() - 1);
              }
            }
          }
        }
      } else if (jxTableCase == multiSelectPanel.getTemplateListTaskCase()) {
        JOptionPane.showMessageDialog(null,
                                      selectedData + " query already exists, please choose  another one ",
                                      "Information", JOptionPane.INFORMATION_MESSAGE);
      }
    }

    if (e.getSource() == multiSelectPanel.getUnPickBtn()) {
      if (jxTableCaseSelected.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("Not selected row in table. Please select and try again!");
      }
      String selectedData = (String) jxTableCaseSelected.getDefaultTableModel()
          .getValueAt(jxTableCaseSelected.getJxTable().getSelectedRow(), 1);
      int selectedID = (Integer) jxTableCaseSelected.getDefaultTableModel()
          .getValueAt(jxTableCaseSelected.getJxTable().getSelectedRow(), 0);

      jxTableCase = tableToFill(selectedData, selectedID);

      if (selectedData != null) {
        if (jxTableCase == multiSelectPanel.getTaskListCase()) {
          jxTableCase.getDefaultTableModel().addRow(new Object[]{selectedID, selectedData});
          jxTableCase.getJxTable().setRowSelectionInterval(jxTableCase.getJxTable().getRowCount() - 1,
                                                           jxTableCase.getJxTable().getRowCount() - 1);
        }
        int index = jxTableCaseSelected.getJxTable().getSelectedRow();
        if (index > -1) {
          DefaultTableModel defaultTableModel = (DefaultTableModel) jxTableCaseSelected.getJxTable().getModel();
          defaultTableModel.removeRow(index);
          if (jxTableCaseSelected.getJxTable().getRowCount() > 0) {
            jxTableCaseSelected.getJxTable()
                .setRowSelectionInterval(jxTableCaseSelected.getJxTable().getRowCount() - 1,
                                         jxTableCaseSelected.getJxTable().getRowCount() - 1);
          }
        }
        if (jxTableCase == multiSelectPanel.getTaskListCase()) {
          multiSelectPanel.getJTabbedPaneTask().setSelectedIndex(0);
        } else {
          multiSelectPanel.getJTabbedPaneTask().setSelectedIndex(1);
        }
      }
    }

    if (e.getSource() == multiSelectPanel.getPickAllBtn()) {
      List<TaskInfo> taskListAll = Collections.emptyList();
      if (jxTableCase == multiSelectPanel.getTaskListCase()) {
        jxTableCase.getDefaultTableModel().getDataVector().removeAllElements();
        jxTableCase.getDefaultTableModel().fireTableDataChanged();
        taskListAll = profileManager.getTaskInfoList();

        for (TaskInfo task : taskListAll) {
          if (isExistName(task.getName())) {
            jxTableCaseSelected.getDefaultTableModel()
                .addRow(new Object[]{task.getId(), task.getName()});
          }
        }
      } else {
        List<Task> taskList = templateManager.getConfigList(Task.class);
        for (int i = 0; i < taskList.size(); i++) {
          TaskInfo taskInfo = new TaskInfo();
          taskInfo.setId(taskList.get(i).getId());
          taskInfo.setName(taskList.get(i).getName());

          if (isExistName(taskInfo.getName())) {
            jxTableCaseSelected.getDefaultTableModel()
                .addRow(new Object[]{taskInfo.getId(), taskInfo.getName()});
          } else if (jxTableCase == multiSelectPanel.getTemplateListTaskCase()) {
            JOptionPane.showMessageDialog(null,
                                          taskInfo.getName() + " query already exists, please choose  another one ",
                                          "Information", JOptionPane.INFORMATION_MESSAGE);
          }
        }

        multiSelectPanel.getJTabbedPaneTask().setSelectedIndex(0);
      }

    }

    if (e.getSource() == multiSelectPanel.getUnPickAllBtn()) {
      jxTableCaseSelected.getDefaultTableModel().getDataVector().removeAllElements();
      jxTableCaseSelected.getDefaultTableModel().fireTableDataChanged();
      multiSelectPanel.getTaskListCase().getDefaultTableModel().getDataVector().removeAllElements();
      multiSelectPanel.getTaskListCase().getDefaultTableModel().fireTableDataChanged();

      List<TaskInfo> taskListAll = profileManager.getTaskInfoList();
      for (TaskInfo task : taskListAll) {
        jxTableCase = tableToFill(task.getName(), task.getId());
        jxTableCase.getDefaultTableModel()
            .addRow(new Object[]{task.getId(), task.getName()});
      }
      multiSelectPanel.getJTabbedPaneTask().setSelectedIndex(0);
    }
  }

  private JXTableCase tableToFill(String selectedName,
                                  int selectedId) {
    JXTableCase jxTableCase = multiSelectPanel.getTaskListCase();
    DefaultTableModel defaultTableModel = multiSelectPanel.getTemplateListTaskCase().getDefaultTableModel();
    for (int rowIndex = 0; rowIndex < defaultTableModel.getRowCount(); rowIndex++) {
      if (selectedName.equals(defaultTableModel.getValueAt(rowIndex, 1).toString())
          && selectedId == (Integer) (defaultTableModel.getValueAt(rowIndex, 0))) {
        jxTableCase = multiSelectPanel.getTemplateListTaskCase();
      }
    }
    return jxTableCase;
  }


  private boolean isExistName(String selectedData) {
    boolean isExist = true;
    DefaultTableModel defaultTableModel = multiSelectPanel.getSelectedTaskCase().getDefaultTableModel();
    for (int rowIndex = 0; rowIndex < defaultTableModel.getRowCount(); rowIndex++) {
      if (selectedData.equals(defaultTableModel.getValueAt(rowIndex, 1).toString())) {
        isExist = false;
      }
    }
    return isExist;
  }

  @Override
  public void stateChanged(ChangeEvent changeEvent) {
    if (multiSelectPanel.getJTabbedPaneTask().getSelectedIndex() == 0) {
      this.jxTableCase = this.multiSelectPanel.getTaskListCase();
    } else {
      this.jxTableCase = this.multiSelectPanel.getTemplateListTaskCase();
    }
  }
}