package ru.dimension.ui.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.collector.collect.prometheus.ExporterParser;
import ru.dimension.ui.collector.collect.prometheus.MetricFamily;

@Log4j2
public class PrometheusParserTest {
  // TODO content from Spring Boot Actuator’s /prometheus Endpoint
  // TODO https://www.callicoder.com/spring-boot-actuator-metrics-monitoring-dashboard-prometheus-grafana/

  @Test
  public void textToMetricFamilyTest() throws IOException {
    String resp = getTestData("response", "spring_boot_prometheus.json");

    ExporterParser parser = new ExporterParser();

    Map<String, MetricFamily> map = parser.textToMetric(resp);

    assertEqualsMetricFamily(map);
  }

  @Test
  public void textToMetricKeyValueTest() throws IOException {
    String resp = getTestData("response", "spring_boot_prometheus.json");

    ExporterParser parser = new ExporterParser();

    Map<String, List<Entry<String, Double>>> metricFamilyMap = parser.textToMetricKeyValue(resp);

    metricFamilyMap.forEach((key, val) -> assertTrue(val.stream().anyMatch(f -> f.getKey().startsWith(key))));
  }

  private void assertEqualsMetricFamily(Map<String, MetricFamily> map) {
    assertEquals("jetty_threads_idle", map.get("jetty_threads_idle").getName());
    assertEquals("jvm_gc_memory_promoted_bytes_total", map.get("jvm_gc_memory_promoted_bytes_total").getName());
    assertEquals("jetty_threads_jobs", map.get("jetty_threads_jobs").getName());
    assertEquals("hikaricp_connections_active", map.get("hikaricp_connections_active").getName());
    assertEquals("hikaricp_connections_acquire_seconds_max", map.get("hikaricp_connections_acquire_seconds_max")
        .getName());
    assertEquals("jvm_gc_memory_allocated_bytes_total", map.get("jvm_gc_memory_allocated_bytes_total").getName());
    assertEquals("jvm_threads_daemon_threads", map.get("jvm_threads_daemon_threads").getName());
    assertEquals("process_start_time_seconds", map.get("process_start_time_seconds").getName());
    assertEquals("jvm_classes_loaded_classes", map.get("jvm_classes_loaded_classes").getName());
    assertEquals("jvm_gc_max_data_size_bytes", map.get("jvm_gc_max_data_size_bytes").getName());
    assertEquals("jvm_buffer_total_capacity_bytes", map.get("jvm_buffer_total_capacity_bytes").getName());
    assertEquals("hikaricp_connections", map.get("hikaricp_connections").getName());
    assertEquals("jvm_buffer_memory_used_bytes", map.get("jvm_buffer_memory_used_bytes").getName());
    assertEquals("hikaricp_connections_acquire_seconds", map.get("hikaricp_connections_acquire_seconds").getName());
    assertEquals("hikaricp_connections_usage_seconds_max", map.get("hikaricp_connections_usage_seconds_max").getName());
    assertEquals("jetty_threads_current", map.get("jetty_threads_current").getName());
    assertEquals("hikaricp_connections_usage_seconds", map.get("hikaricp_connections_usage_seconds").getName());
    assertEquals("http_server_requests_seconds", map.get("http_server_requests_seconds").getName());
    assertEquals("jdbc_connections_idle", map.get("jdbc_connections_idle").getName());
    assertEquals("system_cpu_count", map.get("system_cpu_count").getName());
    assertEquals("process_uptime_seconds", map.get("process_uptime_seconds").getName());
    assertEquals("jvm_threads_states_threads", map.get("jvm_threads_states_threads").getName());
    assertEquals("jvm_memory_committed_bytes", map.get("jvm_memory_committed_bytes").getName());
    assertEquals("jetty_threads_config_max", map.get("jetty_threads_config_max").getName());
    assertEquals("jdbc_connections_min", map.get("jdbc_connections_min").getName());
    assertEquals("jvm_gc_pause_seconds_max", map.get("jvm_gc_pause_seconds_max").getName());
    assertEquals("system_cpu_usage", map.get("system_cpu_usage").getName());
    assertEquals("hikaricp_connections_pending", map.get("hikaricp_connections_pending").getName());
    assertEquals("jvm_threads_peak_threads", map.get("jvm_threads_peak_threads").getName());
    assertEquals("jvm_memory_used_bytes", map.get("jvm_memory_used_bytes").getName());
    assertEquals("process_cpu_usage", map.get("process_cpu_usage").getName());
    assertEquals("http_server_requests_seconds_max", map.get("http_server_requests_seconds_max").getName());
    assertEquals("jvm_gc_pause_seconds", map.get("jvm_gc_pause_seconds").getName());
    assertEquals("jetty_threads_config_min", map.get("jetty_threads_config_min").getName());
    assertEquals("hikaricp_connections_creation_seconds", map.get("hikaricp_connections_creation_seconds").getName());
    assertEquals("jvm_memory_max_bytes", map.get("jvm_memory_max_bytes").getName());
    assertEquals("jdbc_connections_active", map.get("jdbc_connections_active").getName());
    assertEquals("jdbc_connections_max", map.get("jdbc_connections_max").getName());
    assertEquals("jvm_gc_live_data_size_bytes", map.get("jvm_gc_live_data_size_bytes").getName());
    assertEquals("hikaricp_connections_idle", map.get("hikaricp_connections_idle").getName());
    assertEquals("hikaricp_connections_timeout_total", map.get("hikaricp_connections_timeout_total").getName());
    assertEquals("hikaricp_connections_max", map.get("hikaricp_connections_max").getName());
    assertEquals("jvm_threads_live_threads", map.get("jvm_threads_live_threads").getName());
    assertEquals("logback_events_total", map.get("logback_events_total").getName());
    assertEquals("jvm_classes_unloaded_classes_total", map.get("jvm_classes_unloaded_classes_total").getName());
    assertEquals("jvm_buffer_count_buffers", map.get("jvm_buffer_count_buffers").getName());
    assertEquals("jetty_threads_busy", map.get("jetty_threads_busy").getName());
    assertEquals("hikaricp_connections_min", map.get("hikaricp_connections_min").getName());
    assertEquals("hikaricp_connections_creation_seconds_max", map.get("hikaricp_connections_creation_seconds_max")
        .getName());
  }

  protected String getTestData(String dirName,
                               String fileName) throws IOException {
    return Files.readString(Paths.get("src", "test", "resources", dirName, fileName));
  }
}
