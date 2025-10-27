package ru.dimension.ui.component.module.preview.container;

import java.awt.Component;
import ru.dimension.ui.component.module.preview.PreviewView;

public class PreviewPanelContainer implements IPreviewContainer {
  private final PreviewView panel;

  public PreviewPanelContainer(PreviewView panel) {
    this.panel = panel;
  }

  @Override
  public void show() {
    panel.setVisible(true);
  }

  @Override
  public Component getComponent() {
    return panel;
  }
}