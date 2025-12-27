package ru.dimension.ui.component.module.model;

import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Metric;
import ru.dimension.db.model.profile.CProfile;

import java.util.*;

public class ModelModel {

  private final ProfileManager profileManager;
  private final EventBus eventBus;
  private final Map<ProfileTaskQueryKey, Map.Entry<List<Metric>, List<CProfile>>> queryKeyMap;

  public ModelModel(ProfileManager profileManager,
                    EventBus eventBus) {
    this.profileManager = profileManager;
    this.eventBus = eventBus;
    this.queryKeyMap = new HashMap<>();
  }

  public ProfileManager getProfileManager() {
    return profileManager;
  }

  public Map<ProfileTaskQueryKey, Map.Entry<List<Metric>, List<CProfile>>> getQueryKeyMap() {
    return queryKeyMap;
  }

  public void clearCacheForProfile(int profileId) {
    queryKeyMap.entrySet().removeIf(entry -> entry.getKey().getProfileId() == profileId);
  }
}