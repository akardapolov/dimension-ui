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

package ru.dimension.ui.collector.collect.utils;

import com.google.gson.JsonElement;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CollectUtil {

  private static final int DEFAULT_TIMEOUT = 60000;
  private static final int HEX_STR_WIDTH = 2;
  private static final String SMILING_PLACEHOLDER = "^_^";
  private static final String SMILING_PLACEHOLDER_REX = "\\^_\\^";
  private static final String SMILING_PLACEHOLDER_REGEX = "(\\^_\\^)(\\w|-|$|\\.)+(\\^_\\^)";
  private static final Pattern SMILING_PLACEHOLDER_REGEX_PATTERN = Pattern.compile(SMILING_PLACEHOLDER_REGEX);
  private static final String CRYING_PLACEHOLDER_REX = "\\^o\\^";
  private static final String CRYING_PLACEHOLDER_REGEX = "(\\^o\\^)(\\w|-|$|\\.)+(\\^o\\^)";
  private static final Pattern CRYING_PLACEHOLDER_REGEX_PATTERN = Pattern.compile(CRYING_PLACEHOLDER_REGEX);
  private static final List<String> UNIT_SYMBOLS = Arrays.asList("%", "G", "g", "M", "m", "K", "k", "B", "b");

  /**
   * count match keyword number
   *
   * @param content content
   * @param keyword keyword
   * @return match num
   */
  public static int countMatchKeyword(String content,
                                      String keyword) {
    if (content == null || "".equals(content) || keyword == null || "".equals(keyword.trim())) {
      return 0;
    }
    try {
      Pattern pattern = Pattern.compile(keyword);
      Matcher matcher = pattern.matcher(content);
      int count = 0;
      while (matcher.find()) {
        count++;
      }
      return count;
    } catch (Exception e) {
      return 0;
    }
  }

  public static DoubleAndUnit extractDoubleAndUnitFromStr(String str) {
    if (str == null || "".equals(str)) {
      return null;
    }
    DoubleAndUnit doubleAndUnit = new DoubleAndUnit();
    try {
      Double doubleValue = Double.parseDouble(str);
      doubleAndUnit.setValue(doubleValue);
      return doubleAndUnit;
    } catch (Exception e) {
      log.debug(e.getMessage());
    }
    // extract unit from str value, eg: 23.43GB, 33KB, 44.22G
    try {
      // B KB MB GB % ....
      for (String unitSymbol : UNIT_SYMBOLS) {
        int index = str.indexOf(unitSymbol);
        if (index == 0) {
          Double doubleValue = 0d;
          String unit = str.trim();
          doubleAndUnit.setValue(doubleValue);
          doubleAndUnit.setUnit(unit);
          return doubleAndUnit;
        }
        if (index > 0) {
          Double doubleValue = Double.parseDouble(str.substring(0, index));
          String unit = str.substring(index).trim();
          doubleAndUnit.setValue(doubleValue);
          doubleAndUnit.setUnit(unit);
          return doubleAndUnit;
        }
      }
    } catch (Exception e) {
      log.debug(e.getMessage());
    }
    return doubleAndUnit;
  }

  /**
   * double and unit
   */
  public static final class DoubleAndUnit {

    private Double value;
    private String unit;

    public Double getValue() {
      return value;
    }

    public void setValue(Double value) {
      this.value = value;
    }

    public String getUnit() {
      return unit;
    }

    public void setUnit(String unit) {
      this.unit = unit;
    }
  }

  /**
   * get timeout integer
   *
   * @param timeout timeout str
   * @return timeout
   */
  public static int getTimeout(String timeout) {
    return getTimeout(timeout, DEFAULT_TIMEOUT);
  }

  /**
   * get timeout integer or default value
   *
   * @param timeout        timeout str
   * @param defaultTimeout default timeout
   * @return timeout
   */
  public static int getTimeout(String timeout,
                               int defaultTimeout) {
    if (timeout == null || "".equals(timeout.trim())) {
      return defaultTimeout;
    }
    try {
      return Double.valueOf(timeout).intValue();
    } catch (Exception e) {
      return defaultTimeout;
    }
  }

  /**
   * is contains cryPlaceholder ^o^xxx^o^
   *
   * @param jsonElement json element
   * @return return true when contains
   */
  public static boolean containCryPlaceholder(JsonElement jsonElement) {
    String jsonStr = jsonElement.toString();
    return CRYING_PLACEHOLDER_REGEX_PATTERN.matcher(jsonStr).find();
  }

  public static boolean notContainCryPlaceholder(JsonElement jsonElement) {
    return !containCryPlaceholder(jsonElement);
  }

  /**
   * match existed cry placeholder fields ^o^field^o^
   *
   * @param jsonElement json element
   * @return match field str
   */
  public static Set<String> matchCryPlaceholderField(JsonElement jsonElement) {
    String jsonStr = jsonElement.toString();
    return CRYING_PLACEHOLDER_REGEX_PATTERN.matcher(jsonStr).results()
        .map(item -> item.group().replaceAll(CRYING_PLACEHOLDER_REX, ""))
        .collect(Collectors.toSet());
  }

  public static String replaceUriSpecialChar(String uri) {
    uri = uri.replaceAll(" ", "%20");
    // todo more special
    return uri;
  }

  /**
   * convert 16 hexString to byte[]
   *
   * @param hexString 16 hexString
   * @return byte[]
   */
  public static byte[] fromHexString(String hexString) {
    if (null == hexString || "".equals(hexString.trim())) {
      return null;
    }
    byte[] bytes = new byte[hexString.length() / HEX_STR_WIDTH];
    String hex;
    for (int i = 0; i < hexString.length() / HEX_STR_WIDTH; i++) {
      hex = hexString.substring(i * HEX_STR_WIDTH, i * HEX_STR_WIDTH + HEX_STR_WIDTH);
      bytes[i] = (byte) Integer.parseInt(hex, 16);
    }
    return bytes;
  }
}
