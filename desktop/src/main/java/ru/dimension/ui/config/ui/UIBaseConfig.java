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
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.view.tab.MainTabPane;
import ru.dimension.ui.view.tab.MainTab;

public final class UIBaseConfig {

  private UIBaseConfig() {
  }

  public static void configure(DimensionDI.Builder builder) {
    builder
        // BaseFrameConfig
        .provideNamed(JSplitPane.class, "splitProfileListButtonsAndStatus", ServiceLocator.singleton(
            () -> GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 200)
        ))
        .provideNamed(JPanel.class, "jPanelProfileStatus", ServiceLocator.singleton(() -> {
          JPanel panel = new JPanel(new BorderLayout());
          panel.setBorder(new EtchedBorder());
          LaF.setBackgroundConfigPanel(LafColorGroup.CONFIG_PANEL, panel);
          return panel;
        }))
        .provideNamed(JTabbedPane.class, "mainTabPane", ServiceLocator.singleton(() -> {
          MainTab mainTab = new MainTab();
          mainTab.add(new JPanel(), MainTabPane.WORKSPACE.getName());
          mainTab.add(new JPanel(), MainTabPane.DASHBOARD.getName());
          mainTab.add(new JPanel(), MainTabPane.REPORT.getName());
          mainTab.add(new JPanel(), MainTabPane.ADHOC.getName());
          return mainTab;
        }))

        // ToolbarConfig
        .provideNamed(JButton.class, "toolbarConfigButton", ServiceLocator.singleton(() -> {
          JButton jButton = new JButton(ru.dimension.ui.helper.GUIHelper.loadIcon("/icons/config.png"));
          jButton.setActionCommand(CONFIGURATION.name());
          jButton.setMnemonic('C');
          jButton.setText("Configuration");
          jButton.setToolTipText("Application configuration, model and data loading pipelines");
          jButton.setBorder(ru.dimension.ui.helper.GUIHelper.getBorderForButton(LaF.getColorBorder(LafColorGroup.BORDER)));
          return jButton;
        }))
        .provideNamed(JButton.class, "toolbarTemplateButton", ServiceLocator.singleton(() -> {
          JButton jButton = new JButton(ru.dimension.ui.helper.GUIHelper.loadIcon("/icons/template.png"));
          jButton.setActionCommand(TEMPLATE.name());
          jButton.setMnemonic('T');
          jButton.setText("Templates");
          jButton.setToolTipText("List of templates");
          jButton.setBorder(ru.dimension.ui.helper.GUIHelper.getBorderForButton(LaF.getColorBorder(LafColorGroup.BORDER)));
          return jButton;
        }));
  }
}