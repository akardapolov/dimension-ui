package ru.dimension.ui.component.module.factory;

import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.ChartsModule;

public interface ChartsModuleFactory {
  ChartsModule create(MessageBroker.Component component);
}
