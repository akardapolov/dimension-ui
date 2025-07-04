package ru.dimension.ui.collector;

import java.sql.Connection;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;

public interface Collector {

  void fillMetadataJdbc(ProfileTaskQueryKey profileTaskQueryKey,
                        QueryInfo queryInfo,
                        TableInfo tableInfo,
                        Connection connection);

  void fillMetadataHttp(ConnectionInfo connectionInfo,
                        QueryInfo queryInfo,
                        TableInfo tableInfo);

  void start(ProfileInfo profileInfo);

  void stop(ProfileInfo profileInfo);
}
