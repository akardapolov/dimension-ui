package ru.dimension.ui;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import lombok.extern.log4j.Log4j2;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.config.DIConfig;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LaFType;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.BaseFrame;
import ru.dimension.ui.view.LoadingDialog;

@Log4j2
public class Application {

  public static void main(String... args) {
    System.getProperties().setProperty("oracle.jdbc.J2EE13Compliant", "true");

    if ("ru".equals(System.getProperty("user.language"))) {
      Internationalization.setLanguage("ru");
    } else if ("en".equals(System.getProperty("user.language"))) {
      Internationalization.setLanguage("en");
    } else {
      Internationalization.setLanguage();
    }

    try {
      System.setProperty("flatlaf.uiScale", "1.1x");

      String lafVMOption = "LaF";
      if (LaFType.DEFAULT.name().equalsIgnoreCase(System.getProperty(lafVMOption))) {
        LaF.setLookAndFeel(LaFType.DEFAULT);
      } else if (LaFType.LIGHT.name().equalsIgnoreCase(System.getProperty(lafVMOption))) {
        LaF.setLookAndFeel(LaFType.LIGHT);
      } else if (LaFType.DARK.name().equalsIgnoreCase(System.getProperty(lafVMOption))) {
        LaF.setLookAndFeel(LaFType.DARK);
      } else {
        LaF.setLookAndFeel(LaFType.DEFAULT);
      }
    } catch (Exception e) {
      log.catching(e);
    }

    SwingUtilities.invokeLater(() -> {
      LoadingDialog loadingDialog = new LoadingDialog();
      loadingDialog.setMessage("Loading...");
      loadingDialog.setVisible(true);

      new SwingWorker<BaseFrame, String>() {

        @Override
        protected BaseFrame doInBackground() {
          DIConfig.init();

          try {
            GUIHelper.prewarmRegistry();
          } catch (Throwable ignore) {}

          BaseFrame baseFrame = ServiceLocator.get(BaseFrame.class);

          baseFrame.initUi();

          return baseFrame;
        }

        @Override
        protected void process(java.util.List<String> chunks) {
          if (!chunks.isEmpty()) {
            loadingDialog.setMessage(chunks.getLast());
          }
        }

        @Override
        protected void done() {
          try {
            loadingDialog.lockCancel("Opening UI...");

            BaseFrame baseFrame = get();
            baseFrame.setVisible(true);

          } catch (Exception e) {
            log.catching(e);
            System.exit(1);
          } finally {
            loadingDialog.dispose();
          }
        }
      }.execute();
    });
  }
}