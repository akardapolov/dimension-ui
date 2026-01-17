package ru.dimension.ui.helper;

import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class FileNameSanitizer {

  // Символы, недопустимые в Windows: \ / : * ? " < > |
  // Также исключаем { } для надёжности
  private static final Pattern INVALID_CHARS = Pattern.compile("[\\\\/:*?\"<>|{}]");

  // Максимальная длина имени файла (без расширения) для безопасности
  private static final int MAX_FILENAME_LENGTH = 200;

  private FileNameSanitizer() {}

  public static String sanitize(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return "unnamed";
    }

    String sanitized = fileName.trim();

    // {key="value",key2="value2"} -> _key_value_key2_value2
    sanitized = sanitized
        .replace("{", "_")
        .replace("}", "")
        .replace("=\"", "_")
        .replace("\",", "_")
        .replace("\"", "")
        .replace("\\", "_")  // backslash в значениях типа text\plain
        .replace(";", "_")
        .replace("=", "_");

    // Заменяем оставшиеся недопустимые символы
    sanitized = INVALID_CHARS.matcher(sanitized).replaceAll("_");

    // Убираем множественные подчёркивания подряд
    sanitized = sanitized.replaceAll("_+", "_");

    // Убираем подчёркивания в начале и конце
    sanitized = sanitized.replaceAll("^_+|_+$", "");

    // Приводим к нижнему регистру и заменяем пробелы
    sanitized = sanitized.toLowerCase().replace(" ", "_");

    // Обрезаем до максимальной длины
    if (sanitized.length() > MAX_FILENAME_LENGTH) {
      // Добавляем хеш для уникальности при обрезке
      String hash = String.format("%08x", fileName.hashCode());
      int prefixLen = MAX_FILENAME_LENGTH - hash.length() - 1;
      sanitized = sanitized.substring(0, prefixLen) + "_" + hash;
    }

    // Если после всех преобразований пусто - генерируем имя из хеша
    if (sanitized.isBlank()) {
      sanitized = "metric_" + String.format("%08x", fileName.hashCode());
    }

    return sanitized;
  }

  public static String sanitizePath(String filePath) {
    if (filePath == null || filePath.isBlank()) {
      return filePath;
    }

    int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
    int lastDot = filePath.lastIndexOf('.');

    if (lastSeparator < 0) {
      // Нет директории, только имя файла
      if (lastDot > 0) {
        String name = filePath.substring(0, lastDot);
        String ext = filePath.substring(lastDot);
        return sanitize(name) + ext;
      }
      return sanitize(filePath);
    }

    String dir = filePath.substring(0, lastSeparator + 1);
    String fileNameWithExt = filePath.substring(lastSeparator + 1);

    if (lastDot > lastSeparator) {
      String name = fileNameWithExt.substring(0, fileNameWithExt.lastIndexOf('.'));
      String ext = fileNameWithExt.substring(fileNameWithExt.lastIndexOf('.'));
      return dir + sanitize(name) + ext;
    }

    return dir + sanitize(fileNameWithExt);
  }
}