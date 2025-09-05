package ru.dimension.ui.helper;

import lombok.extern.log4j.Log4j2;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.model.AdHocKey;
import ru.dimension.ui.model.info.ConnectionInfo;

@Log4j2
public final class KeyHelper {
  private KeyHelper() {}

  public static AdHocKey getAdHocKey(ConnectionInfo connectionInfo, String tableName, CProfile cProfile) {
    return new AdHocKey(connectionInfo.getId(), tableName, cProfile.getColId());
  }

  public static String getGlobalKey(ConnectionInfo connectionInfo, String tableName) {
    return connectionInfo.getId() + "_" + tableName;
  }

  public static String getGlobalKey(int id, String tableName) {
    return id + "_" + tableName;
  }
}
