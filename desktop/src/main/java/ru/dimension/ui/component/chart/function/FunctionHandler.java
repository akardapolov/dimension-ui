package ru.dimension.ui.component.chart.function;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.TableNameEmptyException;
import ru.dimension.db.model.GroupFunction;
import ru.dimension.db.model.output.StackedColumn;
import ru.dimension.db.model.profile.TProfile;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.ui.model.info.QueryInfo;
import ru.dimension.ui.component.chart.FunctionDataHandler;
import ru.dimension.ui.component.chart.StackedChart;

@Log4j2
public abstract class FunctionHandler implements FunctionDataHandler {

  protected Metric metric;
  protected QueryInfo queryInfo;
  protected DStore dStore;

  @Getter
  private TProfile tProfile;

  public FunctionHandler(Metric metric,
                         QueryInfo queryInfo,
                         DStore dStore) {
    this.metric = metric;
    this.queryInfo = queryInfo;
    this.dStore = dStore;

    initTProfile(queryInfo);
  }

  private void initTProfile(QueryInfo queryInfo) {
    try {
      tProfile = this.dStore.getTProfile(queryInfo.getName());
    } catch (TableNameEmptyException e) {
      log.catching(e);
      throw new RuntimeException(e);
    }
  }

  protected void fillSeries(List<StackedColumn> sColumnList,
                            Set<String> series) {
    sColumnList.stream()
        .map(StackedColumn::getKeyCount)
        .map(Map::keySet)
        .flatMap(Collection::stream)
        .forEach(series::add);
  }

  protected void handleFunction(long begin,
                                long end,
                                boolean isClientRealTime,
                                long finalX,
                                double yK,
                                StackedChart stackedChart,
                                GroupFunction groupFunction) {
    try {
      List<StackedColumn> stackedColumns
          = dStore.getStacked(queryInfo.getName(), metric.getYAxis(), groupFunction, begin, end);

      long x;
      double y = getY(groupFunction, stackedColumns);

      if (isClientRealTime) {
        x = stackedColumns.isEmpty() ? finalX : stackedColumns.getLast().getTail();
      } else {
        x = finalX;
      }

      stackedChart.addSeriesValue(x, y / yK, metric.getYAxis().getColName());

    } catch (Exception e) {
      log.info(e);
    }
  }

  protected double getY(GroupFunction groupFunction,
                        List<StackedColumn> stackedColumns) {
    double y = 0;

    Optional<StackedColumn> stackedColumn = stackedColumns.stream().findAny();

    if (stackedColumn.isPresent()) {
      Map<String, Double> keyValues = Collections.emptyMap();

      if (GroupFunction.AVG.equals(groupFunction)) {
        keyValues = stackedColumn.get().getKeyAvg();
      } else if (GroupFunction.SUM.equals(groupFunction)) {
        keyValues = stackedColumn.get().getKeySum();
      }

      if (!keyValues.isEmpty()) {
        String colName = metric.getYAxis().getColName();
        Optional<Double> value = keyValues.entrySet()
            .stream()
            .filter(f -> f.getKey().equalsIgnoreCase(colName))
            .map(Map.Entry::getValue)
            .findAny();

        if (value.isPresent()) {
          y = value.get();
        }
      }
    }

    return y;
  }
}