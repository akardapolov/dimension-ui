package ru.dimension.ui.view.handler.task;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
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
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.panel.config.task.MultiSelectQueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;

@Log4j2
@Singleton
public class MultiSelectQueryHandler implements ActionListener, ChangeListener {

  private final MultiSelectQueryPanel multiSelectQueryPanel;
  private JXTableCase jxTableCase;
  private final JXTableCase jxTableCaseSelected;
  private final TemplateManager templateManager;
  private final ProfileManager profileManager;
  private final TaskPanel taskPanel;

  @Inject
  public MultiSelectQueryHandler(@Named("multiSelectQueryPanel") MultiSelectQueryPanel multiSelectQueryPanel,
                                 @Named("templateManager") TemplateManager templateManager,
                                 @Named("taskConfigPanel") TaskPanel taskPanel,
                                 @Named("profileManager") ProfileManager profileManager) {
    this.multiSelectQueryPanel = multiSelectQueryPanel;
    this.multiSelectQueryPanel.getPickBtn().addActionListener(this);
    this.multiSelectQueryPanel.getPickAllBtn().addActionListener(this);
    this.multiSelectQueryPanel.getUnPickBtn().addActionListener(this);
    this.multiSelectQueryPanel.getUnPickAllBtn().addActionListener(this);
    this.jxTableCaseSelected = multiSelectQueryPanel.getSelectedQueryCase();
    this.taskPanel = taskPanel;

    this.multiSelectQueryPanel.getJTabbedPaneQuery().addChangeListener(this);
    this.jxTableCase = multiSelectQueryPanel.getQueryListCase();

    this.templateManager = templateManager;
    this.profileManager = profileManager;
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == multiSelectQueryPanel.getPickBtn()) {
      if (jxTableCase.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("Not selected row in table. Please select and try again!");
      }
      String selectedData = (String) jxTableCase.getDefaultTableModel()
          .getValueAt(jxTableCase.getJxTable().getSelectedRow(), 1);
      int selectedID = (Integer) jxTableCase.getDefaultTableModel()
          .getValueAt(jxTableCase.getJxTable().getSelectedRow(), 0);
      String selectedDescription = (String) jxTableCase.getDefaultTableModel()
          .getValueAt(jxTableCase.getJxTable().getSelectedRow(), 2);
      String selectedText = (String) jxTableCase.getDefaultTableModel()
          .getValueAt(jxTableCase.getJxTable().getSelectedRow(), 3);

      if (isExistName(selectedData)) {
        if (selectedData != null) {
          jxTableCaseSelected.getDefaultTableModel()
              .addRow(new Object[]{selectedID, selectedData, selectedDescription, selectedText});
          jxTableCaseSelected.getJxTable()
              .setRowSelectionInterval(jxTableCaseSelected.getJxTable().getRowCount() - 1,
                                       jxTableCaseSelected.getJxTable().getRowCount() - 1);
          if (jxTableCase == multiSelectQueryPanel.getQueryListCase()) {
            int index = jxTableCase.getJxTable().getSelectedRow();
            if (index > -1) {
              DefaultTableModel defaultTableModel = (DefaultTableModel) jxTableCase.getJxTable().getModel();
              defaultTableModel.removeRow(index);
              if (jxTableCase.getJxTable().getRowCount() > 0) {
                jxTableCase.getJxTable().setRowSelectionInterval(jxTableCase.getJxTable().getRowCount() - 1,
                                                                 jxTableCase.getJxTable().getRowCount() - 1);
              }
            }
          }
        }
      } else if (jxTableCase == multiSelectQueryPanel.getTemplateListQueryCase()) {
        JOptionPane.showMessageDialog(null,
                                      selectedData + " query already exists, please choose  another one ",
                                      "Information", JOptionPane.INFORMATION_MESSAGE);
      }
    }

    if (e.getSource() == multiSelectQueryPanel.getUnPickBtn()) {
      if (jxTableCaseSelected.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("Not selected row in table. Please select and try again!");
      }

      String selectedData = (String) jxTableCaseSelected.getDefaultTableModel()
          .getValueAt(jxTableCaseSelected.getJxTable().getSelectedRow(), 1);
      int selectedID = (Integer) jxTableCaseSelected.getDefaultTableModel()
          .getValueAt(jxTableCaseSelected.getJxTable().getSelectedRow(), 0);
      String selectedDescription = (String) jxTableCaseSelected.getDefaultTableModel()
          .getValueAt(jxTableCaseSelected.getJxTable().getSelectedRow(), 2);
      String selectedText = (String) jxTableCaseSelected.getDefaultTableModel()
          .getValueAt(jxTableCaseSelected.getJxTable().getSelectedRow(), 3);

      jxTableCase = tableToFill(selectedData, selectedID);

      if (selectedData != null) {
        if (jxTableCase == multiSelectQueryPanel.getQueryListCase()) {
          jxTableCase.getDefaultTableModel()
              .addRow(new Object[]{selectedID, selectedData, selectedDescription, selectedText});
          jxTableCase.getJxTable().setRowSelectionInterval(jxTableCase.getJxTable().getRowCount() - 1,
                                                           jxTableCase.getJxTable().getRowCount() - 1);
          multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);
        } else {
          multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(1);
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
      }
    }

    if (e.getSource() == multiSelectQueryPanel.getPickAllBtn()) {
      List<String> listSelectedQueryForExclude = getSelectedQueryNameList();
      String connDriver = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4).toString();

      List<Query> queryListAll;
      if (jxTableCase == multiSelectQueryPanel.getQueryListCase()) {
        jxTableCase.getDefaultTableModel().getDataVector().removeAllElements();
        jxTableCase.getDefaultTableModel().fireTableDataChanged();

        profileManager.getQueryInfoListByConnDriver(connDriver).stream()
            .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
            .forEach(q -> multiSelectQueryPanel.getSelectedQueryCase().getDefaultTableModel()
                .addRow(new Object[]{q.getId(), q.getName(), q.getDescription(), q.getText()}));

      } else {
        queryListAll = templateManager.getQueryListByConnDriver(connDriver).stream()
            .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
            .toList();

        for (Query query : queryListAll) {
          if (isExistName(query.getName())) {
            jxTableCaseSelected.getDefaultTableModel()
                .addRow(new Object[]{query.getId(), query.getName(), query.getDescription(), query.getText()});
          } else if (jxTableCase == multiSelectQueryPanel.getTemplateListQueryCase()) {
            JOptionPane.showMessageDialog(null,
                                          query.getName() + " query already exists, please choose  another one ",
                                          "Information", JOptionPane.INFORMATION_MESSAGE);
          }
        }
      }

      profileManager.updateCache();
    }

    if (e.getSource() == multiSelectQueryPanel.getUnPickAllBtn()) {
      jxTableCaseSelected.getDefaultTableModel().getDataVector().removeAllElements();
      jxTableCaseSelected.getDefaultTableModel().fireTableDataChanged();
      multiSelectQueryPanel.getQueryListCase().getDefaultTableModel().getDataVector().removeAllElements();
      multiSelectQueryPanel.getQueryListCase().getDefaultTableModel().fireTableDataChanged();

      String connDriver = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4).toString();
      List<String> listSelectedQueryForExclude = getSelectedQueryNameList();
      profileManager.getQueryInfoListByConnDriver(connDriver).stream()
          .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
          .forEach(q -> multiSelectQueryPanel.getQueryListCase().getDefaultTableModel()
              .addRow(new Object[]{q.getId(), q.getName(), q.getDescription(), q.getText()}));

      multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);
    }
  }

  private boolean isExistName(String selectedData) {
    boolean isExist = true;
    DefaultTableModel defaultTableModel = multiSelectQueryPanel.getSelectedQueryCase().getDefaultTableModel();
    for (int rowIndex = 0; rowIndex < defaultTableModel.getRowCount(); rowIndex++) {
      if (selectedData.equals(defaultTableModel.getValueAt(rowIndex, 1).toString())) {
        isExist = false;
      }
    }
    return isExist;
  }

  private JXTableCase tableToFill(String selectedName,
                                  int selectedId) {
    JXTableCase jxTableCase = multiSelectQueryPanel.getQueryListCase();
    DefaultTableModel defaultTableModel = multiSelectQueryPanel.getTemplateListQueryCase().getDefaultTableModel();
    for (int rowIndex = 0; rowIndex < defaultTableModel.getRowCount(); rowIndex++) {
      if (selectedName.equals(defaultTableModel.getValueAt(rowIndex, 1).toString())
          && selectedId == (Integer) (defaultTableModel.getValueAt(rowIndex, 0))) {
        jxTableCase = multiSelectQueryPanel.getTemplateListQueryCase();
      }
    }
    return jxTableCase;
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if (multiSelectQueryPanel.getJTabbedPaneQuery().getSelectedIndex() == 0) {
      this.jxTableCase = this.multiSelectQueryPanel.getQueryListCase();
    } else {
      this.jxTableCase = this.multiSelectQueryPanel.getTemplateListQueryCase();
    }
  }

  private List<String> getSelectedQueryNameList() {
    List<String> queryListId = new ArrayList<>();

    DefaultTableModel tableModel = multiSelectQueryPanel.getSelectedQueryCase().getDefaultTableModel();
    if (tableModel.getRowCount() > 0) {
      for (int i = 0; i < tableModel.getRowCount(); i++) {
        String selectedQueryName = tableModel.getValueAt(i, 1).toString();
        queryListId.add(selectedQueryName);
      }
    }

    return queryListId;
  }
}