package ru.dimension.ui.config.core;

import ru.dimension.di.DimensionDI;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.router.Router;
import ru.dimension.ui.router.RouterImpl;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.router.event.EventListenerImpl;
import ru.dimension.ui.security.EncryptDecrypt;
import ru.dimension.ui.state.NavigatorState;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.impl.NavigatorStateImpl;
import ru.dimension.ui.state.impl.SqlQueryStateImpl;

public final class RoutingSecurityStateConfig {

  private RoutingSecurityStateConfig() {
  }

  public static void configure(DimensionDI.Builder builder) {
    builder
        // Router
        .bindNamed(Router.class, "router", RouterImpl.class)
        .bindNamed(EventListener.class, "eventListener", EventListenerImpl.class)

        // Security
        .provideNamed(EncryptDecrypt.class, "encryptDecrypt", ServiceLocator.singleton(EncryptDecrypt::new))

        // State
        .bindNamed(NavigatorState.class, "navigatorState", NavigatorStateImpl.class)
        .bindNamed(SqlQueryState.class, "sqlQueryState", SqlQueryStateImpl.class);
  }
}