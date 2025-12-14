package ru.dimension.ui.component.module.factory;

import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.ConfigModule;

public interface ConfigModuleFactory {
  ConfigModule create(MessageBroker.Component component);
}
