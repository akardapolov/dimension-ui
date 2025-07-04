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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonUtil {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");

  private static final Pattern PHONE_PATTERN = Pattern.compile("^(((13[0-9])|(14[0-9])|(15[0-9])|(16[0-9])|(19[0-9])|(18[0-9])|(17[0-9]))+\\d{8})?$");

  private static final int PHONE_LENGTH = 11;

  public static boolean isNullOrBlank(String param) {
    return param == null || param.trim().length() == 0;
  }

  public static Integer parseStrInteger(final String str) {
    if (isNullOrBlank(str)) {
      return null;
    }

    try {
      return Integer.parseInt(str);
    } catch (Exception e) {
      log.debug(e.getMessage(), e);
      return null;
    }
  }

  public static Double parseStrDouble(final String str) {
    if (isNullOrBlank(str)) {
      return null;
    }

    try {
      return Double.parseDouble(str);
    } catch (Exception e) {
      log.debug(e.getMessage(), e);
      return null;
    }
  }

  public static int parseTimeStrToSecond(final String str) {
    if (isNullOrBlank(str)) {
      return -1;
    }

    try {
      return LocalTime.parse(str).toSecondOfDay();
    } catch (Exception e) {
      log.debug(e.getMessage(), e);
      return -1;
    }
  }

  public static String parseDoubleStr(String str,
                                      String unit) {
    if (isNullOrBlank(str)) {
      return null;
    }

    try {
      if (unit != null && str.endsWith(unit)) {
        str = str.substring(0, str.length() - unit.length());
      }
      BigDecimal bigDecimal = new BigDecimal(str);
      return bigDecimal.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    } catch (Exception e) {
      log.debug(e.getMessage(), e);
      return null;
    }
  }

  public static boolean validateEmail(final String email) {
    if (isNullOrBlank(email)) {
      return false;
    }

    Matcher m = EMAIL_PATTERN.matcher(email);
    return m.find();
  }

  public static boolean validatePhoneNum(final String phoneNum) {
    if (isNullOrBlank(phoneNum) || phoneNum.length() != PHONE_LENGTH) {
      return false;
    }

    Matcher m = PHONE_PATTERN.matcher(phoneNum);
    return m.find();
  }

  public static String getMessageFromThrowable(Throwable throwable) {
    if (throwable == null) {
      return "throwable is null, unknown error.";
    }
    String message = null;
    Throwable cause = throwable.getCause();
    if (cause != null) {
      message = cause.getMessage();
    }
    if (message == null || "".equals(message)) {
      message = throwable.getMessage();
    }
    if (message == null || "".equals(message)) {
      message = throwable.getLocalizedMessage();
    }
    if (message == null || "".equals(message)) {
      message = throwable.toString();
    }
    if (message == null || "".equals(message)) {
      message = "unknown error.";
    }
    return message;
  }

  public static String removeBlankLine(String value) {
    if (value == null) {
      return null;
    }
    return value.replaceAll("(?m)^\\s*$(\\n|\\r\\n)", "");
  }

  public static String getLangMappingValueFromI18nMap(String lang,
                                                      Map<String, String> i18nMap) {
    if (i18nMap == null || i18nMap.isEmpty()) {
      return null;
    }
    return Optional.ofNullable(i18nMap.get(lang))
        .orElse(i18nMap.values().stream()
                    .findFirst().orElse(null));
  }
}
