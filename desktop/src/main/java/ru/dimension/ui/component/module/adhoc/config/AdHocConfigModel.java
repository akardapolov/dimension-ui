package ru.dimension.ui.component.module.adhoc.config;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Data
@Log4j2
public class AdHocConfigModel {
  private final StringBuilder globalKey;

  public AdHocConfigModel() {
    this.globalKey = new StringBuilder();
  }

  public void setGlobalKey(String globalKey) {
    this.globalKey.setLength(0);
    this.globalKey.append(globalKey);
  }

  public String getGlobalKey() {
    return this.globalKey.toString();
  }
}