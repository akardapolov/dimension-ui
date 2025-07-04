package ru.dimension.ui.model.config;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class ConfigEntity {

  @EqualsAndHashCode.Include
  @SerializedName(value = "id")
  private int id;
  @SerializedName(value = "name")
  private String name;
}
