package ru.dimension.ui.collector.collect.prometheus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ExporterParser {

  @Inject
  public ExporterParser() {
  }

  public Map<String, List<Map.Entry<String, Double>>> textToMetricKeyValue(String resp) {
    Map<String, List<Map.Entry<String, Double>>> metricKeyValue = new LinkedHashMap<>();
    parsePrometheusBlocks(resp).forEach(block -> {
      Map.Entry<MetricFamily, List<Map.Entry<String, Double>>> entry = parse(block);
      MetricFamily mf = entry.getKey();
      if (mf.getName() != null) {
        metricKeyValue.put(mf.getName(), entry.getValue());
      }
    });
    return metricKeyValue;
  }

  public Map<String, MetricFamily> textToMetric(String resp) {
    Map<String, MetricFamily> metricMap = new ConcurrentHashMap<>(10);
    parsePrometheusBlocks(resp).forEach(block -> {
      MetricFamily metricFamily = parse(block).getKey();
      if (metricFamily.getName() != null) {
        metricMap.put(metricFamily.getName(), metricFamily);
      }
    });
    return metricMap;
  }

  /**
   * Split exposition text into blocks. Block can start with # TYPE or # HELP.
   */
  public static List<String> parsePrometheusBlocks(String prometheusData) {
    if (prometheusData == null || prometheusData.isBlank()) {
      return List.of();
    }

    List<String> blocks = new ArrayList<>();

    // Start at "# HELP" or "# TYPE" and consume until next "# HELP/# TYPE" or EOF
    Pattern pattern = Pattern.compile(
        "(?ms)(^#\\s+(?:HELP|TYPE)\\b.*?)(?=^#\\s+(?:HELP|TYPE)\\b|\\z)"
    );
    Matcher matcher = pattern.matcher(prometheusData);

    while (matcher.find()) {
      blocks.add(matcher.group(1).trim());
    }

    return blocks;
  }

  public Map.Entry<MetricFamily, List<Map.Entry<String, Double>>> parse(String prometheusData) {
    List<String> lines = prometheusData == null ? List.of() : prometheusData.lines().toList();

    MetricFamily metricFamily = new MetricFamily();
    List<Map.Entry<String, Double>> metricKeyValueList = new ArrayList<>();
    List<MetricFamily.Metric> metrics = new ArrayList<>();

    for (String raw : lines) {
      String line = raw == null ? "" : raw.trim();
      if (line.isEmpty()) continue;

      if (line.startsWith("# HELP ")) {
        // "# HELP <name> <help text...>"
        String[] parts = line.split("\\s+", 4);
        if (parts.length >= 3) {
          String name = parts[2];
          String help = parts.length == 4 ? parts[3] : "";
          if (metricFamily.getName() == null) {
            metricFamily.setName(name);
          }
          metricFamily.setHelp(help);
        }
        continue;
      }

      if (line.startsWith("# TYPE ")) {
        // "# TYPE <name> <type>"
        String[] parts = line.split("\\s+", 4);
        if (parts.length >= 4) {
          String name = parts[2];
          MetricType type = MetricType.getType(parts[3].toLowerCase());
          metricFamily.setName(name);
          metricFamily.setMetricType(type != null ? type : MetricType.UNTYPED);
        }
        continue;
      }

      if (line.startsWith("#")) {
        continue; // ignore other comments
      }

      // Sample line
      if (line.matches("^[a-zA-Z_][a-zA-Z0-9_]*.*")) {
        Map.Entry<String, Double> kv = getKeyAndValueFromMetricLine(line);
        metricKeyValueList.add(kv);

        // If type missing in this block, do not crash
        if (metricFamily.getMetricType() == null) {
          metricFamily.setMetricType(MetricType.UNTYPED);
        }
        // If name missing, infer from key (before '{')
        if (metricFamily.getName() == null) {
          metricFamily.setName(getBaseMetricName(kv.getKey()));
        }

        MetricFamily.Metric m = parseMetricLine(kv, metricFamily);
        metrics.add(m);
      }
    }

    metricFamily.setMetricList(metrics);
    return Map.entry(metricFamily, metricKeyValueList);
  }

  private MetricFamily.Metric parseMetricLine(Map.Entry<String, Double> metricKeyValue,
                                              MetricFamily metricFamily) {
    MetricFamily.Metric metric = new MetricFamily.Metric();

    // Match for name and optional labels: name{...}
    Pattern pattern = Pattern.compile("([^{}]+)\\{(.*?)}");
    Matcher matcher = pattern.matcher(metricKeyValue.getKey());

    if (matcher.matches()) {
      String labelsPart = matcher.group(2);
      metric.setLabelPair(parseLabels(labelsPart));
    } else {
      metric.setLabelPair(new ArrayList<>());
    }

    setMetricTypeAndValue(metricFamily, metric, metricKeyValue.getValue());
    return metric;
  }

  /**
   * IMPORTANT: label values may contain spaces => cannot split whole line by whitespace.
   * We split "key" from "value [timestamp]" by:
   * - if there is '}', key ends at last '}' + 1
   * - else key ends at first whitespace
   */
  private Map.Entry<String, Double> getKeyAndValueFromMetricLine(String line) {
    String s = line.trim();

    int keyEnd;
    int lastBrace = s.lastIndexOf('}');
    if (lastBrace >= 0) {
      keyEnd = lastBrace + 1;
    } else {
      keyEnd = firstWhitespaceIndex(s);
      if (keyEnd < 0) {
        throw new IllegalArgumentException("Invalid metric sample line: " + line);
      }
    }

    String key = s.substring(0, keyEnd).trim();
    String tail = s.substring(keyEnd).trim(); // "<value> [timestamp]"
    if (tail.isEmpty()) {
      throw new IllegalArgumentException("Invalid metric sample line (no value): " + line);
    }

    // tail should start with value token; timestamp (if present) comes after it
    String[] tailTokens = tail.split("\\s+");
    String valueToken = tailTokens[0];

    return Map.entry(key, parsePrometheusDouble(valueToken));
  }

  private static int firstWhitespaceIndex(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == ' ' || c == '\t') return i;
    }
    return -1;
  }

  private static String getBaseMetricName(String key) {
    int brace = key.indexOf('{');
    return brace >= 0 ? key.substring(0, brace) : key;
  }

  private static Double parsePrometheusDouble(String token) {
    return switch (token) {
      case "NaN" -> Double.NaN;
      case "+Inf", "Inf" -> Double.POSITIVE_INFINITY;
      case "-Inf" -> Double.NEGATIVE_INFINITY;
      default -> Double.valueOf(token);
    };
  }

  private List<MetricFamily.Label> parseLabels(String labelsPart) {
    List<MetricFamily.Label> labels = new ArrayList<>();
    if (labelsPart == null || labelsPart.isBlank()) {
      return labels;
    }

    String[] labelPairs = labelsPart.split(",");
    for (String pair : labelPairs) {
      String[] keyValue = pair.split("=", 2);
      if (keyValue.length != 2) continue;

      MetricFamily.Label label = new MetricFamily.Label();
      label.setName(keyValue[0].trim());
      label.setValue(keyValue[1].replaceAll("\"", "").trim());
      labels.add(label);
    }
    return labels;
  }

  private void setMetricTypeAndValue(MetricFamily metricFamily,
                                     MetricFamily.Metric metric,
                                     Double value) {
    MetricType type = metricFamily.getMetricType();
    if (type == null) {
      type = MetricType.UNTYPED;
      metricFamily.setMetricType(type);
    }

    switch (type) {
      case INFO -> {
        MetricFamily.Info info = new MetricFamily.Info();
        info.setValue(value);
        metric.setInfo(info);
      }
      case COUNTER -> {
        MetricFamily.Counter counter = new MetricFamily.Counter();
        counter.setValue(value);
        metric.setCounter(counter);
      }
      case GAUGE -> {
        MetricFamily.Gauge gauge = new MetricFamily.Gauge();
        gauge.setValue(value);
        metric.setGauge(gauge);
      }
      case UNTYPED -> {
        MetricFamily.Untyped untyped = new MetricFamily.Untyped();
        untyped.setValue(value);
        metric.setUntyped(untyped);
      }
      case SUMMARY, HISTOGRAM -> {
        log.warn("SUMMARY/HISTOGRAM not fully supported yet. Storing as GAUGE.");
        MetricFamily.Gauge gauge = new MetricFamily.Gauge();
        gauge.setValue(value);
        metric.setGauge(gauge);
      }
      default -> throw new IllegalStateException("Unsupported metric type: " + type);
    }
  }
}