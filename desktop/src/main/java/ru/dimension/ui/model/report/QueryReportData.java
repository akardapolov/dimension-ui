package ru.dimension.ui.model.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryReportData {
  private List<CProfileReport> cProfileReportList = new ArrayList<>();
  private List<MetricReport> metricReportList = new ArrayList<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    QueryReportData that = (QueryReportData) o;
    return Objects.equals(cProfileReportList, that.cProfileReportList) &&
        Objects.equals(metricReportList, that.metricReportList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cProfileReportList, metricReportList);
  }
}
