package ru.dimension.ui.component.module.factory;

import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.ModelModule;

public interface ModelModuleFactory {
  ModelModule create(MessageBroker.Component component);
}