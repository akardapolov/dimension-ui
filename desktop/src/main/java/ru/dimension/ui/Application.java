package ru.dimension.ui;

import lombok.extern.log4j.Log4j2;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.config.DIConfig;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LaFType;
import ru.dimension.ui.prompt.Internationalization;
import ru.dimension.ui.view.BaseFrame;

@Log4j2
public class Application {

  /**
   * Use LaF parameter in VM option to enable dark, light or default theme
   * <p>
   * Supported any of: "-DLaF=dark", "-DLaF=light", "-DLaF=default"
   * <p>
   * Example: java -DLaF=dark -Dfile.encoding=UTF8 -jar desktop-1.0-SNAPSHOT-jar-with-dependencies.jar
   */
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

    DIConfig.init();

    BaseFrame baseFrame = ServiceLocator.get(BaseFrame.class);
    baseFrame.setVisible(true);
  }
}