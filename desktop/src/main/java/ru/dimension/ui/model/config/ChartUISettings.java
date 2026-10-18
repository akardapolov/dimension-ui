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

  @SerializedName(value = "hideYAxisLabel")
  private boolean hideYAxisLabel = false;

  @SerializedName(value = "hideYAxisValues")
  private boolean hideYAxisValues = false;

  @SerializedName(value = "hideXAxisLabel")
  private boolean hideXAxisLabel = false;

  @SerializedName(value = "hideXAxisValues")
  private boolean hideXAxisValues = false;

  @SerializedName(value = "hidePlotInsets")
  private boolean hidePlotInsets = true;

  @SerializedName(value = "hideChartPadding")
  private boolean hideChartPadding = true;

  @SerializedName(value = "legendFixedSize")
  private boolean legendFixedSize = false;

  @SerializedName(value = "legendFixedWidth")
  private int legendFixedWidth = 150;

  @SerializedName(value = "valueAbbreviation")
  private boolean valueAbbreviation = true;

  @SerializedName(value = "valueAxisFixedSize")
  private boolean valueAxisFixedSize = true;

  @SerializedName(value = "valueAxisFixedWidth")
  private int valueAxisFixedWidth = 50;

}