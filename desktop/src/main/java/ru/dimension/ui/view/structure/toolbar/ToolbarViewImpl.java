package ru.dimension.ui.view.structure.toolbar;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.util.SystemInfo;
import dagger.Lazy;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.view.structure.ToolbarView;
import ru.dimension.ui.laf.LookAndFeelsComboBox;
import ru.dimension.ui.model.view.ToolbarButtonState;

@Log4j2
@Singleton
public class ToolbarViewImpl extends JToolBar implements ToolbarView {

  private final Lazy<ToolbarPresenter> toolbarPresenter;
  private final JButton toolbarConfigButton;
  private final JButton toolbarTemplateButton;
  private final LookAndFeelsComboBox lookAndFeelComboBox;


  @Inject
  public ToolbarViewImpl(@Named("toolbarPresenter") Lazy<ToolbarPresenter> toolbarPresenter,
                         @Named("toolbarConfigButton") JButton toolbarConfigButton,
                         @Named("toolbarTemplateButton") JButton toolbarTemplateButton) {
    this.toolbarPresenter = toolbarPresenter;
    this.toolbarConfigButton = toolbarConfigButton;
    this.toolbarTemplateButton = toolbarTemplateButton;

    this.lookAndFeelComboBox = new LookAndFeelsComboBox();
    this.lookAndFeelComboBox.setEnabled(false);

    this.setBorder(new EtchedBorder());

    this.add(Box.createRigidArea(new Dimension(10, 0)));
    this.add(this.toolbarConfigButton);
    this.add(Box.createRigidArea(new Dimension(10, 0)));
    this.add(this.toolbarTemplateButton);

    JPanel lookAndFeelPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    DefaultComboBoxModel<LookAndFeelInfo> lafModel = new DefaultComboBoxModel<>();
    lafModel.addElement(new LookAndFeelInfo("Light", FlatLightLaf.class.getName()));
    lafModel.addElement(new LookAndFeelInfo("Darcula", FlatDarculaLaf.class.getName()));

    UIManager.LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
    for (UIManager.LookAndFeelInfo lookAndFeel : lookAndFeels) {
      String name = lookAndFeel.getName();
      String className = lookAndFeel.getClassName();
      if (className.equals("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel") ||
          className.equals("com.sun.java.swing.plaf.motif.MotifLookAndFeel")) {
        continue;
      }

      if ((SystemInfo.isWindows && className.equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel")) ||
          (SystemInfo.isMacOS && className.equals("com.apple.laf.AquaLookAndFeel")) ||
          (SystemInfo.isLinux && className.equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"))) {
        name += " ";
      } else if (className.equals(MetalLookAndFeel.class.getName())) {
        name += " ";
      } else if (className.equals(NimbusLookAndFeel.class.getName())) {
        name += " ";
      }

      lafModel.addElement(new LookAndFeelInfo(name, className));
    }

    lookAndFeelComboBox.setModel(lafModel);

    lookAndFeelComboBox.addActionListener(e -> lookAndFeelChanged());
    lookAndFeelPane.add(lookAndFeelComboBox, "cell 0 1");
    this.add(lookAndFeelPane);
  }

  @Override
  public void bindPresenter() {
    this.toolbarConfigButton.addActionListener(this.toolbarPresenter.get());
    this.toolbarTemplateButton.addActionListener(this.toolbarPresenter.get());
  }

  @Override
  public void setProfileButtonState(ToolbarButtonState toolbarButtonState) {
    this.toolbarConfigButton.setEnabled(toolbarButtonState == ToolbarButtonState.ENABLE);
    this.toolbarTemplateButton.setEnabled(toolbarButtonState == ToolbarButtonState.ENABLE);
  }

  private void lookAndFeelChanged() {
    String lafClassName = lookAndFeelComboBox.getSelectedLookAndFeel();
    if (lafClassName == null) {
      return;
    }

    if (lafClassName.equals(UIManager.getLookAndFeel().getClass().getName())) {
      return;
    }

    EventQueue.invokeLater(() -> {
      try {
        FlatAnimatedLafChange.showSnapshot();

        // change look and feel
        UIManager.setLookAndFeel(lafClassName);

        // clear custom default font when switching to non-FlatLaf LaF
        if (!(UIManager.getLookAndFeel() instanceof FlatLaf)) {
          UIManager.put("defaultFont", null);
        }

        // update all components
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();


      } catch (Exception ex) {

      }
    });
  }

}
