package ru.dimension.ui.view.structure.template;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.view.panel.template.TemplateCatalogPanel;
import ru.dimension.ui.view.panel.template.TemplateDetailsPanel;
import ru.dimension.ui.view.structure.TemplateView;

@Log4j2
@Singleton
public class TemplateViewImpl extends JDialog implements TemplateView {

  private final TemplateCatalogPanel catalogPanel;

  @Inject
  public TemplateViewImpl(@Named("templateCatalogPanel") TemplateCatalogPanel catalogPanel,
                          @Named("templateDetailsPanel") TemplateDetailsPanel detailsPanel) {
    this.catalogPanel = catalogPanel;

    JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, catalogPanel, detailsPanel);
    mainSplit.setDividerLocation(280);
    mainSplit.setBorder(null);

    setLayout(new BorderLayout());
    add(mainSplit, BorderLayout.CENTER);

    setTitle("Templates");
    setEscToClose();
    packTemplate(false);
  }

  @Override
  public void bindPresenter(TemplatePresenter templatePresenter) {
    addWindowListener(templatePresenter);
  }

  @Override
  public void showTemplate() {
    packTemplate(true);
  }

  @Override
  public void hideTemplate() {
    packTemplate(false);
  }

  private void packTemplate(boolean visible) {
    setModal(false);
    setResizable(true);
    pack();
    setSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width - 300,
                          Toolkit.getDefaultToolkit().getScreenSize().height - 100));
    setLocation(Toolkit.getDefaultToolkit().getScreenSize().width / 2 - getWidth() / 2,
                Toolkit.getDefaultToolkit().getScreenSize().height / 2 - getHeight() / 2);
    if (visible) {
      setVisible(true);
      requestActivation();
    } else {
      setVisible(false);
    }
  }

  private void requestActivation() {
    SwingUtilities.invokeLater(() -> {
      toFront();
      setAlwaysOnTop(true);
      setAlwaysOnTop(false);
      requestFocus();
      catalogPanel.requestSearchFocus();
    });
  }

  private void setEscToClose() {
    setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    final String actionKey = "TEMPLATE_DIALOG_CLOSE_ON_ESC";
    JRootPane root = getRootPane();
    root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionKey);
    root.getActionMap().put(actionKey, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TemplateViewImpl.this.dispatchEvent(new WindowEvent(TemplateViewImpl.this, WindowEvent.WINDOW_CLOSING));
      }
    });
  }
}