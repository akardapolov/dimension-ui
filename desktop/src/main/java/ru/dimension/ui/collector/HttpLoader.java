package ru.dimension.ui.collector;

import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ru.dimension.db.metadata.DataType;
import ru.dimension.db.model.profile.SProfile;
import ru.dimension.db.model.profile.cstype.CSType;
import ru.dimension.db.model.profile.cstype.CType;
import ru.dimension.db.model.profile.cstype.SType;
import ru.dimension.ui.collector.collect.CollectorConstants;
import ru.dimension.ui.collector.collect.common.HttpProtocol;
import ru.dimension.ui.collector.collect.prometheus.ExporterParser;
import ru.dimension.ui.model.info.ConnectionInfo;
import ru.dimension.ui.model.parse.ParseType;

public interface HttpLoader {

  default HttpProtocol getHttpProtocol(ConnectionInfo connectionInfo) {
    HttpProtocol httpProtocol = new HttpProtocol();
    try {
      httpProtocol.parseUrl(connectionInfo.getUrl());
      httpProtocol.setMethod(connectionInfo.getHttpMethod().name());
      httpProtocol.setAuthorization(null);
      httpProtocol.setParseType(
          ParseType.PROMETHEUS.equals(connectionInfo.getParseType()) ? CollectorConstants.PARSE_PROMETHEUS : CollectorConstants.PARSE_JSON_PATH);
      httpProtocol.setSuccessCodes(List.of("200"));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    return httpProtocol;
  }

  default void fillSProfileFromResponse(ExporterParser parser,
                                        String response,
                                        SProfile sProfile) {
    Map<String, CSType> csTypeMap = new LinkedHashMap<>();

    csTypeMap.put("ID", CSType.builder().isTimeStamp(true).sType(SType.RAW).cType(CType.LONG).dType(DataType.LONG)
        .build());

    Map<String, List<Entry<String, Double>>> metricKeyValue = parser.textToMetricKeyValue(response);

    metricKeyValue.forEach((metricName, metricFamily) -> metricFamily.forEach(metric -> {
      csTypeMap.put(metric.getKey(), CSType.builder().isTimeStamp(false)
          .sType(SType.RAW)
          .cType(CType.DOUBLE)
          .dType(DataType.DOUBLE).build());
    }));

    sProfile.setCsTypeMap(csTypeMap);
  }
}
