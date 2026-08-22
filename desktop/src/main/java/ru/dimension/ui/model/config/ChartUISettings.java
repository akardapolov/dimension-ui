package ru.dimension.ui.model.config;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ChartUISettings {

  @SerializedName(value = "hideCustomLegend")
  private boolean hideCustomLegend = true;

  @SerializedName(value = "hideBuiltInLegend")
  private boolean hideBuiltInLegend = true;

  @SerializedName(value = "hideYAxis")
  private boolean hideYAxis = true;

  @SerializedName(value = "hideXAxis")
  private boolean hideXAxis = true;

  @SerializedName(value = "hidePlotInsets")
  private boolean hidePlotInsets = true;

  @SerializedName(value = "hideChartPadding")
  private boolean hideChartPadding = true;

}
