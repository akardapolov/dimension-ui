package ru.dimension.ui.prompt;

import java.util.Locale;
import java.util.ResourceBundle;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@UtilityClass
@Log4j2
public final class Internationalization {

  private static String language = "ru";

  public static void setLanguage() {
    language = Locale.getDefault().getLanguage();
  }

  public static void setLanguage(String languageLocale) {
    language = languageLocale;
  }

  private static Locale updateLocale(String lang) {
    Locale locale = new Locale(lang);
    Locale.setDefault(locale);
    return locale;
  }

  public static ResourceBundle getInternationalizationBundle() {
    return ResourceBundle.getBundle("ru.dimension.ui.prompt.Resource", updateLocale(language));
  }
}
