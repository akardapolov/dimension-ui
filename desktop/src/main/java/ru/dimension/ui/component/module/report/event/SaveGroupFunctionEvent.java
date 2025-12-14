package ru.dimension.ui.component.module.report.event;

import lombok.Value;
import ru.dimension.db.model.profile.CProfile;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.function.GroupFunction;

@Value
public class SaveGroupFunctionEvent {
  Component component;
  ProfileTaskQueryKey key;
  CProfile cProfile;
  GroupFunction groupFunction;
}