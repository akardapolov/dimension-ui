package ru.dimension.ui.view.handler.core;

import jakarta.inject.Inject;
import ru.dimension.ui.view.panel.config.ButtonPanel;

public class SimpleCrudButtonPanelHandler {

  private final ButtonPanel panel;

  @Inject
  public SimpleCrudButtonPanelHandler(ButtonPanel panel) {
    this.panel = panel;

    ButtonPanelBindings.bind(panel, new ButtonPanelBindings.CrudActions() {
      @Override
      public void onNew() {
        ButtonPanelBindings.setEditMode(SimpleCrudButtonPanelHandler.this.panel);
      }

      @Override
      public void onEdit() {
        ButtonPanelBindings.setEditMode(SimpleCrudButtonPanelHandler.this.panel);
      }

      @Override
      public void onSave() {
        ButtonPanelBindings.setViewMode(SimpleCrudButtonPanelHandler.this.panel, true);
      }

      @Override
      public void onCancel() {
        ButtonPanelBindings.setViewMode(SimpleCrudButtonPanelHandler.this.panel, true);
      }
    });

    ButtonPanelBindings.setViewMode(panel, false);
  }
}