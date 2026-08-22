package ru.dimension.ui.model.config;

import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Data;

@EqualsAndHashCode(callSuper = true)
@Data
public class UISettings extends ConfigEntity {

  @SerializedName(value = "chartSettings")
  private ChartUISettings chartSettings;

  public UISettings() {
    this.setId(1);
    this.setName("ui_settings");
    this.chartSettings = new ChartUISettings();
  }

}
