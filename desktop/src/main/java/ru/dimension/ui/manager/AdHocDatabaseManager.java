package ru.dimension.ui.manager;

import ru.dimension.db.core.DStore;
import ru.dimension.ui.model.info.ConnectionInfo;

public interface AdHocDatabaseManager {

  void createDataBase(ConnectionInfo connectionInfo);

  DStore getDataBase(ConnectionInfo connectionInfo);
}
