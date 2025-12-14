package ru.dimension.ui.component.module.factory;

import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.preview.PreviewChartsModule;

public interface PreviewChartsModuleFactory {
  PreviewChartsModule create(MessageBroker.Component component);
}