package ru.dimension.ui.config.ui;

import static ru.dimension.ui.model.view.ToolbarAction.CONFIGURATION;
import static ru.dimension.ui.model.view.ToolbarAction.TEMPLATE;

import java.awt.BorderLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import ru.dimension.di.DimensionDI;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.component.module.ChartsModule;
import ru.dimension.ui.component.module.ConfigModule;
import ru.dimension.ui.component.module.ManageModule;
import ru.dimension.ui.component.module.ModelModule;
import ru.dimension.ui.component.module.PreviewModule;
import ru.dimension.ui.component.module.adhoc.AdHocChartsModule;
import ru.dimension.ui.component.module.adhoc.AdHocConfigModule;
import ru.dimension.ui.component.module.adhoc.AdHocModelModule;
import ru.dimension.ui.component.module.factory.*;
import ru.dimension.ui.component.module.manage.ManagePresenter;
import ru.dimension.ui.component.module.preview.PreviewChartsModule;
import ru.dimension.ui.component.module.preview.PreviewConfigModule;
import ru.dimension.ui.component.module.report.DesignModule;
import ru.dimension.ui.component.module.report.PlaygroundModule;
import ru.dimension.ui.component.module.report.playground.MetricColumnPanel;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.view.tab.MainTabPane;
import ru.dimension.ui.view.icon.ConfigurationIcon;
import ru.dimension.ui.view.icon.TemplateIcon;
import ru.dimension.ui.view.tab.MainTab;

public final class UIBaseConfig {

  private UIBaseConfig() {
  }

  public static void configure(DimensionDI.Builder builder) {
    builder
        .provideNamed(JSplitPane.class,  "splitProfileListButtonsAndStatus", ServiceLocator.singleton(UIBaseConfig::createSplitPane))
        .provideNamed(JPanel.class,      "jPanelProfileStatus",              ServiceLocator.singleton(UIBaseConfig::createProfileStatusPanel))
        .provideNamed(JTabbedPane.class, "mainTabPane",                      ServiceLocator.singleton(UIBaseConfig::createMainTabPane))

        .provideNamed(JButton.class, "toolbarConfigButton",   ServiceLocator.singleton(UIBaseConfig::createToolbarConfigButton))
        .provideNamed(JButton.class, "toolbarTemplateButton", ServiceLocator.singleton(UIBaseConfig::createToolbarTemplateButton))

        .bindFactory(ModelModuleFactory.class,           ModelModule.class)
        .bindFactory(ManageModuleFactory.class,          ManageModule.class)
        .bindFactory(ConfigModuleFactory.class,          ConfigModule.class)
        .bindFactory(ChartsModuleFactory.class,          ChartsModule.class)

        .bindFactory(ManageModulePresenterFactory.class, ManagePresenter.class)

        .bindFactory(PreviewConfigModuleFactory.class, PreviewConfigModule.class)
        .bindFactory(PreviewChartsModuleFactory.class, PreviewChartsModule.class)

        .bindFactory(PlaygroundModuleFactory.class,  PlaygroundModule.class)
        .bindFactory(DesignModuleFactory.class,      DesignModule.class)
        .bindFactory(MetricColumnPanelFactory.class, MetricColumnPanel.class)

        .bindFactory(AdHocModelModuleFactory.class,  AdHocModelModule.class)
        .bindFactory(AdHocConfigModuleFactory.class, AdHocConfigModule.class)
        .bindFactory(AdHocChartsModuleFactory.class, AdHocChartsModule.class)
        .bindFactory(PreviewModuleFactory.class,     PreviewModule.class);
  }

  private static JSplitPane createSplitPane() {
    return GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 200);
  }

  private static JPanel createProfileStatusPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(LafColorGroup.CONFIG_PANEL, panel);
    return panel;
  }

  private static JTabbedPane createMainTabPane() {
    MainTab mainTab = new MainTab();
    mainTab.add(new JPanel(), MainTabPane.WORKSPACE.getName());
    mainTab.add(new JPanel(), MainTabPane.DASHBOARD.getName());
    mainTab.add(new JPanel(), MainTabPane.REPORT.getName());
    mainTab.add(new JPanel(), MainTabPane.ADHOC.getName());
    return mainTab;
  }

  private static JButton createToolbarConfigButton() {
    JButton jButton = new JButton(new ConfigurationIcon(18, 18));
    jButton.setActionCommand(CONFIGURATION.name());
    jButton.setMnemonic('C');
    jButton.setText("Configuration");
    jButton.setToolTipText("Application configuration, model and data loading pipelines");
    jButton.setBorder(GUIHelper.getBorderForButton(LaF.getColorBorder(LafColorGroup.BORDER)));
    return jButton;
  }

  private static JButton createToolbarTemplateButton() {
    JButton jButton = new JButton(new TemplateIcon(18, 18));
    jButton.setActionCommand(TEMPLATE.name());
    jButton.setMnemonic('T');
    jButton.setText("Templates");
    jButton.setToolTipText("List of templates");
    jButton.setBorder(GUIHelper.getBorderForButton(LaF.getColorBorder(LafColorGroup.BORDER)));
    return jButton;
  }
}