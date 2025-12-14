package ru.dimension.ui.component.module.factory;

import ru.dimension.di.Assisted;
import ru.dimension.ui.component.module.PreviewModule;
import ru.dimension.ui.component.module.preview.spi.PreviewMode;
import ru.dimension.ui.model.ProfileTaskQueryKey;

public interface PreviewModuleFactory {
  PreviewModule create(@Assisted PreviewMode mode, @Assisted ProfileTaskQueryKey key);
}