package ru.dimension.ui.view.panel.config.ui;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.model.config.UISettings;

@Log4j2
@Singleton
public class UISettingsPresenter {

  private final UISettingsModel model;
  private final UISettingsView view;
  private final ConfigurationManager configurationManager;

  @Inject
  public UISettingsPresenter(UISettingsModel model,
                             UISettingsView view,
                             ConfigurationManager configurationManager) {
    this.model = model;
    this.view = view;
    this.configurationManager = configurationManager;

    setupListeners();
    loadSettings();
  }

  private void setupListeners() {
    view.getAreaListPanel().addSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        String selected = view.getAreaListPanel().getSelectedArea();
        log.debug("Selected area: {}", selected);
      }
    });

    view.getChartSettingsPanel().addChangeListener(e -> saveSettings());
  }
  private void loadSettings() {
    try {
      List<UISettings> settingsList = configurationManager.getConfigList(UISettings.class);
      UISettings settings = settingsList.isEmpty() ? new UISettings() : settingsList.get(0);
      model.setSettings(settings);

      view.getChartSettingsPanel().setHideBuiltInLegend(settings.getChartSettings().isHideBuiltInLegend());
      view.getChartSettingsPanel().setHideYAxis(settings.getChartSettings().isHideYAxis());
      view.getChartSettingsPanel().setHideXAxis(settings.getChartSettings().isHideXAxis());
      view.getChartSettingsPanel().setHidePlotInsets(settings.getChartSettings().isHidePlotInsets());
      view.getChartSettingsPanel().setHideChartPadding(settings.getChartSettings().isHideChartPadding());
      view.getChartSettingsPanel().setLegendFixedSize(settings.getChartSettings().isLegendFixedSize());
      view.getChartSettingsPanel().setLegendFixedWidth(settings.getChartSettings().getLegendFixedWidth());
    } catch (Exception e) {
      log.error("Failed to load UI settings", e);
    }
  }

  private void saveSettings() {
    try {
      UISettings settings = model.getSettings();
      settings.getChartSettings().setHideBuiltInLegend(view.getChartSettingsPanel().isHideBuiltInLegend());
      settings.getChartSettings().setHideYAxis(view.getChartSettingsPanel().isHideYAxis());
      settings.getChartSettings().setHideXAxis(view.getChartSettingsPanel().isHideXAxis());
      settings.getChartSettings().setHidePlotInsets(view.getChartSettingsPanel().isHidePlotInsets());
      settings.getChartSettings().setHideChartPadding(view.getChartSettingsPanel().isHideChartPadding());
      settings.getChartSettings().setLegendFixedSize(view.getChartSettingsPanel().isLegendFixedSize());
      settings.getChartSettings().setLegendFixedWidth(view.getChartSettingsPanel().getLegendFixedWidth());

      List<UISettings> existing = configurationManager.getConfigList(UISettings.class);
      if (existing.isEmpty()) {
        configurationManager.addConfig(settings, UISettings.class);
      } else {
        configurationManager.updateConfig(settings, UISettings.class);
      }
      log.info("UI settings saved");
    } catch (Exception e) {
      log.error("Failed to save UI settings", e);
    }
  }
}
