package ru.dimension.ui.view.panel.config.ui;

import jakarta.inject.Singleton;
import lombok.Data;
import ru.dimension.ui.model.config.UISettings;

@Data
@Singleton
public class UISettingsModel {

  private UISettings settings;

  public UISettingsModel() {
    this.settings = new UISettings();
  }

}
