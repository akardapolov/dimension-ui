package ru.dimension.ui.bus;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;

@Log4j2
@Singleton
public class EventBusImpl implements EventBus {

  private final MBassador<Object> mBassador;

  @Inject
  public EventBusImpl() {
    this.mBassador = new MBassador<>(new BusConfiguration()
                                         .addFeature(Feature.SyncPubSub.Default())
                                         .addFeature(Feature.AsynchronousHandlerInvocation.Default())
                                         .addFeature(Feature.AsynchronousMessageDispatch.Default())
                                         .addPublicationErrorHandler(new LoggingPublicationErrorHandler()));

    log.info("MBassador event bus initialized");
  }

  @Override
  public void subscribe(Object subscriber) {
    mBassador.subscribe(subscriber);
    log.debug("Subscribed: {}", subscriber.getClass().getSimpleName());
  }

  @Override
  public void unsubscribe(Object subscriber) {
    mBassador.unsubscribe(subscriber);
    log.debug("Unsubscribed: {}", subscriber.getClass().getSimpleName());
  }

  @Override
  public void publish(Object event) {
    log.debug("Publishing event: {}", event.getClass().getSimpleName());
    mBassador.publish(event);
  }
}