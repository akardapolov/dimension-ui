package ru.dimension.ui.collector;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import ru.dimension.db.metadata.DataType;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.cstype.CSType;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.db.model.profile.cstype.SType;
import ru.dimension.ui.collector.collect.common.JmxProtocol;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.security.EncryptDecrypt;

public interface JmxLoader {

  String COLUMN_TIMESTAMP = "ID";

  String COLUMN_HEAP_USED = "heapUsed";
  String COLUMN_HEAP_COMMITTED = "heapCommitted";
  String COLUMN_HEAP_MAX = "heapMax";
  String COLUMN_NON_HEAP_USED = "nonHeapUsed";
  String COLUMN_NON_HEAP_COMMITTED = "nonHeapCommitted";
  String COLUMN_LOADED_CLASS_COUNT = "loadedClassCount";
  String COLUMN_TOTAL_LOADED_CLASS_COUNT = "totalLoadedClassCount";
  String COLUMN_UNLOADED_CLASS_COUNT = "unloadedClassCount";
  String COLUMN_GC_COUNT = "gcCount";
  String COLUMN_GC_TIME = "gcTime";

  List<String> JMX_COLUMNS = List.of(COLUMN_TIMESTAMP,
                                     COLUMN_HEAP_USED,
                                     COLUMN_HEAP_COMMITTED,
                                     COLUMN_HEAP_MAX,
                                     COLUMN_NON_HEAP_USED,
                                     COLUMN_NON_HEAP_COMMITTED,
                                     COLUMN_LOADED_CLASS_COUNT,
                                     COLUMN_TOTAL_LOADED_CLASS_COUNT,
                                     COLUMN_UNLOADED_CLASS_COUNT,
                                     COLUMN_GC_COUNT,
                                     COLUMN_GC_TIME);

  default JmxProtocol getJmxProtocol(ConnectionInfo connectionInfo,
                                     EncryptDecrypt encryptDecrypt) {
    String url = connectionInfo.getUrl() == null ? "" : connectionInfo.getUrl().trim();

    if (url.isEmpty()) {
      return JmxProtocol.builder()
          .local(true)
          .build();
    }

    String password = connectionInfo.getPassword();
    if (password != null && !password.isEmpty() && encryptDecrypt != null) {
      password = encryptDecrypt.decrypt(password);
    }

    if (url.startsWith("service:jmx")) {
      return JmxProtocol.builder()
          .local(false)
          .url(url)
          .username(connectionInfo.getUserName())
          .password(password)
          .build();
    }

    // IPv6 literals are not supported in the host:port form, use a full service:jmx url for them
    int portIndex = url.lastIndexOf(':');
    if (portIndex <= 0 || portIndex == url.length() - 1) {
      throw new IllegalArgumentException("Invalid JMX url, expected host:port or service:jmx url: " + url);
    }

    return JmxProtocol.builder()
        .local(false)
        .host(url.substring(0, portIndex))
        .port(Integer.parseInt(url.substring(portIndex + 1)))
        .username(connectionInfo.getUserName())
        .password(password)
        .build();
  }

  default JMXConnector connect(JmxProtocol protocol)
      throws IOException {
    JMXServiceURL serviceUrl = protocol.getUrl() != null && protocol.getUrl().startsWith("service:jmx")
        ? new JMXServiceURL(protocol.getUrl())
        : new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + protocol.getHost() + ":" + protocol.getPort() + "/jmxrmi");

    Map<String, Object> env = new HashMap<>();
    if (protocol.getUsername() != null && !protocol.getUsername().isEmpty()
        && protocol.getPassword() != null && !protocol.getPassword().isEmpty()) {
      env.put(JMXConnector.CREDENTIALS, new String[]{protocol.getUsername(), protocol.getPassword()});
    }

    return JMXConnectorFactory.connect(serviceUrl, env);
  }

  default void fillSProfileJmx(SProfile sProfile) {
    Map<String, CSType> csTypeMap = new LinkedHashMap<>();

    for (String column : JMX_COLUMNS) {
      csTypeMap.put(column, CSType.builder()
          .isTimeStamp(COLUMN_TIMESTAMP.equals(column))
          .sType(SType.RAW)
          .cType(CType.LONG)
          .dType(DataType.LONG)
          .build());
    }

    sProfile.setCsTypeMap(csTypeMap);
  }
}
