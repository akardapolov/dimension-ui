package ru.dimension.ui.view.panel.config.ui;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.Message;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Action;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.model.config.UISettings;

@Log4j2
@Singleton
public class UISettingsPresenter {

  private final MessageBroker broker = MessageBroker.getInstance();

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
      view.getChartSettingsPanel().setHideYAxisLabel(settings.getChartSettings().isHideYAxisLabel());
      view.getChartSettingsPanel().setHideYAxisValues(settings.getChartSettings().isHideYAxisValues());
      view.getChartSettingsPanel().setHideXAxisLabel(settings.getChartSettings().isHideXAxisLabel());
      view.getChartSettingsPanel().setHideXAxisValues(settings.getChartSettings().isHideXAxisValues());
      view.getChartSettingsPanel().setHidePlotInsets(settings.getChartSettings().isHidePlotInsets());
      view.getChartSettingsPanel().setHideChartPadding(settings.getChartSettings().isHideChartPadding());
      view.getChartSettingsPanel().setLegendFixedSize(settings.getChartSettings().isLegendFixedSize());
      view.getChartSettingsPanel().setLegendFixedWidth(settings.getChartSettings().getLegendFixedWidth());
      view.getChartSettingsPanel().setValueAbbreviation(settings.getChartSettings().isValueAbbreviation());
      view.getChartSettingsPanel().setValueAxisFixedSize(settings.getChartSettings().isValueAxisFixedSize());
      view.getChartSettingsPanel().setValueAxisFixedWidth(settings.getChartSettings().getValueAxisFixedWidth());
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
      settings.getChartSettings().setHideYAxisLabel(view.getChartSettingsPanel().isHideYAxisLabel());
      settings.getChartSettings().setHideYAxisValues(view.getChartSettingsPanel().isHideYAxisValues());
      settings.getChartSettings().setHideXAxisLabel(view.getChartSettingsPanel().isHideXAxisLabel());
      settings.getChartSettings().setHideXAxisValues(view.getChartSettingsPanel().isHideXAxisValues());
      settings.getChartSettings().setHidePlotInsets(view.getChartSettingsPanel().isHidePlotInsets());
      settings.getChartSettings().setHideChartPadding(view.getChartSettingsPanel().isHideChartPadding());
      settings.getChartSettings().setLegendFixedSize(view.getChartSettingsPanel().isLegendFixedSize());
      settings.getChartSettings().setLegendFixedWidth(view.getChartSettingsPanel().getLegendFixedWidth());
      settings.getChartSettings().setValueAbbreviation(view.getChartSettingsPanel().isValueAbbreviation());
      settings.getChartSettings().setValueAxisFixedSize(view.getChartSettingsPanel().isValueAxisFixedSize());
      settings.getChartSettings().setValueAxisFixedWidth(view.getChartSettingsPanel().getValueAxisFixedWidth());

      List<UISettings> existing = configurationManager.getConfigList(UISettings.class);
      if (existing.isEmpty()) {
        configurationManager.addConfig(settings, UISettings.class);
      } else {
        configurationManager.updateConfig(settings, UISettings.class);
      }

      notifyChartUISettingsChanged();

      log.info("UI settings saved");
    } catch (Exception e) {
      log.error("Failed to save UI settings", e);
    }
  }

  private void notifyChartUISettingsChanged() {
    for (Component targetComponent : Component.values()) {
      if (Component.CONFIGURATION.equals(targetComponent) || Component.PLAYGROUND.equals(targetComponent)) {
        continue;
      }

      broker.sendMessage(Message.builder()
          .destination(Destination.withDefault(targetComponent, Module.CHARTS))
          .action(Action.CHART_UI_SETTINGS_CHANGED)
          .build());
    }
  }
}
