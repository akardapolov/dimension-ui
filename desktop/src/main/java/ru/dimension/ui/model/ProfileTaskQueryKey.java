package ru.dimension.ui.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
@ToString
@NoArgsConstructor
public class ProfileTaskQueryKey {

  private int profileId;
  private int taskId;
  private int queryId;

  public String getColorProfileName() {
    return "color_profile" + "_" + profileId + "_" + taskId + "_" + queryId;
  }
}
