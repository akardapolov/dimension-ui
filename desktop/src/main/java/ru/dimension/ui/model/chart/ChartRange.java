package ru.dimension.ui.model.chart;

import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartRange {

  private long begin;
  private long end;

  @Override
  public String toString() {
    return "ChartRange{" +
        "begin=" + getDate(begin) +
        ", end=" + getDate(end) +
        '}';
  }

  protected String getDate(long l) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    Date date = new Date(l);
    return dateFormat.format(date);
  }
}
