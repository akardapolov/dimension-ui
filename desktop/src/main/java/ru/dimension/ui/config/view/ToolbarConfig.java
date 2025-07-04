package ru.dimension.ui.config.view;

import static ru.dimension.ui.model.view.ToolbarAction.CONFIGURATION;
import static ru.dimension.ui.model.view.ToolbarAction.TEMPLATE;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JButton;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;

@Module
public class ToolbarConfig {

  private javax.swing.BorderFactory BorderFactory;

  @Provides
  @Singleton
  @Named("toolbarConfigButton")
  public JButton getConfigButton() {
    JButton jButton = new JButton();
    jButton.setActionCommand(CONFIGURATION.name());
    jButton.setMnemonic('C');
    jButton.setText("Configuration");
    jButton.setBorder(GUIHelper.getBorderForButton(LaF.getColorBorder(LafColorGroup.BORDER)));
    return jButton;
  }

  @Provides
  @Singleton
  @Named("toolbarTemplateButton")
  public JButton getTemplateButton() {
    JButton jButton = new JButton();
    jButton.setActionCommand(TEMPLATE.name());
    jButton.setMnemonic('T');
    jButton.setText("Templates");
    jButton.setBorder(GUIHelper.getBorderForButton(LaF.getColorBorder(LafColorGroup.BORDER)));
    return jButton;
  }
}
