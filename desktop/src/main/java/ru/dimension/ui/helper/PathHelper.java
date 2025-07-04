package ru.dimension.ui.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PathHelper {

  private static final String startsWithRegex = "(?<![_ \\-\\p{L}\\d\\[\\]\\(\\) ])";
  private static final String endsWithRegex = "(?=[\\.\\n])";
  private static final String containsRegex = "%s(?:[^\\/\\\\]*(?=((?i)%s(?!.))))";

  public static List<Path> searchRegularFilesStartsWith(final Path initialPath,
                                                        final String fileName,
                                                        final String fileExt) throws IOException {
    return searchRegularFiles(initialPath, startsWithRegex + fileName, fileExt);
  }

  public static List<Path> searchRegularFilesEndsWith(final Path initialPath,
                                                      final String fileName,
                                                      final String fileExt) throws IOException {
    return searchRegularFiles(initialPath, fileName + endsWithRegex, fileExt);
  }

  public static List<Path> searchRegularFilesAll(final Path initialPath) throws IOException {
    return searchRegularFiles(initialPath, "", "");
  }

  public static List<Path> searchRegularFiles(final Path initialPath,
                                              final String fileName,
                                              final String fileExt)
      throws IOException {
    final String regex = String.format(containsRegex, fileName, fileExt);
    final Pattern pattern = Pattern.compile(regex);
    try (Stream<Path> walk = Files.walk(initialPath.toRealPath())) {
      return walk.filter(path -> Files.isRegularFile(path) &&
              pattern.matcher(path.toString()).find())
          .collect(Collectors.toList());
    }
  }

  private PathHelper() {
  }
}
