package ru.dimension.ui.component.module.factory;

import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.ManageModule;

public interface ManageModuleFactory {
  ManageModule create(MessageBroker.Component component);
}
