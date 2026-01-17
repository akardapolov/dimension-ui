package ru.dimension.ui.view.handler.profile;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.handler.core.TTTableSelection;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Singleton
public final class MultiSelectTaskHandler {

  private final MultiSelectTaskPanel panel;
  private final ProfileManager profileManager;
  private final TemplateManager templateManager;

  @Inject
  public MultiSelectTaskHandler(@Named("multiSelectPanel") MultiSelectTaskPanel panel,
                                @Named("profileManager") ProfileManager profileManager,
                                @Named("templateManager") TemplateManager templateManager) {
    this.panel = panel;
    this.profileManager = profileManager;
    this.templateManager = templateManager;

    panel.getPickBtn().addActionListener(e -> handlePick());
    panel.getUnPickBtn().addActionListener(e -> handleUnPick());
    panel.getPickAllBtn().addActionListener(e -> handlePickAll());
    panel.getUnPickAllBtn().addActionListener(e -> handleUnPickAll());

    panel.getJTabbedPaneTask().addChangeListener(e -> updateButtonStates());
  }

  private void handlePick() {
    JXTableCase sourceCase = getActiveSourceCase();
    TTTableSelection.<TaskRow>selectedItem(sourceCase).ifPresent(row -> {
      if (isAlreadySelected(row.getName())) {
        JOptionPane.showMessageDialog(null, row.getName() + " already exists", "Info", JOptionPane.INFORMATION_MESSAGE);
        return;
      }
      panel.getSelectedTaskCase().getTypedTable().addItem(row);
      if (sourceCase == panel.getTaskListCase()) {
        removeRow(sourceCase, row);
      }
    });
    updateButtonStates();
  }

  private void handleUnPick() {
    TTTableSelection.<TaskRow>selectedItem(panel.getSelectedTaskCase()).ifPresent(row -> {
      removeRow(panel.getSelectedTaskCase(), row);
      if (isConfigTask(row)) {
        panel.getTaskListCase().getTypedTable().addItem(row);
      }
    });
    updateButtonStates();
  }

  private void handlePickAll() {
    TTTable<TaskRow, JXTable> target = panel.getSelectedTaskCase().getTypedTable();
    if (panel.getJTabbedPaneTask().getSelectedIndex() == 0) {
      profileManager.getTaskInfoList().forEach(t -> {
        if (!isAlreadySelected(t.getName())) target.addItem(new TaskRow(t.getId(), t.getName()));
      });
      panel.getTaskListCase().clearTable();
    } else {
      templateManager.getConfigList(Task.class).forEach(t -> {
        if (!isAlreadySelected(t.getName())) target.addItem(new TaskRow(t.getId(), t.getName()));
      });
    }
    updateButtonStates();
  }

  private void handleUnPickAll() {
    panel.getSelectedTaskCase().clearTable();

    panel.getTaskListCase().clearTable();
    panel.getTaskListCase().getTypedTable().setItems(profileManager.getTaskInfoList().stream()
                                         .map(t -> new TaskRow(t.getId(), t.getName())).collect(Collectors.toList()));
    updateButtonStates();
  }

  private JXTableCase getActiveSourceCase() {
    return panel.getJTabbedPaneTask().getSelectedIndex() == 0 ? panel.getTaskListCase() : panel.getTemplateListTaskCase();
  }

  private boolean isAlreadySelected(String name) {
    TTTable<TaskRow, JXTable> target = panel.getSelectedTaskCase().getTypedTable();
    return target.model().items().stream().anyMatch(r -> r.getName().equals(name));
  }

  private boolean isConfigTask(TaskRow row) {
    return profileManager.getTaskInfoList().stream().anyMatch(t -> t.getId() == row.getId() && t.getName().equals(row.getName()));
  }

  private void removeRow(JXTableCase tableCase, TaskRow row) {
    TTTable<TaskRow, JXTable> tt = tableCase.getTypedTable();
    List<TaskRow> items = new ArrayList<>(tt.model().items());
    items.removeIf(i -> i.getId() == row.getId() && i.getName().equals(row.getName()));
    tt.setItems(items);
  }

  private void updateButtonStates() {
    boolean hasSource = getActiveSourceCase().getJxTable().getSelectedRow() != -1;
    boolean hasSelected = panel.getSelectedTaskCase().getJxTable().getSelectedRow() != -1;
    panel.getPickBtn().setEnabled(hasSource);
    panel.getUnPickBtn().setEnabled(hasSelected);
  }
}