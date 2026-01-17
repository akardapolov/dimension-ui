package ru.dimension.ui.view.handler.core;

import java.awt.event.ActionListener;
import java.util.Objects;
import javax.swing.JButton;
import ru.dimension.ui.view.panel.config.ButtonPanel;

public final class ButtonPanelBindings {

  private ButtonPanelBindings() {
  }

  public interface CrudActions {
    default void onNew() {}
    default void onCopy() {}
    default void onDelete() {}
    default void onEdit() {}
    default void onSave() {}
    default void onCancel() {}
  }

  public static void bind(ButtonPanel panel, CrudActions actions) {
    Objects.requireNonNull(panel, "panel");
    Objects.requireNonNull(actions, "actions");

    bind(panel.getBtnNew(), actions::onNew);
    bind(panel.getBtnCopy(), actions::onCopy);
    bind(panel.getBtnDel(), actions::onDelete);
    bind(panel.getBtnEdit(), actions::onEdit);
    bind(panel.getBtnSave(), actions::onSave);
    bind(panel.getBtnCancel(), actions::onCancel);
  }

  public static void bind(JButton button, Runnable action) {
    if (button == null || action == null) {
      return;
    }
    ActionListener listener = e -> action.run();
    button.addActionListener(listener);
  }

  public static void setViewMode(ButtonPanel panel, boolean hasSelection) {
    if (panel == null) {
      return;
    }

    panel.getBtnNew().setEnabled(true);
    panel.getBtnCopy().setEnabled(hasSelection);
    panel.getBtnDel().setEnabled(hasSelection);
    panel.getBtnEdit().setEnabled(hasSelection);

    panel.getBtnSave().setEnabled(false);
    panel.getBtnCancel().setEnabled(false);
  }

  public static void setEditMode(ButtonPanel panel) {
    if (panel == null) {
      return;
    }

    panel.getBtnNew().setEnabled(false);
    panel.getBtnCopy().setEnabled(false);
    panel.getBtnDel().setEnabled(false);
    panel.getBtnEdit().setEnabled(false);

    panel.getBtnSave().setEnabled(true);
    panel.getBtnCancel().setEnabled(true);
  }
}