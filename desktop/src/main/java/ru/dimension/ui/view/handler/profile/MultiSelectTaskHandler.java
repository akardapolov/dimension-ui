package ru.dimension.ui.view.handler.profile;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Singleton
public class MultiSelectTaskHandler implements ActionListener, ChangeListener {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;
  private final MultiSelectTaskPanel multiSelectPanel;
  private JXTableCase taskListCase;
  private final JXTableCase selectedTaskCase;

  @Inject
  public MultiSelectTaskHandler(@Named("multiSelectPanel") MultiSelectTaskPanel multiSelectPanel,
                                @Named("templateManager") TemplateManager templateManager,
                                @Named("profileManager") ProfileManager profileManager) {
    this.multiSelectPanel = multiSelectPanel;
    this.multiSelectPanel.getPickBtn().addActionListener(this);
    this.multiSelectPanel.getUnPickAllBtn().addActionListener(this);
    this.multiSelectPanel.getUnPickBtn().addActionListener(this);
    this.multiSelectPanel.getPickAllBtn().addActionListener(this);
    this.multiSelectPanel.getJTabbedPaneTask().addChangeListener(this);

    this.taskListCase = multiSelectPanel.getTaskListCase();
    this.selectedTaskCase = multiSelectPanel.getSelectedTaskCase();

    this.profileManager = profileManager;
    this.templateManager = templateManager;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == multiSelectPanel.getPickBtn()) {
      handlePick();
    } else if (e.getSource() == multiSelectPanel.getUnPickBtn()) {
      handleUnPick();
    } else if (e.getSource() == multiSelectPanel.getPickAllBtn()) {
      handlePickAll();
    } else if (e.getSource() == multiSelectPanel.getUnPickAllBtn()) {
      handleUnPickAll();
    }
  }

  private void handlePick() {
    if (taskListCase.getJxTable().getSelectedRow() == -1) {
      throw new NotSelectedRowException("Not selected row in table. Please select and try again!");
    }

    TTTable<TaskRow, JXTable> ttTaskList = taskListCase.getTypedTable();
    TTTable<TaskRow, JXTable> ttSelected = selectedTaskCase.getTypedTable();

    int selectedRow = taskListCase.getJxTable().getSelectedRow();
    TaskRow selectedTaskRow = ttTaskList.model().itemAt(selectedRow);
    String selectedData = selectedTaskRow.getName();
    int selectedID = selectedTaskRow.getId();

    if (isExistName(selectedData)) {
      if (selectedData != null) {
        ttSelected.addItem(new TaskRow(selectedID, selectedData));
        selectedTaskCase.getJxTable().setRowSelectionInterval(
            selectedTaskCase.getJxTable().getRowCount() - 1,
            selectedTaskCase.getJxTable().getRowCount() - 1);

        if (taskListCase == multiSelectPanel.getTaskListCase()) {
          int index = taskListCase.getJxTable().getSelectedRow();
          if (ttTaskList.model().getRowCount() > 0) {
            removeRowAt(ttTaskList, index);
            if (taskListCase.getJxTable().getRowCount() > 0) {
              taskListCase.getJxTable().setRowSelectionInterval(
                  taskListCase.getJxTable().getRowCount() - 1,
                  taskListCase.getJxTable().getRowCount() - 1);
            }
          }
        }
      }
    } else if (taskListCase == multiSelectPanel.getTemplateListTaskCase()) {
      JOptionPane.showMessageDialog(null,
                                    selectedData + " query already exists, please choose another one",
                                    "Information", JOptionPane.INFORMATION_MESSAGE);
    }
  }

  private void handleUnPick() {
    if (selectedTaskCase.getJxTable().getSelectedRow() == -1) {
      throw new NotSelectedRowException("Not selected row in table. Please select and try again!");
    }

    TTTable<TaskRow, JXTable> ttSelected = selectedTaskCase.getTypedTable();

    int selectedRow = selectedTaskCase.getJxTable().getSelectedRow();
    TaskRow selectedTaskRow = ttSelected.model().itemAt(selectedRow);
    String selectedData = selectedTaskRow.getName();
    int selectedID = selectedTaskRow.getId();

    taskListCase = tableToFill(selectedData, selectedID);

    if (selectedData != null) {
      if (taskListCase == multiSelectPanel.getTaskListCase()) {
        TTTable<TaskRow, JXTable> ttTaskList = taskListCase.getTypedTable();
        ttTaskList.addItem(new TaskRow(selectedID, selectedData));
        taskListCase.getJxTable().setRowSelectionInterval(
            taskListCase.getJxTable().getRowCount() - 1,
            taskListCase.getJxTable().getRowCount() - 1);
      }

      int index = selectedTaskCase.getJxTable().getSelectedRow();
      if (index > -1) {
        removeRowAt(ttSelected, index);
        if (selectedTaskCase.getJxTable().getRowCount() > 0) {
          selectedTaskCase.getJxTable().setRowSelectionInterval(
              selectedTaskCase.getJxTable().getRowCount() - 1,
              selectedTaskCase.getJxTable().getRowCount() - 1);
        }
      }

      if (taskListCase == multiSelectPanel.getTaskListCase()) {
        multiSelectPanel.getJTabbedPaneTask().setSelectedIndex(0);
      } else {
        multiSelectPanel.getJTabbedPaneTask().setSelectedIndex(1);
      }
    }
  }

  private void handlePickAll() {
    TTTable<TaskRow, JXTable> ttSelected = selectedTaskCase.getTypedTable();

    if (taskListCase == multiSelectPanel.getTaskListCase()) {
      taskListCase.clearTable();

      List<TaskInfo> taskListAll = profileManager.getTaskInfoList();
      for (TaskInfo task : taskListAll) {
        if (isExistName(task.getName())) {
          ttSelected.addItem(new TaskRow(task.getId(), task.getName()));
        }
      }
    } else {
      List<Task> taskList = templateManager.getConfigList(Task.class);
      for (Task task : taskList) {
        if (isExistName(task.getName())) {
          ttSelected.addItem(new TaskRow(task.getId(), task.getName()));
        } else {
          JOptionPane.showMessageDialog(null,
                                        task.getName() + " query already exists, please choose another one",
                                        "Information", JOptionPane.INFORMATION_MESSAGE);
        }
      }
      multiSelectPanel.getJTabbedPaneTask().setSelectedIndex(0);
    }
  }

  private void handleUnPickAll() {
    selectedTaskCase.clearTable();
    multiSelectPanel.getTaskListCase().clearTable();

    TTTable<TaskRow, JXTable> ttTaskList = multiSelectPanel.getTaskListCase().getTypedTable();

    List<TaskInfo> taskListAll = profileManager.getTaskInfoList();
    for (TaskInfo task : taskListAll) {
      JXTableCase targetCase = tableToFill(task.getName(), task.getId());
      if (targetCase == multiSelectPanel.getTaskListCase()) {
        ttTaskList.addItem(new TaskRow(task.getId(), task.getName()));
      }
    }
    multiSelectPanel.getJTabbedPaneTask().setSelectedIndex(0);
  }

  private JXTableCase tableToFill(String selectedName, int selectedId) {
    JXTableCase result = multiSelectPanel.getTaskListCase();
    TTTable<TaskRow, JXTable> ttTemplate = multiSelectPanel.getTemplateListTaskCase().getTypedTable();

    for (int i = 0; i < ttTemplate.model().getRowCount(); i++) {
      TaskRow row = ttTemplate.model().itemAt(i);
      if (selectedName.equals(row.getName()) && selectedId == row.getId()) {
        result = multiSelectPanel.getTemplateListTaskCase();
        break;
      }
    }
    return result;
  }

  private boolean isExistName(String selectedData) {
    TTTable<TaskRow, JXTable> ttSelected = multiSelectPanel.getSelectedTaskCase().getTypedTable();

    for (int i = 0; i < ttSelected.model().getRowCount(); i++) {
      TaskRow row = ttSelected.model().itemAt(i);
      if (selectedData.equals(row.getName())) {
        return false;
      }
    }
    return true;
  }

  private void removeRowAt(TTTable<TaskRow, JXTable> tt, int index) {
    List<TaskRow> items = new ArrayList<>();
    for (int i = 0; i < tt.model().getRowCount(); i++) {
      if (i != index) {
        items.add(tt.model().itemAt(i));
      }
    }
    tt.setItems(items);
  }

  @Override
  public void stateChanged(ChangeEvent changeEvent) {
    if (multiSelectPanel.getJTabbedPaneTask().getSelectedIndex() == 0) {
      this.taskListCase = this.multiSelectPanel.getTaskListCase();
    } else {
      this.taskListCase = this.multiSelectPanel.getTemplateListTaskCase();
    }
  }
}