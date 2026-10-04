package ru.dimension.ui.collector.collect;

import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.collector.JmxLoader;
import ru.dimension.ui.collector.collect.common.JmxProtocol;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.security.EncryptDecrypt;
import ru.dimension.ui.state.SqlQueryState;

import static ru.dimension.ui.collector.JmxLoader.COLUMN_GC_COUNT;
import static ru.dimension.ui.collector.JmxLoader.COLUMN_GC_TIME;
import static ru.dimension.ui.collector.JmxLoader.COLUMN_HEAP_COMMITTED;
import static ru.dimension.ui.collector.JmxLoader.COLUMN_HEAP_MAX;
import static ru.dimension.ui.collector.JmxLoader.COLUMN_HEAP_USED;
import static ru.dimension.ui.collector.JmxLoader.COLUMN_LOADED_CLASS_COUNT;
import static ru.dimension.ui.collector.JmxLoader.COLUMN_NON_HEAP_COMMITTED;
import static ru.dimension.ui.collector.JmxLoader.COLUMN_NON_HEAP_USED;
import static ru.dimension.ui.collector.JmxLoader.COLUMN_TIMESTAMP;
import static ru.dimension.ui.collector.JmxLoader.COLUMN_TOTAL_LOADED_CLASS_COUNT;
import static ru.dimension.ui.collector.JmxLoader.COLUMN_UNLOADED_CLASS_COUNT;

@Log4j2
public class JmxCollect extends AbstractCollect implements JmxLoader {

  private final ProfileTaskQueryKey profileTaskQueryKey;
  private final TableInfo tableInfo;
  private final SqlQueryState sqlQueryState;
  private final DStore dStore;

  private final JmxProtocol jmxProtocol;

  private volatile JMXConnector connector;
  private volatile boolean closed;

  public JmxCollect(ProfileTaskQueryKey profileTaskQueryKey,
                    ConnectionInfo connectionInfo,
                    EncryptDecrypt encryptDecrypt,
                    TableInfo tableInfo,
                    SqlQueryState sqlQueryState,
                    DStore dStore) {
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.tableInfo = tableInfo;
    this.sqlQueryState = sqlQueryState;
    this.dStore = dStore;

    this.jmxProtocol = getJmxProtocol(connectionInfo, encryptDecrypt);
  }

  @Override
  public void collect() {
    long startTime = System.currentTimeMillis();

    try {
      dStore.putDataDirect(tableInfo.getTableName(), collectValues(startTime));
    } catch (Exception e) {
      log.catching(e);
      resetConnection();
    } finally {
      sqlQueryState.setLastTimestamp(profileTaskQueryKey, startTime);
    }
  }

  @Override
  public String getProtocol() {
    return CollectorConstants.PROTOCOL_JMX;
  }

  public void close() {
    closed = true;
    resetConnection();
  }

  private List<List<Object>> collectValues(long startTime)
      throws Exception {
    MBeanServerConnection server = getMBeanServerConnection();

    Map<String, Long> values = new LinkedHashMap<>();
    values.put(COLUMN_TIMESTAMP, startTime);

    MemoryMXBean memoryMXBean = JMX.newMXBeanProxy(server,
                                                   new ObjectName("java.lang:type=Memory"),
                                                   MemoryMXBean.class);
    MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
    MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
    values.put(COLUMN_HEAP_USED, heap.getUsed());
    values.put(COLUMN_HEAP_COMMITTED, heap.getCommitted());
    values.put(COLUMN_HEAP_MAX, heap.getMax());
    values.put(COLUMN_NON_HEAP_USED, nonHeap.getUsed());
    values.put(COLUMN_NON_HEAP_COMMITTED, nonHeap.getCommitted());

    ClassLoadingMXBean classLoadingMXBean = JMX.newMXBeanProxy(server,
                                                               new ObjectName("java.lang:type=ClassLoading"),
                                                               ClassLoadingMXBean.class);
    values.put(COLUMN_LOADED_CLASS_COUNT, (long) classLoadingMXBean.getLoadedClassCount());
    values.put(COLUMN_TOTAL_LOADED_CLASS_COUNT, classLoadingMXBean.getTotalLoadedClassCount());
    values.put(COLUMN_UNLOADED_CLASS_COUNT, classLoadingMXBean.getUnloadedClassCount());

    long gcCount = 0;
    long gcTime = 0;
    for (ObjectName gcName : server.queryNames(new ObjectName("java.lang:type=GarbageCollector,*"), null)) {
      Object count = server.getAttribute(gcName, "CollectionCount");
      Object time = server.getAttribute(gcName, "CollectionTime");
      if (count instanceof Long countValue) {
        gcCount += countValue;
      }
      if (time instanceof Long timeValue) {
        gcTime += timeValue;
      }
    }
    values.put(COLUMN_GC_COUNT, gcCount);
    values.put(COLUMN_GC_TIME, gcTime);

    List<List<Object>> data = new ArrayList<>(JMX_COLUMNS.size());
    for (String column : JMX_COLUMNS) {
      data.add(addValue(values.get(column)));
    }

    return data;
  }

  private void collectHistogram() {
    // TODO: implement live object histogram by class (jmap -histo style) via jcmd GC.class_histogram
    //  or JFR streaming (jdk.ObjectAllocationSample/jdk.ObjectCount); poll every 10-20 seconds,
    //  keep it off the 1-second JMX cadence because a histogram forces heavy work on the target JVM
  }

  private MBeanServerConnection getMBeanServerConnection()
      throws IOException {
    if (jmxProtocol.isLocal()) {
      return ManagementFactory.getPlatformMBeanServer();
    }

    JMXConnector current = connector;
    if (current == null) {
      synchronized (this) {
        current = connector;
        if (current == null) {
          if (closed) {
            throw new IllegalStateException("JMX collector is stopped for: " + tableInfo.getTableName());
          }
          current = connect(jmxProtocol);
          connector = current;
        }
      }
    }

    return current.getMBeanServerConnection();
  }

  private void resetConnection() {
    JMXConnector current;
    synchronized (this) {
      current = connector;
      connector = null;
    }

    if (current != null) {
      try {
        current.close();
      } catch (Exception ignored) {
      }
    }
  }

  private ArrayList<Object> addValue(Object value) {
    ArrayList<Object> list = new ArrayList<>(1);
    list.add(value);
    return list;
  }
}
