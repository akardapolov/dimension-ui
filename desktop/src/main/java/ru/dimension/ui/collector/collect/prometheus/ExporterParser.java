package ru.dimension.ui.collector.collect.prometheus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ExporterParser {

  @Inject
  public ExporterParser() {
  }

  public Map<String, List<Map.Entry<String, Double>>> textToMetricKeyValue(String resp) {
    Map<String, List<Map.Entry<String, Double>>> metricKeyValue = new LinkedHashMap<>();
    parsePrometheusBlocks(resp)
        .forEach(block -> {
          Map.Entry<MetricFamily, List<Map.Entry<String, Double>>> entry = parse(block);
          metricKeyValue.put(entry.getKey().getName(), entry.getValue());
        });
    return metricKeyValue;
  }

  public Map<String, MetricFamily> textToMetric(String resp) {
    Map<String, MetricFamily> metricMap = new ConcurrentHashMap<>(10);
    parsePrometheusBlocks(resp)
        .forEach(block -> {
          MetricFamily metricFamily = parse(block).getKey();
          metricMap.put(metricFamily.getName(), metricFamily);
        });
    return metricMap;
  }

  public static List<String> parsePrometheusBlocks(String prometheusData) {
    List<String> blocks = new ArrayList<>();
    // Pattern to match blocks starting with # HELP till the next # HELP or EOF
    String regex = "(# HELP.*?)(?=\\n# HELP|$)";
    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
    Matcher matcher = pattern.matcher(prometheusData);

    while (matcher.find()) {
      blocks.add(matcher.group(1).trim()); // Trim to remove possible leading/trailing whitespaces
    }

    return blocks;
  }

  public Map.Entry<MetricFamily, List<Map.Entry<String, Double>>> parse(String prometheusData) {
    List<String> lines = prometheusData.lines().toList();

    MetricFamily metricFamily = new MetricFamily();
    List<Map.Entry<String, Double>> metricKeyValueList = new ArrayList<>();

    List<MetricFamily.Metric> metrics = new ArrayList<>();
    MetricFamily.Metric currentMetric = null;

    for (String line : lines) {
      if (line.startsWith("# HELP")) {
        metricFamily.setHelp(line.substring(line.indexOf(" ") + 1));
      } else if (line.startsWith("# TYPE")) {
        String[] parts = line.split(" ");
        metricFamily.setName(parts[2]);
        MetricType type = MetricType.getType(parts[3].toLowerCase());
        metricFamily.setMetricType(type);
      } else if (line.matches("^[a-zA-Z_][a-zA-Z0-9_]*.*")) {
        metricKeyValueList.add(getKeyAndValueFromMetricLine(line));
        currentMetric = parseMetricLine(line, metricFamily);
        metrics.add(currentMetric);
      }
    }

    metricFamily.setMetricList(metrics);
    return Map.entry(metricFamily, metricKeyValueList);
  }

  private MetricFamily.Metric parseMetricLine(String line,
                                              MetricFamily metricFamily) {
    MetricFamily.Metric metric = new MetricFamily.Metric();

    Map.Entry<String, Double> metricKeyValue = getKeyAndValueFromMetricLine(line);

    // Match for name and optional labels
    Pattern pattern = Pattern.compile("([^{}]+)\\{(.*?)}");
    Matcher matcher = pattern.matcher(metricKeyValue.getKey());

    if (matcher.matches()) {
      String labelsPart = matcher.group(2);
      metric.setLabelPair(parseLabels(labelsPart));
    } else {
      metric.setLabelPair(new ArrayList<>());
    }

    try {
      setMetricTypeAndValue(metricFamily, metric, metricKeyValue.getValue());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return metric;
  }

  private Map.Entry<String, Double> getKeyAndValueFromMetricLine(String line) {
    int lastSpaceIndex = line.lastIndexOf(' ');
    String key = line.substring(0, lastSpaceIndex);
    String[] value = line.substring(lastSpaceIndex + 1).split(" ");

    return Map.entry(key, Double.valueOf(value[0]));
  }

  private List<MetricFamily.Label> parseLabels(String labelsPart) {
    List<MetricFamily.Label> labels = new ArrayList<>();
    String[] labelPairs = labelsPart.split(",");
    for (String pair : labelPairs) {
      String[] keyValue = pair.split("=");
      MetricFamily.Label label = new MetricFamily.Label();
      label.setName(keyValue[0].trim());
      label.setValue(keyValue[1].replaceAll("\"", "").trim());
      labels.add(label);
    }
    return labels;
  }

  private void setMetricTypeAndValue(MetricFamily metricFamily,
                                     MetricFamily.Metric metric,
                                     Double value) throws Exception {
    switch (metricFamily.getMetricType()) {
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
        log.warn("Not full supported yet");
        MetricFamily.Gauge gaugeSum = new MetricFamily.Gauge();
        gaugeSum.setValue(value);
        metric.setGauge(gaugeSum);
      }
      default -> throw new Exception("no such type in metricFamily");
    }
  }
}
