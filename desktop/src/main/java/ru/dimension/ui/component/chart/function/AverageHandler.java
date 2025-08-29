package ru.dimension.ui.component.chart.function;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.GroupFunction;
import ru.dimension.db.model.filter.CompositeFilter;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.chart.StackedChart;
import ru.dimension.ui.helper.FilterHelper;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;

@Log4j2
public class AverageHandler extends FunctionHandler {

  private Map<CProfile, LinkedHashSet<String>> topMapSelected;

  public AverageHandler(ProfileTaskQueryKey profileTaskQueryKey,
                        Metric metric,
                        QueryInfo queryInfo,
                        DStore dStore) {
    super(profileTaskQueryKey, metric, queryInfo, dStore);
  }

  @Override
  public void fillSeriesData(long begin,
                             long end,
                             Set<String> series) {
    series.add(metric.getYAxis().getColName());
  }

  @Override
  public void setFilter(Map<CProfile, LinkedHashSet<String>> topMapSelected) {
    this.topMapSelected = topMapSelected;
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
                             Map<CProfile, LinkedHashSet<String>> topMapSelected,
                             StackedChart stackedChart) {
    GroupFunction groupFunction = GroupFunction.AVG;
    try {
      CompositeFilter compositeFilter = FilterHelper.toCompositeFilter(topMapSelected);

      List<StackedColumn> stackedColumns
          = dStore.getStacked(queryInfo.getName(), metric.getYAxis(), groupFunction, compositeFilter, begin, end);

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

    if (topMapSelected == null) {
      return dStore.getStacked(queryInfo.getName(), metric.getYAxis(), GroupFunction.AVG, null, begin, end);
    } else {
      CompositeFilter compositeFilter = FilterHelper.toCompositeFilter(topMapSelected);

      return dStore.getStacked(queryInfo.getName(),
                               metric.getYAxis(),
                               GroupFunction.AVG,
                               compositeFilter,
                               begin,
                               end);
    }
  }
}
