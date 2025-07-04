package ru.dimension.ui.model.report;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryReportData {

  private List<CProfileReport> cProfileReportList = new ArrayList<>();
  private List<MetricReport> metricReportList = new ArrayList<>();
}
