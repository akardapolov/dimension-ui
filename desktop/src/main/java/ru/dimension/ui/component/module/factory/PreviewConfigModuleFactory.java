package ru.dimension.ui.component.module.factory;

import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.preview.PreviewConfigModule;

public interface PreviewConfigModuleFactory {
  PreviewConfigModule create(MessageBroker.Component component);
}