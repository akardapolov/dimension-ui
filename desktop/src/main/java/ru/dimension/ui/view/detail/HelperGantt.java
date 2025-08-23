package ru.dimension.ui.view.detail;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.dimension.db.core.DStore;
import ru.dimension.db.exception.BeginEndWrongOrderException;
import ru.dimension.db.exception.GanttColumnNotSupportedException;
import ru.dimension.db.exception.SqlColMetadataException;
import ru.dimension.db.model.CompareFunction;
import ru.dimension.db.model.filter.CompositeFilter;
import ru.dimension.db.model.filter.FilterCondition;
import ru.dimension.db.model.filter.LogicalOperator;
import ru.dimension.db.model.output.GanttColumnCount;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.model.view.SeriesType;

public interface HelperGantt {

  default List<GanttColumnCount> getGanttColumnList(SeriesType seriesType,
                                                    DStore dStore,
                                                    String tableName,
                                                    CProfile firstGrpBy,
                                                    CProfile secondGrpBy,
                                                    CProfile cProfileFilter,
                                                    String[] filterData,
                                                    CompareFunction compareFunction,
                                                    long begin,
                                                    long end)
      throws SqlColMetadataException, BeginEndWrongOrderException, GanttColumnNotSupportedException {
    List<GanttColumnCount> ganttColumnList = new ArrayList<>();
    if (Objects.isNull(seriesType) || SeriesType.COMMON.equals(seriesType)) {
      ganttColumnList =
          dStore.getGanttCount(tableName, firstGrpBy, secondGrpBy, null, begin, end);
    } else if (SeriesType.CUSTOM.equals(seriesType)) {
      CompositeFilter compositeFilter = new CompositeFilter(
          List.of(new FilterCondition(cProfileFilter, filterData, compareFunction)),
          LogicalOperator.AND);

      ganttColumnList =
          dStore.getGanttCount(tableName, firstGrpBy, secondGrpBy, compositeFilter, begin, end);
    }
    return ganttColumnList;
  }
}
