package ru.dimension.ui.component.chart;

import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.CProfile;

public interface FunctionDataHandler extends HelperChart {

  void fillSeriesData(long begin,
                      long end,
                      Set<String> series);

  void setFilter(Map.Entry<CProfile, List<String>> filter);

  void handleFunction(long begin,
                      long end,
                      boolean isClientRealTime,
                      long finalX,
                      double yK,
                      Set<String> series,
                      StackedChart stackedChart);

  void handleFunction(long begin,
                      long end,
                      double yK,
                      Set<String> series,
                      CProfile cProfileFilter,
                      String[] filterData,
                      CompareFunction compareFunction,
                      StackedChart stackedChart);

  List<StackedColumn> handleFunctionComplex(long begin,
                                            long end)
      throws BeginEndWrongOrderException, SqlColMetadataException;
}
