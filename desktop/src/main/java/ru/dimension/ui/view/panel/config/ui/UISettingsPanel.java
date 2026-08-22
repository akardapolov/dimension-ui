package ru.dimension.ui.view.panel.config.ui;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.manager.ConfigurationManager;

@Singleton
public class UISettingsPanel extends JPanel implements UISettingsView {

  private final AreaListPanel areaListPanel;
  private final ChartSettingsPanel chartSettingsPanel;

  @Inject
  public UISettingsPanel() {
    setLayout(new BorderLayout());

    areaListPanel = new AreaListPanel();
    chartSettingsPanel = new ChartSettingsPanel();

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, areaListPanel, chartSettingsPanel);
    splitPane.setResizeWeight(0.25);
    splitPane.setDividerSize(3);
    splitPane.setContinuousLayout(true);
    splitPane.setMinimumSize(new Dimension(100, 100));

    add(splitPane, BorderLayout.CENTER);

    initializePresenter();
  }

  private void initializePresenter() {
    try {
      ConfigurationManager configurationManager = ServiceLocator.get(ConfigurationManager.class);
      UISettingsModel model = new UISettingsModel();
      new UISettingsPresenter(model, this, configurationManager);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize UI settings presenter", e);
    }
  }

  @Override
  public AreaListPanel getAreaListPanel() {
    return areaListPanel;
  }

  @Override
  public ChartSettingsPanel getChartSettingsPanel() {
    return chartSettingsPanel;
  }

}
