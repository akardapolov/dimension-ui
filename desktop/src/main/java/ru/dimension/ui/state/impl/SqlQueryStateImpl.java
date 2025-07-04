package ru.dimension.ui.state.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
@Singleton
public class SqlQueryStateImpl implements SqlQueryState {

  private final Map<ProfileTaskQueryKey, Long> profileTaskQueryKeyMap;

  @Inject
  public SqlQueryStateImpl() {
    this.profileTaskQueryKeyMap = new ConcurrentHashMap<>();
  }

  @Override
  public void initializeLastTimestamp(ProfileTaskQueryKey profileTaskQueryKey,
                                      long value) {
    profileTaskQueryKeyMap.put(profileTaskQueryKey, value);
  }

  @Override
  public void setLastTimestamp(ProfileTaskQueryKey profileTaskQueryKey,
                               long value) {
    profileTaskQueryKeyMap.replace(profileTaskQueryKey, value);
  }

  @Override
  public long getLastTimestamp(ProfileTaskQueryKey profileTaskQueryKey) {
    return profileTaskQueryKeyMap.getOrDefault(profileTaskQueryKey, 0L);
  }

  @Override
  public void clear(ProfileTaskQueryKey profileTaskQueryKey) {
    profileTaskQueryKeyMap.remove(profileTaskQueryKey);
  }
}
