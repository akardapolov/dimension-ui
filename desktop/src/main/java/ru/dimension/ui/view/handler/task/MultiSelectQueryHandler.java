package ru.dimension.ui.view.handler.task;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JOptionPane;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.view.handler.core.TTTableSelection;
import ru.dimension.ui.view.panel.config.task.MultiSelectQueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;
import ru.dimension.ui.view.table.row.Rows.QueryTableRow;

@Singleton
public final class MultiSelectQueryHandler {

  private final MultiSelectQueryPanel panel;
  private final TaskPanel taskPanel;
  private final ProfileManager profileManager;
  private final TemplateManager templateManager;

  @Inject
  public MultiSelectQueryHandler(@Named("multiSelectQueryPanel") MultiSelectQueryPanel panel,
                                 @Named("taskConfigPanel") TaskPanel taskPanel,
                                 @Named("profileManager") ProfileManager profileManager,
                                 @Named("templateManager") TemplateManager templateManager) {
    this.panel = panel;
    this.taskPanel = taskPanel;
    this.profileManager = profileManager;
    this.templateManager = templateManager;

    panel.getPickBtn().addActionListener(e -> handlePick());
    panel.getUnPickBtn().addActionListener(e -> handleUnPick());
    panel.getPickAllBtn().addActionListener(e -> handlePickAll());
    panel.getUnPickAllBtn().addActionListener(e -> handleUnPickAll());

    panel.getJTabbedPaneQuery().addChangeListener(e -> updateButtonStates());
  }

  private void handlePick() {
    JXTableCase sourceCase = getActiveSourceCase();
    TTTableSelection.<QueryTableRow>selectedItem(sourceCase).ifPresent(row -> {
      if (isAlreadySelected(row.getName())) {
        JOptionPane.showMessageDialog(null, row.getName() + " already exists", "Info", JOptionPane.INFORMATION_MESSAGE);
        return;
      }
      panel.getSelectedQueryCase().getTypedTable().addItem(row);
      if (sourceCase == panel.getQueryListCase()) removeRow(sourceCase, row);
    });
    updateButtonStates();
  }

  private void handleUnPick() {
    TTTableSelection.<QueryTableRow>selectedItem(panel.getSelectedQueryCase()).ifPresent(row -> {
      removeRow(panel.getSelectedQueryCase(), row);
      if (isConfigQuery(row)) panel.getQueryListCase().getTypedTable().addItem(row);
    });
    updateButtonStates();
  }

  private void handlePickAll() {
    TTTable<QueryTableRow, JXTable> target = panel.getSelectedQueryCase().getTypedTable();

    if (panel.getJTabbedPaneQuery().getSelectedIndex() == 0) {
      TTTable<QueryTableRow, JXTable> queryListCase = panel.getQueryListCase().getTypedTable();

      queryListCase.model().items().forEach(row -> {
        if (!isAlreadySelected(row.getName())) {
          target.addItem(row);
        }
      });
      panel.getQueryListCase().clearTable();
    } else {
      String driver = getDriver();
      if (driver != null) {
        templateManager.getQueryListByConnDriver(driver).forEach(q -> {
          if (!isAlreadySelected(q.getName())) {
            target.addItem(new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()));
          }
        });
      }
    }
    updateButtonStates();
  }

  private void handleUnPickAll() {
    panel.getSelectedQueryCase().clearTable();
    rebuildAvailableQueryList();
    updateButtonStates();
  }

  private void rebuildAvailableQueryList() {
    String driver = getDriver();
    ConnectionType connType = getConnectionType();
    boolean hasConnection = hasConnection();

    List<QueryTableRow> available = new ArrayList<>();
    Set<Integer> addedIds = new HashSet<>();

    if (hasConnection && connType == ConnectionType.HTTP) {
      profileManager.getHttpOrphanQueryInfoList().stream()
          .filter(q -> !addedIds.contains(q.getId()))
          .forEach(q -> {
            available.add(new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()));
            addedIds.add(q.getId());
          });
    } else {
      if (driver != null) {
        profileManager.getQueryInfoListByConnDriver(driver).forEach(q -> {
          available.add(new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()));
          addedIds.add(q.getId());
        });
      }

      profileManager.getOrphanQueryInfoList().stream()
          .filter(q -> !addedIds.contains(q.getId()))
          .forEach(q -> {
            available.add(new QueryTableRow(q.getId(), q.getName(), q.getDescription(), q.getText()));
            addedIds.add(q.getId());
          });
    }

    TTTable<QueryTableRow, JXTable> target = panel.getQueryListCase().getTypedTable();
    target.setItems(available);
  }

  private ConnectionType getConnectionType() {
    try {
      Object typeObj = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(5);
      return typeObj instanceof ConnectionType ? (ConnectionType) typeObj : ConnectionType.valueOf(typeObj.toString());
    } catch (Exception e) {
      return null;
    }
  }

  private boolean hasConnection() {
    try {
      List<?> row = taskPanel.getTaskConnectionComboBox().getSelectedRow();
      return row != null && !row.isEmpty() && row.getFirst() != null;
    } catch (Exception e) {
      return false;
    }
  }

  private String getDriver() {
    try { return taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4).toString(); } catch (Exception e) { return null; }
  }

  private JXTableCase getActiveSourceCase() {
    return panel.getJTabbedPaneQuery().getSelectedIndex() == 0 ? panel.getQueryListCase() : panel.getTemplateListQueryCase();
  }

  private boolean isAlreadySelected(String name) {
    TTTable<QueryTableRow, JXTable> tt = panel.getSelectedQueryCase().getTypedTable();
    return tt.model().items().stream().anyMatch(r -> r.getName().equals(name));
  }

  private boolean isConfigQuery(QueryTableRow row) {
    return profileManager.getQueryInfoList().stream().anyMatch(q -> q.getId() == row.getId() && q.getName().equals(row.getName()));
  }

  private void removeRow(JXTableCase tableCase, QueryTableRow row) {
    TTTable<QueryTableRow, JXTable> tt = tableCase.getTypedTable();
    List<QueryTableRow> items = new ArrayList<>(tt.model().items());
    items.removeIf(i -> i.getId() == row.getId() && i.getName().equals(row.getName()));
    tt.setItems(items);
  }

  private void updateButtonStates() {
    boolean hasSource = getActiveSourceCase().getJxTable().getSelectedRow() != -1;
    boolean hasSelected = panel.getSelectedQueryCase().getJxTable().getSelectedRow() != -1;
    panel.getPickBtn().setEnabled(hasSource);
    panel.getUnPickBtn().setEnabled(hasSelected);
  }
}