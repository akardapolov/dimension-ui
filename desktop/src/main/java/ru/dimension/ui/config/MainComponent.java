package ru.dimension.ui.config;

import dagger.Component;
import javax.inject.Singleton;
import ru.dimension.ui.config.presenter.PresenterConfig;
import ru.dimension.ui.view.BaseFrame;
import ru.dimension.ui.config.view.BaseFrameConfig;
import ru.dimension.ui.config.view.ConfigurationConfig;
import ru.dimension.ui.config.view.PanelConfig;
import ru.dimension.ui.config.view.ToolbarConfig;
import ru.dimension.ui.config.view.ViewConfig;

@Component(modules = {
    CollectorConfig.class,
    ExecutorConfig.class,
    BaseFrameConfig.class,
    ViewConfig.class,
    PanelConfig.class,
    ToolbarConfig.class,
    PresenterConfig.class,
    HandlerConfig.class,
    ConfigurationConfig.class,
    RouterConfig.class,
    StateConfig.class,
    ManagerConfig.class,
    FileConfig.class,
    LocalDBConfig.class,
    CacheConfig.class,
    SecurityConfig.class,
    ParserHttpConfig.class
})
@Singleton
public interface MainComponent {

  BaseFrame createBaseFrame();
}