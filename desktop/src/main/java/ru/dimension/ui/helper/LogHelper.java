package ru.dimension.ui.helper;

import java.util.LinkedHashSet;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;

@Log4j2
public class LogHelper {
  public static String formatPattern = "dd-MM-yyyy HH:mm:ss";

  private LogHelper() {}

  public static void logMapSelected(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    log.info("################################");

    if (topMapSelected == null || topMapSelected.isEmpty()) {
      log.info("Map is empty or null");
      return;
    }

    for (Map.Entry<CProfile, LinkedHashSet<String>> entry : topMapSelected.entrySet()) {
      CProfile profile = entry.getKey();
      LinkedHashSet<String> filterSet = entry.getValue();

      String filters = String.join(", ", filterSet);
      log.info("CProfile: {} with filter: {}", profile.getColName(), filters);
    }

    log.info("################################");
  }
}
