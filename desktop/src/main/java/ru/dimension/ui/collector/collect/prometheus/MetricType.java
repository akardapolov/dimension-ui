package ru.dimension.ui.collector.collect.prometheus;

public enum MetricType {
  INFO("info"),
  COUNTER("counter"),
  GAUGE("gauge"),
  SUMMARY("summary"),
  UNTYPED("untyped"),
  HISTOGRAM("histogram");

  private final String value;

  MetricType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static MetricType getType(String value) {
    for (MetricType metricType : values()) {
      if (metricType.getValue().equals(value)) {
        return metricType;
      }
    }
    return null;
  }
}
