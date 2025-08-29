package ru.dimension.ui.config;

import dagger.Module;
import dagger.Provides;
import javax.inject.Named;
import javax.inject.Singleton;
import ru.dimension.ui.helper.ColorHelper;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.manager.ConfigurationManager;

@Module
public class HelperConfig {

  @Provides
  @Singleton
  ColorHelper provideColorHelper(FilesHelper filesHelper,
                                 @Named("configurationManager") ConfigurationManager configurationManager) {
    return new ColorHelper(filesHelper, configurationManager);
  }
}
