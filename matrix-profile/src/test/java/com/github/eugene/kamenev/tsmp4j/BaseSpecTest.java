package com.github.eugene.kamenev.tsmp4j;

import com.github.eugene.kamenev.tsmp4j.algo.mp.MatrixProfile;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BaseSpecTest {

  public static final List<ToyData> data;

  static {
    try {
      data = loadData("mp_toy_data.csv", rows -> Arrays.stream(rows)
          .map(s -> new ToyData(Double.parseDouble(s[0]),
                                Double.parseDouble(s[1]),
                                Double.parseDouble(s[2])))
          .collect(Collectors.toList()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  protected static String getTestData(String fileName) throws IOException {
    return Files.readString(Paths.get("src","test", "resources", fileName));
  }

  protected static <T> T loadData(String file,
                                  Function<String[][], T> transform) throws IOException, URISyntaxException {

    String dataString = getTestData(file);
    String[] lines = dataString.split("\n");
    List<String[]> data = new ArrayList<>();

    for (int i = 1; i < lines.length; i++) {
      data.add(lines[i].split(","));
    }
    String[][] dataArray = new String[data.size()][];
    dataArray = data.toArray(dataArray);

    return transform.apply(dataArray);
  }

  protected static double parseDouble(String str) {
    if ("Inf".equals(str)) {
      return Double.POSITIVE_INFINITY;
    } else if ("-Inf".equals(str)) {
      return Double.NEGATIVE_INFINITY;
    } else {
      return Double.parseDouble(str);
    }
  }

  protected static int parseInt(String str) {
    if ("Inf".equals(str.trim())) {
      return Integer.MAX_VALUE;
    } else if ("-Inf".equals(str.trim())) {
      return Integer.MIN_VALUE;
    } else {
      return Integer.parseInt(str.trim());
    }
  }

  protected MatrixProfile loadCheck(String fileName,
                                    boolean b) throws IOException, URISyntaxException {
    return loadCheck(fileName, false);
  }

  public record ToyData(double x, double y, double z) {}
}

