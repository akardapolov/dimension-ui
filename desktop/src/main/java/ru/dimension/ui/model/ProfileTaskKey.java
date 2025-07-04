package ru.dimension.ui.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
@ToString
public class ProfileTaskKey {

  private int profileId;
  private int taskId;
}
