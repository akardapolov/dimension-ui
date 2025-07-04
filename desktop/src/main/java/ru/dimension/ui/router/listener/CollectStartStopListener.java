package ru.dimension.ui.router.listener;

import ru.dimension.ui.model.ProfileTaskQueryKey;

public interface CollectStartStopListener {

  void fireOnStartCollect(ProfileTaskQueryKey profileTaskQueryKey);

  void fireOnStopCollect(ProfileTaskQueryKey profileTaskQueryKey);
}
