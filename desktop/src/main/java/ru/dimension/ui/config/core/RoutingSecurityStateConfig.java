package ru.dimension.ui.config.core;

import ru.dimension.di.DimensionDI;
import ru.dimension.ui.router.Router;
import ru.dimension.ui.router.RouterImpl;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.EventBusImpl;
import ru.dimension.ui.router.event.EventDispatcher;
import ru.dimension.ui.router.event.EventDispatcherImpl;
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
        .bindNamed(EventDispatcher.class, "eventListener", EventDispatcherImpl.class)

        // MBassador Event Bus
        .bindNamed(EventBus.class, "eventBus", EventBusImpl.class)

        // Security
        .bindNamed(EncryptDecrypt.class, "encryptDecrypt", EncryptDecrypt.class)

        // State
        .bindNamed(NavigatorState.class, "navigatorState", NavigatorStateImpl.class)
        .bindNamed(SqlQueryState.class, "sqlQueryState", SqlQueryStateImpl.class);
  }
}