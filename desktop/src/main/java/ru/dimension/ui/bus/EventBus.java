package ru.dimension.ui.bus;

/**
 * Application event bus interface for publish/subscribe pattern.
 */
public interface EventBus {

  void subscribe(Object subscriber);

  void unsubscribe(Object subscriber);

  void publish(Object event);
}