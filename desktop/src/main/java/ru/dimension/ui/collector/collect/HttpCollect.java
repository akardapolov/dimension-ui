package ru.dimension.ui.collector.collect;

import static ru.dimension.ui.collector.collect.CollectorConstants.PARSE_JSON_PATH;
import static ru.dimension.ui.collector.collect.CollectorConstants.PARSE_PROMETHEUS;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.DigestScheme;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.protocol.HttpContext;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.EnumByteExceedException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.ui.collector.HttpLoader;
import ru.dimension.ui.collector.collect.common.HttpProtocol;
import ru.dimension.ui.collector.collect.prometheus.ExporterParser;
import ru.dimension.ui.collector.collect.utils.CollectUtil;
import ru.dimension.ui.collector.collect.utils.CommonUtil;
import ru.dimension.ui.collector.http.HttpResponseFetcher;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.model.info.TableInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class HttpCollect extends AbstractCollect implements HttpLoader {
  static String RIGHT_DASH = "/";

  static Set<Integer> defaultSuccessStatusCodes = Stream.of(HttpStatus.SC_OK,
                                                            HttpStatus.SC_CREATED,
                                                            HttpStatus.SC_ACCEPTED,
                                                            HttpStatus.SC_MULTIPLE_CHOICES,
                                                            HttpStatus.SC_MOVED_PERMANENTLY,
                                                            HttpStatus.SC_MOVED_TEMPORARILY)
      .collect(Collectors.toSet());

  private final HttpProtocol httpProtocol;

  private final ExporterParser parser;

  private final ProfileTaskQueryKey profileTaskQueryKey;
  private final TaskInfo taskInfo;
  private final ConnectionInfo connectionInfo;
  private final QueryInfo queryInfo;
  private final TableInfo tableInfo;
  private final SqlQueryState sqlQueryState;
  private final HttpResponseFetcher httpResponseFetcher;
  private final DStore dStore;

  public HttpCollect(ProfileTaskQueryKey profileTaskQueryKey,
                     TaskInfo taskInfo,
                     ConnectionInfo connectionInfo,
                     QueryInfo queryInfo,
                     TableInfo tableInfo,
                     SqlQueryState sqlQueryState,
                     HttpResponseFetcher httpResponseFetcher,
                     DStore dStore) {
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.taskInfo = taskInfo;
    this.connectionInfo = connectionInfo;
    this.queryInfo = queryInfo;
    this.tableInfo = tableInfo;
    this.sqlQueryState = sqlQueryState;
    this.httpResponseFetcher = httpResponseFetcher;
    this.dStore = dStore;

    this.parser = new ExporterParser();

    this.httpProtocol = getHttpProtocol(connectionInfo);
  }

  @Override
  public void collect() {
    long startTime = System.currentTimeMillis();

    try {
      validateParams(httpProtocol);
    } catch (Exception e) {
      log.catching(e);
      return;
    }

    String resp;
    try {
      resp = httpResponseFetcher.fetchResponse(httpProtocol);
      if (resp == null || "".equals(resp)) {
        log.info("http response entity is empty");
        return;
      }
    } catch (Exception e) {
      sqlQueryState.setLastTimestamp(profileTaskQueryKey, startTime);

      String errorMsg = CommonUtil.getMessageFromThrowable(e);
      log.error(errorMsg, e);
      return;
    }

    Long responseTime = System.currentTimeMillis() - startTime;
    String parseType = httpProtocol.getParseType();
    try {
      if (PARSE_PROMETHEUS.equals(parseType)) {
        parseResponseByPrometheus(resp, startTime);
      } else if (PARSE_JSON_PATH.equals(parseType)) {
        // TODO
        parseResponseByJsonPath(resp, httpProtocol, responseTime);
      } else {
        // TODO
        parseResponseByDefault(resp, httpProtocol, responseTime);
      }
    } catch (Exception e) {
      sqlQueryState.setLastTimestamp(profileTaskQueryKey, startTime);

      log.info("parse error: {}.", e.getMessage(), e);
    }

    sqlQueryState.setLastTimestamp(profileTaskQueryKey, startTime);
  }

  private void validateParams(HttpProtocol httpProtocol) throws Exception {
    if (httpProtocol == null) {
      throw new Exception("Http/Https collect must has http params");
    }
    if (httpProtocol.getUrl() == null
        || "".equals(httpProtocol.getUrl())
        || !httpProtocol.getUrl().startsWith(RIGHT_DASH)) {
      httpProtocol.setUrl(httpProtocol.getUrl() == null ? RIGHT_DASH : RIGHT_DASH + httpProtocol.getUrl().trim());
    }

    if (httpProtocol.getSuccessCodes().isEmpty()) {
      httpProtocol.setSuccessCodes(List.of("200"));
    }
  }

  private boolean checkSuccessInvoke(HttpProtocol httpProtocol,
                                     int statusCode) {
    List<String> successCodes = httpProtocol.getSuccessCodes();
    Set<Integer> successCodeSet = successCodes != null ? successCodes.stream().map(code -> {
      try {
        return Integer.valueOf(code);
      } catch (Exception ignored) {
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toSet()) : defaultSuccessStatusCodes;
    if (successCodeSet.isEmpty()) {
      successCodeSet = defaultSuccessStatusCodes;
    }
    return successCodeSet.contains(statusCode);
  }

  @Override
  public String getProtocol() {
    return CollectorConstants.PROTOCOL_HTTP;
  }

  public HttpContext createHttpContext(HttpProtocol httpProtocol) {
    HttpProtocol.Authorization auth = httpProtocol.getAuthorization();
    if (auth != null && CollectorConstants.DIGEST_AUTH.equals(auth.getType())) {
      HttpClientContext clientContext = new HttpClientContext();
      if (CommonUtil.isNullOrBlank(auth.getDigestAuthUsername())
          && CommonUtil.isNullOrBlank(auth.getDigestAuthPassword())) {
        UsernamePasswordCredentials credentials
            = new UsernamePasswordCredentials(auth.getDigestAuthUsername(), auth.getDigestAuthPassword().toCharArray());

        // Create AuthScope
        int portNumber;
        try {
          portNumber = Integer.parseInt(httpProtocol.getPort());
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Port must be a valid number", e);
        }

        AuthScope authScope = new AuthScope(httpProtocol.getHost(), portNumber);
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(authScope, credentials);

        AuthCache authCache = new BasicAuthCache();
        authCache.put(new HttpHost(httpProtocol.getHost(), Integer.parseInt(httpProtocol.getPort())), new DigestScheme());
        clientContext.setCredentialsProvider(provider);
        clientContext.setAuthCache(authCache);
        return clientContext;
      }
    }
    return null;
  }

  private void parseResponseByDefault(String resp,
                                      HttpProtocol http,
                                      Long responseTime) {
    JsonElement element = JsonParser.parseString(resp);
    int keywordNum = CollectUtil.countMatchKeyword(resp, http.getKeyword());
    if (element.isJsonArray()) {
      JsonArray array = element.getAsJsonArray();
      for (JsonElement jsonElement : array) {
        //getValueFromJson(aliasFields, builder, responseTime, jsonElement, keywordNum);
      }
    } else {
      //getValueFromJson(aliasFields, builder, responseTime, element, keywordNum);
    }
  }

  private void parseResponseByPrometheus(String resp,
                                         Long startTime)
      throws EnumByteExceedException, SqlColMetadataException {

    Map<String, List<Entry<String, Double>>> metricKeyValue = parser.textToMetricKeyValue(resp);

    AtomicInteger atomicInteger = new AtomicInteger(0);

    List<List<Object>> data = new ArrayList<>();
    data.add(atomicInteger.getAndIncrement(), addValue(startTime));

    metricKeyValue.values().stream()
        .flatMap(List::stream)
        .forEach(entry -> data.add(atomicInteger.getAndIncrement(), addValue(entry.getValue())));

    dStore.putDataDirect(tableInfo.getTableName(), data);
  }

  private <T> ArrayList<T> addValue(T value) {
    ArrayList<T> list = new ArrayList<>(1);
    list.add(value);
    return list;
  }

  public static void parseAndPrint(String json) {
    JsonElement element = JsonParser.parseString(json);

    if (element.isJsonObject()) {
      JsonObject obj = element.getAsJsonObject();

      obj.entrySet().forEach(entry -> {
        String key = entry.getKey();
        JsonElement valueElement = entry.getValue();

        if (valueElement.isJsonPrimitive()) {
          JsonElement primitive = valueElement.getAsJsonPrimitive();

          // Check if the JsonElement is of type long, double, or String and handle it
          if (primitive.getAsJsonPrimitive().isNumber()) {
            // Further check if it's a Long or Double since JSON numbers could be either
            Number num = primitive.getAsNumber();
            if (num.toString().contains(".")) {
              log.info("{}: {} (Double)", key, num.doubleValue());
            } else {
              log.info("{}: {} (Long)", key, num.longValue());
            }
          } else if (primitive.getAsJsonPrimitive().isString()) {
            log.info("{}: {} (String)", key, primitive.getAsString());
          }
        }
      });
    }
  }

  private void parseResponseByJsonPath(String resp,
                                       HttpProtocol http,
                                       Long responseTime) {
  }
}
