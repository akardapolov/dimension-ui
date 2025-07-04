package ru.dimension.ui.view.chart.stacked.function;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.GroupFunction;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.view.chart.StackedChart;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.view.chart.stacked.FunctionHandler;

@Log4j2
public class AverageHandler extends FunctionHandler {

  private Entry<CProfile, List<String>> filter;

  public AverageHandler(Metric metric,
                        QueryInfo queryInfo,
                        DStore dStore) {
    super(metric, queryInfo, dStore);
  }

  @Override
  public void fillSeriesData(long begin,
                             long end,
                             Set<String> series) {
    series.add(metric.getYAxis().getColName());
  }

  @Override
  public void setFilter(Entry<CProfile, List<String>> filter) {
    this.filter = filter;
  }

  @Override
  public void handleFunction(long begin,
                             long end,
                             boolean isClientRealTime,
                             long finalX,
                             double yK,
                             Set<String> series,
                             StackedChart stackedChart) {

    super.handleFunction(begin, end, isClientRealTime, finalX, 1, stackedChart, GroupFunction.AVG);
  }

  @Override
  public void handleFunction(long begin,
                             long end,
                             double yK,
                             Set<String> series,
                             CProfile cProfileFilter,
                             String[] filterData,
                             CompareFunction compareFunction,
                             StackedChart stackedChart) {
    GroupFunction groupFunction = GroupFunction.AVG;
    try {
      List<StackedColumn> stackedColumns
          = dStore.getStacked(queryInfo.getName(), metric.getYAxis(), groupFunction,
                              cProfileFilter, filterData, compareFunction,
                              begin, end);

      long x;
      double y = getY(groupFunction, stackedColumns);

      x = stackedColumns.isEmpty() ? begin : stackedColumns.getLast().getTail();

      stackedChart.addSeriesValue(x, y / yK, metric.getYAxis().getColName());

    } catch (Exception e) {
      e.printStackTrace();
      log.info(e);
    }
  }

  @Override
  public List<StackedColumn> handleFunctionComplex(long begin,
                                                   long end)
      throws BeginEndWrongOrderException, SqlColMetadataException {

    if (filter == null) {
      return dStore.getStacked(queryInfo.getName(), metric.getYAxis(), GroupFunction.AVG, begin, end);
    } else {
      return dStore.getStacked(queryInfo.getName(),
                               metric.getYAxis(),
                               GroupFunction.AVG,
                               filter.getKey(),
                               filter.getValue().toArray(new String[0]),
                               CompareFunction.EQUAL,
                               begin,
                               end);
    }
  }
}
