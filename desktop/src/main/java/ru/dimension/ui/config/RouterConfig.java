package ru.dimension.ui.config;

import dagger.Binds;
import dagger.Module;
import javax.inject.Named;
import ru.dimension.ui.router.Router;
import ru.dimension.ui.router.RouterImpl;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.event.EventListenerImpl;

@Module
public abstract class RouterConfig {

  @Binds
  @Named("router")
  public abstract Router bindRouter(RouterImpl router);

  @Binds
  @Named("eventListener")
  public abstract EventListener bindEventListener(EventListenerImpl eventListener);
}
