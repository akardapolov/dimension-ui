/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.dimension.ui.collector.collect.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpProtocol {

  private String host;
  private String port;
  private String url;
  private String timeout;
  private String ssl = "false";
  private String method;
  private Map<String, String> headers;
  private Map<String, String> params;
  private String payload;
  private Authorization authorization;
  private String parseType;
  private String parseScript;//jsonPath or xmlPath
  private String keyword;
  private List<String> successCodes;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Authorization {

    private String type;
    private String bearerTokenToken;
    private String basicAuthUsername;
    private String basicAuthPassword;
    private String digestAuthUsername;
    private String digestAuthPassword;
  }

  public void parseUrl(String urlString) throws URISyntaxException {
    URI uri = new URI(urlString);

    this.host = uri.getHost();
    this.port = uri.getPort() != -1 ? String.valueOf(uri.getPort()) : null;
    this.ssl = uri.getScheme().equals("https") ? "true" : "false";
    this.url = uri.getPath();

    String query = uri.getQuery();
    if (query != null) {
      parseQueryParams(query, this.params);
    }
  }

  private static void parseQueryParams(String query,
                                       Map<String, String> params) {
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      if (idx != -1 && idx < pair.length() - 1) {
        String key = pair.substring(0, idx);
        String value = pair.substring(idx + 1);
        params.put(key, value);
      } else {
        params.put(pair, null);
      }
    }
  }
}

