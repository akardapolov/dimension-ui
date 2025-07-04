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

package ru.dimension.ui.collector.collect.json;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.cache.CacheProvider;
import com.jayway.jsonpath.spi.cache.LRUCache;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonPathParser {

  private static final ParseContext PARSER;

  static {
    Configuration conf = Configuration.defaultConfiguration()
        .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
        .addOptions(Option.ALWAYS_RETURN_LIST);
    CacheProvider.setCache(new LRUCache(128));
    PARSER = JsonPath.using(conf);
  }

  /**
   * use json path to parse content
   *
   * @param content  json content
   * @param jsonPath jsonPath
   * @return content [{'name': 'tom', 'speed': '433'},{'name': 'lili', 'speed': '543'}]
   */
  public static List<Object> parseContentWithJsonPath(String content,
                                                      String jsonPath) {
    if (content == null || jsonPath == null || "".equals(content) || "".equals(jsonPath)) {
      return Collections.emptyList();
    }
    return PARSER.parse(content).read(jsonPath);
  }

  /**
   * use json path to parse content
   *
   * @param content  json content
   * @param jsonPath jsonPath
   * @return content [{'name': 'tom', 'speed': '433'},{'name': 'lili', 'speed': '543'}]
   */
  public static <T> T parseContentWithJsonPath(String content,
                                               String jsonPath,
                                               TypeRef<T> typeRef) {
    if (content == null || jsonPath == null || "".equals(content) || "".equals(jsonPath)) {
      return null;
    }
    return PARSER.parse(content).read(jsonPath, typeRef);
  }

  public static Map<String, Object> parseResponseByJsonPath(String resp,
                                                            String jsonPath) {
    Map<String, Object> map = new HashMap<>();
    List<Object> results = JsonPathParser.parseContentWithJsonPath(resp, jsonPath);
    for (Object objectValue : results) {
      if (objectValue == null) {
        continue;
      }
      if (objectValue instanceof Map) {
        Map<String, Object> stringMap = (Map<String, Object>) objectValue;
        map.putAll(stringMap);
      } else if (objectValue instanceof String) {
        String stringValue = (String) objectValue;
        log.info("Not supported yet for data: " + stringValue);
      }
    }

    return map;
  }
}
