package ru.dimension.ui.model;

import java.util.Objects;
import ru.dimension.ui.state.ChartKey;

public class AdHocChartKey extends ChartKey {
  private final AdHocKey adHocKey;

  public AdHocChartKey(AdHocKey adHocKey) {
    super(null, null);
    this.adHocKey = adHocKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AdHocChartKey that = (AdHocChartKey) o;
    return Objects.equals(adHocKey, that.adHocKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(adHocKey);
  }
}
