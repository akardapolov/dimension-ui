package ru.dimension.ui;

import dagger.Component;
import javax.inject.Singleton;
import ru.dimension.ui.config.CacheConfig;
import ru.dimension.ui.config.CollectorConfig;
import ru.dimension.ui.config.ExecutorConfig;
import ru.dimension.ui.config.LocalDBConfig;
import ru.dimension.ui.config.MainComponent;
import ru.dimension.ui.config.ManagerTestConfig;
import ru.dimension.ui.config.ParserHttpConfig;
import ru.dimension.ui.config.RouterConfig;
import ru.dimension.ui.config.StateConfig;
import ru.dimension.ui.config.presenter.PresenterConfig;
import ru.dimension.ui.config.FileConfig;
import ru.dimension.ui.config.HandlerConfig;
import ru.dimension.ui.config.SecurityConfig;
import ru.dimension.ui.config.view.BaseFrameConfig;
import ru.dimension.ui.config.view.ConfigurationConfig;
import ru.dimension.ui.config.view.NavigatorConfig;
import ru.dimension.ui.config.view.PanelConfig;
import ru.dimension.ui.config.view.ToolbarConfig;
import ru.dimension.ui.config.view.ViewConfig;

@Component(modules = {
    CollectorConfig.class,
    ExecutorConfig.class,
    BaseFrameConfig.class,
    ViewConfig.class,
    PanelConfig.class,
    NavigatorConfig.class,
    ToolbarConfig.class,
    PresenterConfig.class,
    HandlerConfig.class,
    ConfigurationConfig.class,
    RouterConfig.class,
    StateConfig.class,
    ManagerTestConfig.class,
    FileConfig.class,
    LocalDBConfig.class,
    CacheConfig.class,
    SecurityConfig.class,
    ParserHttpConfig.class
})
@Singleton
public interface MainComponentTest extends MainComponent {

  void inject(HandlerMock handlerMock);
}
