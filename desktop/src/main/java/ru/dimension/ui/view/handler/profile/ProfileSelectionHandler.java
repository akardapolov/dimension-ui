package ru.dimension.ui.view.handler.profile;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.handler.core.AbstractTableSelectionHandler;
import ru.dimension.ui.view.handler.core.ButtonPanelBindings;
import ru.dimension.ui.view.handler.core.ConfigSelectionContext;
import ru.dimension.ui.view.handler.core.RelatedHighlightService;
import ru.dimension.ui.view.panel.config.ButtonPanel;
import ru.dimension.ui.view.panel.config.profile.MultiSelectTaskPanel;
import ru.dimension.ui.view.panel.config.profile.ProfilePanel;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
@Singleton
public final class ProfileSelectionHandler extends AbstractTableSelectionHandler<ProfileRow> {

  private final ProfileManager profileManager;
  private final TemplateManager templateManager;
  private final ConfigSelectionContext context;
  private final ProfilePanel profilePanel;
  private final MultiSelectTaskPanel multiSelectPanel;
  private final ButtonPanel buttonPanel;
  private final JCheckBox checkboxConfig;
  private final JXTableCase taskCase;
  private final RelatedHighlightService highlightService;

  @Inject
  public ProfileSelectionHandler(@Named("profileConfigCase") JXTableCase profileCase,
                                 @Named("taskConfigCase") JXTableCase taskCase,
                                 @Named("profileManager") ProfileManager profileManager,
                                 @Named("templateManager") TemplateManager templateManager,
                                 @Named("configSelectionContext") ConfigSelectionContext context,
                                 @Named("profileConfigPanel") ProfilePanel profilePanel,
                                 @Named("multiSelectPanel") MultiSelectTaskPanel multiSelectPanel,
                                 @Named("profileButtonPanel") ButtonPanel buttonPanel,
                                 @Named("checkboxConfig") JCheckBox checkboxConfig,
                                 @Named("relatedHighlightService") RelatedHighlightService highlightService) {
    super(profileCase);
    this.taskCase = taskCase;
    this.profileManager = profileManager;
    this.templateManager = templateManager;
    this.context = context;
    this.profilePanel = profilePanel;
    this.multiSelectPanel = multiSelectPanel;
    this.buttonPanel = buttonPanel;
    this.checkboxConfig = checkboxConfig;
    this.highlightService = highlightService;

    bind();

    this.checkboxConfig.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        highlightService.clearAllHighlights();
        applyHierarchyMode();
      } else {
        applyFullMode();
        highlightService.highlightFromProfile(context.getSelectedProfileId());
      }
    });

    if (checkboxConfig.isSelected()) {
      applyHierarchyMode();
    } else {
      applyFullModeWithoutRestore();
    }
  }

  @Override
  protected void onSelection(Optional<ProfileRow> item) {
    Integer id = item.map(ProfileRow::getId).orElse(null);
    context.setSelectedProfileId(id);
    ButtonPanelBindings.setViewMode(buttonPanel, id != null);

    updateFields(id);
    updateTaskMultiSelect(id);

    if (checkboxConfig.isSelected()) {
      applyHierarchyMode();
    } else {
      highlightService.highlightFromProfile(id);
    }
  }

  private void updateFields(Integer id) {
    if (id == null) {
      profilePanel.getJTextFieldProfile().setText("");
      profilePanel.getJTextFieldDescription().setText("");
      return;
    }
    ProfileInfo info = profileManager.getProfileInfoById(id);
    if (info != null) {
      profilePanel.getJTextFieldProfile().setText(info.getName());
      profilePanel.getJTextFieldDescription().setText(info.getDescription());
    }
  }

  private void updateTaskMultiSelect(Integer id) {
    TTTable<TaskRow, JXTable> ttSelected = multiSelectPanel.getSelectedTaskCase().getTypedTable();
    TTTable<TaskRow, JXTable> ttAvailable = multiSelectPanel.getTaskListCase().getTypedTable();
    TTTable<TaskRow, JXTable> ttTemplate = multiSelectPanel.getTemplateListTaskCase().getTypedTable();

    ttTemplate.setItems(templateManager.getConfigList(Task.class).stream()
                            .map(t -> new TaskRow(t.getId(), t.getName())).collect(Collectors.toList()));

    List<Integer> selectedIds = (id == null) ? List.of() :
        Optional.ofNullable(profileManager.getProfileInfoById(id))
            .map(ProfileInfo::getTaskInfoList).orElse(List.of());

    List<TaskInfo> allTasks = profileManager.getTaskInfoList();

    ttSelected.setItems(allTasks.stream()
                            .filter(t -> selectedIds.contains(t.getId()))
                            .map(t -> new TaskRow(t.getId(), t.getName())).collect(Collectors.toList()));

    ttAvailable.setItems(allTasks.stream()
                             .filter(t -> !selectedIds.contains(t.getId()))
                             .map(t -> new TaskRow(t.getId(), t.getName())).collect(Collectors.toList()));
  }

  private void applyHierarchyMode() {
    Integer pId = context.getSelectedProfileId();
    TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();

    if (pId == null) {
      tt.setItems(List.of());
      return;
    }

    ProfileInfo info = profileManager.getProfileInfoById(pId);
    if (info != null) {
      tt.setItems(info.getTaskInfoList().stream()
                      .map(profileManager::getTaskInfoById)
                      .filter(Objects::nonNull)
                      .map(t -> new TaskRow(t.getId(), t.getName()))
                      .collect(Collectors.toList()));
    } else {
      tt.setItems(List.of());
    }

    selectFirstRow(taskCase);
  }

  private void applyFullMode() {
    Integer currentTaskId = context.getSelectedTaskId();
    TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();

    List<TaskRow> rows = profileManager.getTaskInfoList().stream()
        .map(t -> new TaskRow(t.getId(), t.getName()))
        .collect(Collectors.toList());
    tt.setItems(rows);

    restoreSelection(taskCase, rows, currentTaskId);
  }

  private void applyFullModeWithoutRestore() {
    TTTable<TaskRow, JXTable> tt = taskCase.getTypedTable();
    tt.setItems(profileManager.getTaskInfoList().stream()
                    .map(t -> new TaskRow(t.getId(), t.getName()))
                    .collect(Collectors.toList()));
  }

  private void restoreSelection(JXTableCase tableCase, List<TaskRow> rows, Integer targetId) {
    if (targetId == null || rows.isEmpty()) {
      return;
    }

    JXTable table = tableCase.getJxTable();
    if (table == null) {
      return;
    }

    SwingUtilities.invokeLater(() -> {
      for (int i = 0; i < rows.size(); i++) {
        if (Objects.equals(rows.get(i).getId(), targetId)) {
          table.setRowSelectionInterval(i, i);
          return;
        }
      }
    });
  }

  private void selectFirstRow(JXTableCase tableCase) {
    JXTable table = tableCase.getJxTable();
    if (table == null) return;

    SwingUtilities.invokeLater(() -> {
      if (table.getRowCount() > 0) {
        table.setRowSelectionInterval(0, 0);
      } else {
        table.clearSelection();
      }
    });
  }
}