package ru.dimension.ui.view.structure.workspace.task;

import lombok.Data;
import ru.dimension.ui.model.table.JXTableCase;

@Data
public class QueryMetricColumnContainer {

  private JXTableCase jXTableCaseQuery;
  private JXTableCase jXTableCaseMetric;
  private JXTableCase jXTableCaseColumn;

  public QueryMetricColumnContainer() {
  }

  public void addQueryToCard(JXTableCase jXTableCaseQuery) {
    this.jXTableCaseQuery = jXTableCaseQuery;
  }

  public void addMetricToCard(JXTableCase jXTableCaseMetric) {
    this.jXTableCaseMetric = jXTableCaseMetric;
  }

  public void addColumnToCard(JXTableCase jXTableCaseColumn) {
    this.jXTableCaseColumn = jXTableCaseColumn;
  }
}
