package ru.dimension.ui.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.core5.http.Method;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.table.BType;
import ru.dimension.db.model.profile.table.IType;
import ru.dimension.db.model.profile.table.TType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.collector.HttpLoader;
import ru.dimension.ui.collector.collect.HttpCollect;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.collector.http.HttpResponseFetcherImpl;
import ru.dimension.ui.common.AbstractDirectTest;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.model.parse.ParseType;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.impl.SqlQueryStateImpl;
import ru.dimension.ui.model.ProfileTaskQueryKey;

public class HttpCollectTest extends AbstractDirectTest implements HttpLoader {

  @BeforeAll
  public void init() {
    SProfile sProfile = new SProfile();
    sProfile.setTableName(tableNamePrometheus);
    sProfile.setTableType(TType.TIME_SERIES);
    sProfile.setIndexType(IType.GLOBAL);
    sProfile.setBackendType(BType.BERKLEYDB);
    sProfile.setCompression(true);

    // Fill SProfile
    fillSProfileFromResponse(parser, response, sProfile);

    // Load metadata for backend
    loadMetadataForTable(sProfile);

    // Create http server
    startHttpServerLocal();
  }

  @Test
  public void computeStackedTest() {
    ProfileTaskQueryKey profileTaskQueryKey = new ProfileTaskQueryKey(1, 1, 1);
    TaskInfo taskInfo = new TaskInfo();
    taskInfo.setId(1);
    taskInfo.setPullTimeout(3);
    ConnectionInfo connectionInfo = new ConnectionInfo();
    connectionInfo.setId(1);
    connectionInfo.setName("Http connection");
    connectionInfo.setUrl(httpServerLocal.getUrl());
    connectionInfo.setHttpMethod(Method.GET);
    connectionInfo.setParseType(ParseType.PROMETHEUS);
    QueryInfo queryInfo = new QueryInfo();
    TableInfo tableInfo = new TableInfo();
    tableInfo.setTableName(tableNamePrometheus);
    tableInfo.setCProfiles(cProfiles);
    SqlQueryState sqlQueryState = new SqlQueryStateImpl();
    sqlQueryState.initializeLastTimestamp(profileTaskQueryKey, System.currentTimeMillis());

    HttpResponseFetcher httpResponseFetcher = new HttpResponseFetcherImpl();

    HttpCollect httpCollect = new HttpCollect(profileTaskQueryKey,
                                              taskInfo,
                                              connectionInfo,
                                              queryInfo,
                                              tableInfo,
                                              sqlQueryState,
                                              httpResponseFetcher,
                                              dStore);

    httpCollect.collect();

    List<List<Object>> actualData = getRawDataAll(Long.MIN_VALUE, Long.MAX_VALUE);

    List<List<Object>> expectedData = new ArrayList<>();
    fillData(expectedData, parser.textToMetricKeyValue(response));

    expectedData.get(0).set(0, sqlQueryState.getLastTimestamp(profileTaskQueryKey));

    for (int i = 0; i < expectedData.size(); i++) {
      assertEquals(objectToString(expectedData.get(i).get(0)), objectToString(actualData.get(0).get(i)));
    }
  }

  public static String objectToString(Object obj) {
    if (obj == null) {
      return "null";
    } else if (obj instanceof Number) {
      return String.format("%s", obj);
    } else {
      return obj.toString();
    }
  }

  private void fillData(List<List<Object>> data,
                        Map<String, List<Entry<String, Double>>> metricKeyValue) {
    AtomicInteger atomicInteger = new AtomicInteger(0);

    data.add(atomicInteger.getAndIncrement(), addValue(System.currentTimeMillis()));

    metricKeyValue.values().stream()
        .flatMap(List::stream)
        .forEach(entry -> data.add(atomicInteger.getAndIncrement(), addValue(entry.getValue())));
  }

  private <T> ArrayList<T> addValue(T value) {
    ArrayList<T> list = new ArrayList<>(1);
    list.add(value);
    return list;
  }
}
