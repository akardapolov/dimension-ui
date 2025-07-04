package ru.dimension.ui.state;

import ru.dimension.ui.model.ProfileTaskQueryKey;

public interface SqlQueryState {

  void initializeLastTimestamp(ProfileTaskQueryKey profileTaskQueryKey,
                               long value);

  void setLastTimestamp(ProfileTaskQueryKey profileTaskQueryKey,
                        long value);

  long getLastTimestamp(ProfileTaskQueryKey profileTaskQueryKey);

  void clear(ProfileTaskQueryKey profileTaskQueryKey);
}
