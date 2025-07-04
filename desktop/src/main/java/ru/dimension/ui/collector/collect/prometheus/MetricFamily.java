package ru.dimension.ui.collector.collect.prometheus;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MetricFamily {

  private String name;
  private String help;
  private MetricType metricType;
  private List<Metric> metricList;

  @Data
  public static class Metric {

    private List<Label> labelPair;
    private Info info;
    private Gauge gauge;
    private Counter counter;
    private Summary summary;
    private Untyped untyped;
    private Histogram histogram;
    private Long timestampMs;
  }

  @Data
  public static class Label {

    private String name;
    private String value;
  }

  @Data
  public static class Info {

    private double value;
  }

  @Data
  public static class Counter {

    private double value;
  }

  @Data
  public static class Gauge {

    private double value;
  }

  @Data
  public static class Untyped {

    private double value;
  }

  @Data
  public static class Summary {

    private long count;
    private double sum;
    private List<Quantile> quantileList = new ArrayList<>();
  }

  @Data
  public static class Quantile {

    private double xLabel;
    private double value;
  }

  @Data
  public static class Histogram {

    private long count;
    private double sum;
    private List<Bucket> bucketList = new ArrayList<>();
  }

  @Data
  public static class Bucket {

    private long cumulativeCount;
    private double upperBound;
  }
}
