package ru.dimension.ui.view.handler.task;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.exception.NotSelectedRowException;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.panel.config.task.MultiSelectQueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;
import ru.dimension.ui.view.table.row.Rows.QueryTableRow;

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

      TTTable<QueryTableRow, JXTable> sourceTT = jxTableCase.getTypedTable();
      int selectedRow = jxTableCase.getJxTable().getSelectedRow();
      QueryTableRow selectedItem = sourceTT.model().itemAt(selectedRow);

      if (selectedItem == null) {
        return;
      }

      if (isExistName(selectedItem.getName())) {
        TTTable<QueryTableRow, JXTable> selectedTT = jxTableCaseSelected.getTypedTable();
        List<QueryTableRow> selectedItems = new ArrayList<>(selectedTT.model().items());
        selectedItems.add(selectedItem);
        selectedTT.setItems(selectedItems);

        jxTableCaseSelected.getJxTable().setRowSelectionInterval(
            jxTableCaseSelected.getJxTable().getRowCount() - 1,
            jxTableCaseSelected.getJxTable().getRowCount() - 1
        );

        if (jxTableCase == multiSelectQueryPanel.getQueryListCase()) {
          List<QueryTableRow> sourceItems = new ArrayList<>(sourceTT.model().items());
          sourceItems.remove(selectedRow);
          sourceTT.setItems(sourceItems);

          if (jxTableCase.getJxTable().getRowCount() > 0) {
            jxTableCase.getJxTable().setRowSelectionInterval(
                jxTableCase.getJxTable().getRowCount() - 1,
                jxTableCase.getJxTable().getRowCount() - 1
            );
          }
        }
      } else if (jxTableCase == multiSelectQueryPanel.getTemplateListQueryCase()) {
        JOptionPane.showMessageDialog(null,
                                      selectedItem.getName() + " query already exists, please choose another one",
                                      "Information", JOptionPane.INFORMATION_MESSAGE);
      }
    }

    if (e.getSource() == multiSelectQueryPanel.getUnPickBtn()) {
      if (jxTableCaseSelected.getJxTable().getSelectedRow() == -1) {
        throw new NotSelectedRowException("Not selected row in table. Please select and try again!");
      }

      TTTable<QueryTableRow, JXTable> selectedTT = jxTableCaseSelected.getTypedTable();
      int selectedRow = jxTableCaseSelected.getJxTable().getSelectedRow();
      QueryTableRow selectedItem = selectedTT.model().itemAt(selectedRow);

      if (selectedItem == null) {
        return;
      }

      jxTableCase = tableToFill(selectedItem.getName(), selectedItem.getId());

      if (jxTableCase == multiSelectQueryPanel.getQueryListCase()) {
        TTTable<QueryTableRow, JXTable> queryListTT = jxTableCase.getTypedTable();
        List<QueryTableRow> queryListItems = new ArrayList<>(queryListTT.model().items());
        queryListItems.add(selectedItem);
        queryListTT.setItems(queryListItems);

        jxTableCase.getJxTable().setRowSelectionInterval(
            jxTableCase.getJxTable().getRowCount() - 1,
            jxTableCase.getJxTable().getRowCount() - 1
        );
        multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);
      } else {
        multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(1);
      }

      List<QueryTableRow> selectedItems = new ArrayList<>(selectedTT.model().items());
      selectedItems.remove(selectedRow);
      selectedTT.setItems(selectedItems);

      if (jxTableCaseSelected.getJxTable().getRowCount() > 0) {
        jxTableCaseSelected.getJxTable().setRowSelectionInterval(
            jxTableCaseSelected.getJxTable().getRowCount() - 1,
            jxTableCaseSelected.getJxTable().getRowCount() - 1
        );
      }
    }

    if (e.getSource() == multiSelectQueryPanel.getPickAllBtn()) {
      List<String> listSelectedQueryForExclude = getSelectedQueryNameList();
      String connDriver = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4).toString();

      TTTable<QueryTableRow, JXTable> selectedTT = jxTableCaseSelected.getTypedTable();
      List<QueryTableRow> currentSelected = new ArrayList<>(selectedTT.model().items());

      if (jxTableCase == multiSelectQueryPanel.getQueryListCase()) {
        TTTable<QueryTableRow, JXTable> queryListTT = jxTableCase.getTypedTable();

        // Добавляем все из query list в selected
        List<QueryTableRow> toAdd = profileManager.getQueryInfoListByConnDriver(connDriver).stream()
            .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
            .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
            .collect(Collectors.toList());

        currentSelected.addAll(toAdd);
        selectedTT.setItems(currentSelected);

        // Очищаем query list
        queryListTT.setItems(new ArrayList<>());

      } else {
        List<Query> queryListAll = templateManager.getQueryListByConnDriver(connDriver).stream()
            .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
            .toList();

        for (Query query : queryListAll) {
          if (isExistName(query.getName())) {
            currentSelected.add(new QueryTableRow(
                query.getId(), query.getName(), query.getDescription(), query.getText()
            ));
          } else {
            JOptionPane.showMessageDialog(null,
                                          query.getName() + " query already exists, please choose another one",
                                          "Information", JOptionPane.INFORMATION_MESSAGE);
          }
        }
        selectedTT.setItems(currentSelected);
      }

      profileManager.updateCache();
    }

    if (e.getSource() == multiSelectQueryPanel.getUnPickAllBtn()) {
      TTTable<QueryTableRow, JXTable> selectedTT = jxTableCaseSelected.getTypedTable();
      selectedTT.setItems(new ArrayList<>());

      String connDriver = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4).toString();
      List<String> listSelectedQueryForExclude = getSelectedQueryNameList();

      List<QueryTableRow> queryListRows = profileManager.getQueryInfoListByConnDriver(connDriver).stream()
          .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
          .map(q -> new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()))
          .collect(Collectors.toList());

      TTTable<QueryTableRow, JXTable> queryListTT = multiSelectQueryPanel.getQueryListCase().getTypedTable();
      queryListTT.setItems(queryListRows);

      multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);
    }
  }

  private boolean isExistName(String selectedData) {
    TTTable<QueryTableRow, JXTable> selectedTT = multiSelectQueryPanel.getSelectedQueryCase().getTypedTable();
    List<QueryTableRow> items = selectedTT.model().items();
    return items.stream().noneMatch(row -> selectedData.equals(row.getName()));
  }

  private JXTableCase tableToFill(String selectedName, int selectedId) {
    JXTableCase result = multiSelectQueryPanel.getQueryListCase();

    TTTable<QueryTableRow, JXTable> templateTT = multiSelectQueryPanel.getTemplateListQueryCase().getTypedTable();
    List<QueryTableRow> templateItems = templateTT.model().items();

    for (QueryTableRow row : templateItems) {
      if (selectedName.equals(row.getName()) && selectedId == row.getId()) {
        result = multiSelectQueryPanel.getTemplateListQueryCase();
        break;
      }
    }
    return result;
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
    List<String> queryListNames = new ArrayList<>();
    TTTable<QueryTableRow, JXTable> selectedTT = multiSelectQueryPanel.getSelectedQueryCase().getTypedTable();
    List<QueryTableRow> items = selectedTT.model().items();
    for (QueryTableRow row : items) {
      queryListNames.add(row.getName());
    }
    return queryListNames;
  }
}