package ru.dimension.ui.router.listener;

import ru.dimension.ui.model.ProfileTaskQueryKey;

public interface AppCacheAddListener {

  void fireOnAddToAppCache(ProfileTaskQueryKey profileTaskQueryKey);
}
