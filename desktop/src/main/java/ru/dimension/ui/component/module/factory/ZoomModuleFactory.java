package ru.dimension.ui.component.module.factory;

import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.preview.ZoomModule;

public interface ZoomModuleFactory {
  ZoomModule create(MessageBroker.Component component);
}