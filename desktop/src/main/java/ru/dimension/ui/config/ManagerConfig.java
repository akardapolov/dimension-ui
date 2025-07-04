package ru.dimension.ui.config;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.ui.manager.impl.TemplateManagerImpl;
import ru.dimension.ui.manager.AdHocDatabaseManager;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ConnectionPoolManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.ReportManager;
import ru.dimension.ui.manager.TemplateManager;
import ru.dimension.ui.manager.impl.AdHocDatabaseManagerImpl;
import ru.dimension.ui.manager.impl.ConfigurationManagerImpl;
import ru.dimension.ui.manager.impl.ConnectionPoolManagerImpl;
import ru.dimension.ui.manager.impl.ProfileManagerImpl;
import ru.dimension.ui.manager.impl.ReportManagerImpl;

@Module
public abstract class ManagerConfig {

  @Binds
  @Named("profileManager")
  public abstract ProfileManager bindProfileManager(ProfileManagerImpl configurationManager);

  @Binds
  @Named("configurationManager")
  public abstract ConfigurationManager bindConfigurationManager(ConfigurationManagerImpl configurationManager);

  @Binds
  @Named("connectionPoolManager")
  public abstract ConnectionPoolManager bindConnectionPoolManager(ConnectionPoolManagerImpl connectionPoolManager);

  @Binds
  @Named("adHocDatabaseManager")
  public abstract AdHocDatabaseManager bindAdHocDatabaseManager(AdHocDatabaseManagerImpl adHocDatabaseManager);

  @Binds
  @Named("templateManager")
  public abstract TemplateManager bindTemplateManager(TemplateManagerImpl templateManager);

  @Binds
  @Named("reportManager")
  public abstract ReportManager bindReportManager(ReportManagerImpl reportManager);
}
