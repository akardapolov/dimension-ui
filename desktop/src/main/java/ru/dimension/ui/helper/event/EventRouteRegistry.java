package ru.dimension.ui.helper.event;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.engio.mbassy.listener.Handler;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.component.broker.MessageBroker;

public class EventRouteRegistry {

  private final MessageBroker.Component targetComponent;
  private final Function<Object, MessageBroker.Component> componentExtractor;
  private final Map<Class<?>, Consumer<Object>> routes = new HashMap<>();
  private final Map<Class<?>, Boolean> scopeChecks = new HashMap<>();

  private EventRouteRegistry(MessageBroker.Component targetComponent,
                             Function<Object, MessageBroker.Component> componentExtractor) {
    this.targetComponent = targetComponent;
    this.componentExtractor = componentExtractor;
  }

  public static EventRouteRegistry forComponent(MessageBroker.Component component,
                                                Function<Object, MessageBroker.Component> extractor) {
    return new EventRouteRegistry(component, extractor);
  }

  /**
   * Registers a route for a Component-Scoped event (checks targetComponent).
   * @param eventType The class of the event
   * @param handler The method reference (e.g., this::handleAddChart)
   */
  public <T> EventRouteRegistry route(Class<T> eventType, Consumer<T> handler) {
    routes.put(eventType, obj -> handler.accept(eventType.cast(obj)));
    scopeChecks.put(eventType, true);
    return this;
  }

  /**
   * Registers a route for a Global event (ignores targetComponent).
   */
  public <T> EventRouteRegistry routeGlobal(Class<T> eventType, Consumer<T> handler) {
    routes.put(eventType, obj -> handler.accept(eventType.cast(obj)));
    scopeChecks.put(eventType, false);
    return this;
  }

  /**
   * Subscribes this registry to the EventBus.
   * NOTE: The caller (Presenter) MUST hold a reference to this object
   * to prevent it from being Garbage Collected (MBassador uses WeakReferences).
   */
  public EventRouteRegistry register(EventBus eventBus) {
    eventBus.subscribe(this);
    return this;
  }

  @Handler
  public void onEvent(Object event) {
    if (event == null) return;

    Class<?> type = event.getClass();

    if (!routes.containsKey(type)) {
      return;
    }

    if (scopeChecks.get(type)) {
      MessageBroker.Component eventComponent = componentExtractor.apply(event);
      if (eventComponent != null && eventComponent != targetComponent) {
        return;
      }
    }

    routes.get(type).accept(event);
  }
}