package ru.dimension.ui.model.chart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.model.date.DateLocale;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartRange {

  private long begin;
  private long end;

  @Override
  public String toString() {
    return "ChartRange{" +
        "begin=" + DateHelper.getDateFormatted(DateLocale.RU, begin) +
        ", end=" + DateHelper.getDateFormatted(DateLocale.RU, end) +
        '}';
  }
}
