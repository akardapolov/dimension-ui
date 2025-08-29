package ru.dimension.ui.model.config;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ColorProfile extends ConfigEntity {
  @SerializedName(value = "colors")
  private Map<String, String> colors; // Key: series name, Value: hex color code
}