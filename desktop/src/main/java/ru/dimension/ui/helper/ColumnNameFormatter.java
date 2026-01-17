package ru.dimension.ui.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class ColumnNameFormatter {

  private static final Pattern PROMETHEUS_PATTERN =
      Pattern.compile("^([a-zA-Z_][a-zA-Z0-9_]*)\\{(.+)}$");

  private static final int DEFAULT_MAX_LENGTH = 30;
  private static final int HASH_LENGTH = 4;
  private static final int LABELS_TO_SHOW = 2; // показываем последние N labels

  private ColumnNameFormatter() {}

  @AllArgsConstructor
  @Getter
  public static class FormattedName {
    private final String shortName;  // для отображения в title
    private final String fullName;   // для tooltip
  }

  public static FormattedName format(String columnName) {
    return format(columnName, DEFAULT_MAX_LENGTH);
  }

  public static FormattedName format(String columnName, int maxLength) {
    if (columnName == null || columnName.isEmpty()) {
      return new FormattedName("", "");
    }

    String fullName = columnName.trim();

    // Если уже короткое — оставляем как есть
    if (fullName.length() <= maxLength) {
      return new FormattedName(fullName, fullName);
    }

    // Пробуем парсить как Prometheus-метрику
    Matcher matcher = PROMETHEUS_PATTERN.matcher(fullName);
    if (matcher.matches()) {
      String shortName = formatPrometheusMetric(
          matcher.group(1),
          matcher.group(2),
          fullName,
          maxLength
      );
      return new FormattedName(shortName, fullName);
    }

    // Обычная строка — сокращаем с хешем для уникальности
    String shortName = truncateWithHash(fullName, maxLength);
    return new FormattedName(shortName, fullName);
  }

  /**
   * Форматирует Prometheus-метрику вида name{label1="val1",label2="val2",...}
   * Результат: short_name{last_labels}#hash
   */
  private static String formatPrometheusMetric(String baseName,
                                               String labelsString,
                                               String fullMetric,
                                               int maxLength) {
    String hash = getShortHash(fullMetric);

    // Парсим labels и берём последние N
    String[] labelPairs = labelsString.split(",");
    StringBuilder significantLabels = new StringBuilder();

    int startIdx = Math.max(0, labelPairs.length - LABELS_TO_SHOW);
    for (int i = startIdx; i < labelPairs.length; i++) {
      if (significantLabels.length() > 0) {
        significantLabels.append(",");
      }
      // Извлекаем только значение (без кавычек) для краткости
      String labelValue = extractLabelValue(labelPairs[i]);
      significantLabels.append(truncateValue(labelValue, 12));
    }

    // Формируем: short_base{labels}#hash
    // Резервируем место: {}# + hash(4) + labels
    int reservedLen = 3 + HASH_LENGTH + Math.min(significantLabels.length(), 25);
    int baseMaxLen = maxLength - reservedLen;

    String shortBase;
    if (baseName.length() > baseMaxLen && baseMaxLen > 3) {
      shortBase = abbreviateSnakeCase(baseName, baseMaxLen);
    } else if (baseMaxLen > 0) {
      shortBase = baseName.substring(0, Math.min(baseName.length(), baseMaxLen));
    } else {
      shortBase = "";
    }

    String result = shortBase + "{" + significantLabels + "}#" + hash;

    // Финальная проверка длины
    if (result.length() > maxLength) {
      return truncateWithHash(fullMetric, maxLength);
    }

    return result;
  }

  /**
   * Извлекает значение из label пары вида: key="value" или key='value'
   */
  private static String extractLabelValue(String labelPair) {
    int eqIdx = labelPair.indexOf('=');
    if (eqIdx < 0) {
      return labelPair.trim();
    }
    String value = labelPair.substring(eqIdx + 1).trim();
    // Убираем кавычки
    if ((value.startsWith("\"") && value.endsWith("\"")) ||
        (value.startsWith("'") && value.endsWith("'"))) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

  /**
   * Сокращает snake_case имя, оставляя первые буквы каждого слова
   * erlang_vm_allocators -> erl_vm_alloc
   */
  private static String abbreviateSnakeCase(String name, int maxLen) {
    if (name.length() <= maxLen) {
      return name;
    }

    String[] parts = name.split("_");
    if (parts.length == 1) {
      return name.substring(0, Math.min(name.length(), maxLen - 2)) + "..";
    }

    StringBuilder sb = new StringBuilder();
    int targetPartLen = Math.max(3, (maxLen - parts.length) / parts.length);

    for (int i = 0; i < parts.length; i++) {
      if (i > 0) sb.append("_");
      String part = parts[i];
      if (part.length() > targetPartLen) {
        sb.append(part.substring(0, targetPartLen));
      } else {
        sb.append(part);
      }

      // Проверяем не превысили ли лимит
      if (sb.length() >= maxLen - 2) {
        break;
      }
    }

    String result = sb.toString();
    if (result.length() > maxLen - 2) {
      result = result.substring(0, maxLen - 2);
    }
    return result + "..";
  }

  private static String truncateValue(String value, int maxLen) {
    if (value == null) return "";
    if (value.length() <= maxLen) return value;
    return value.substring(0, maxLen - 2) + "..";
  }

  private static String truncateWithHash(String text, int maxLength) {
    String hash = getShortHash(text);
    int prefixLen = maxLength - HASH_LENGTH - 3; // 3 для "..#"

    if (prefixLen <= 0) {
      return "#" + hash;
    }

    String prefix = text.substring(0, Math.min(prefixLen, text.length()));
    return prefix + "..#" + hash;
  }

  /**
   * Возвращает 4-символьный hex хеш для уникальности
   */
  private static String getShortHash(String text) {
    int hash = text.hashCode();
    return String.format("%04x", Math.abs(hash) & 0xFFFF);
  }
}