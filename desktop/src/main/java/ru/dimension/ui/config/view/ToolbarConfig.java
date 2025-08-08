package ru.dimension.ui.config.view;

import static ru.dimension.ui.model.view.ToolbarAction.CONFIGURATION;
import static ru.dimension.ui.model.view.ToolbarAction.TEMPLATE;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
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
    ImageIcon configIcon = GUIHelper.loadIcon("/icons/config.png");

    JButton jButton = new JButton(configIcon);
    jButton.setActionCommand(CONFIGURATION.name());
    jButton.setMnemonic('C');
    jButton.setText("Configuration");
    jButton.setToolTipText("Application configuration, model and data loading pipelines");
    jButton.setBorder(GUIHelper.getBorderForButton(LaF.getColorBorder(LafColorGroup.BORDER)));
    return jButton;
  }

  @Provides
  @Singleton
  @Named("toolbarTemplateButton")
  public JButton getTemplateButton() {
    ImageIcon templateIcon = GUIHelper.loadIcon("/icons/template.png");

    JButton jButton = new JButton(templateIcon);
    jButton.setActionCommand(TEMPLATE.name());
    jButton.setMnemonic('T');
    jButton.setText("Templates");
    jButton.setToolTipText("List of templates");
    jButton.setBorder(GUIHelper.getBorderForButton(LaF.getColorBorder(LafColorGroup.BORDER)));
    return jButton;
  }
}
