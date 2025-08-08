package ru.dimension.ui.state;

import lombok.Getter;
import lombok.ToString;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.db.model.profile.CProfile;
import java.util.Objects;

@ToString
public class ChartKey {
  @Getter
  private final ProfileTaskQueryKey profileTaskQueryKey;
  private final CProfile cProfile;

  public ChartKey(ProfileTaskQueryKey profileTaskQueryKey, CProfile cProfile) {
    this.profileTaskQueryKey = profileTaskQueryKey;
    this.cProfile = cProfile;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChartKey chartKey = (ChartKey) o;
    return Objects.equals(profileTaskQueryKey, chartKey.profileTaskQueryKey) &&
        Objects.equals(cProfile, chartKey.cProfile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(profileTaskQueryKey, cProfile);
  }
}
