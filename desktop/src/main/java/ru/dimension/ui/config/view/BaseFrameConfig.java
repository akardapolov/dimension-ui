package ru.dimension.ui.config.view;

import dagger.Module;
import dagger.Provides;
import java.awt.BorderLayout;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.view.tab.MainTabPane;
import ru.dimension.ui.view.tab.MainTab;

@Module
public class BaseFrameConfig {

  @Provides
  @Singleton
  @Named("workspaceSplitPane")
  public JSplitPane getJSplitPane() {
    return GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 170);
  }

  @Provides
  @Singleton
  @Named("splitProfileListButtonsAndStatus")
  public JSplitPane getJSplitPaneProfileListButtonsAndStatus() {
    return GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 200);
  }

  @Provides
  @Singleton
  @Named("jPanelProfileStatus")
  public JPanel getJPanelProfileStatus() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(LafColorGroup.CONFIG_PANEL, panel);
    return panel;
  }

  @Provides
  @Singleton
  @Named("mainTabPane")
  public JTabbedPane getJTabbedPane() {
    MainTab mainTab = new MainTab();
    mainTab.add(new JPanel(), MainTabPane.WORKSPACE.getName());
    mainTab.add(new JPanel(), MainTabPane.DASHBOARD.getName());
    mainTab.add(new JPanel(), MainTabPane.REPORT.getName());
    mainTab.add(new JPanel(), MainTabPane.ADHOC.getName());
    return mainTab;
  }
}
