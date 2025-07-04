package ru.dimension.ui.view.analyze.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageRouter {
  public enum Destination { DIMENSION, TOP, CHART_LIST, CHART_CONFIG}
  public enum Action { ADD_CHART, REMOVE_CHART, REMOVE_ALL_CHART,
    ADD_CHART_FILTER, REMOVE_CHART_FILTER,
    SET_CHART_LEGEND_STATE, SET_CHART_CARD_STATE,
    SET_FILTER, SET_CHECKBOX_CHART, CLEAR_ALL_CHECKBOX_CHART,
    SET_BEGIN_END,
    LOAD_TOP, CLEAR_TOP }

  private final Map<Destination, List<MessageAction>> receivers = new HashMap<>();

  public MessageRouter() {
    for (Destination dest : Destination.values()) {
      receivers.put(dest, new ArrayList<>());
    }
  }

  public void registerReceiver(Destination destination, MessageAction receiver) {
    receivers.computeIfAbsent(destination, k -> new ArrayList<>()).add(receiver);
  }

  public void sendMessage(Message message) {
    receivers.getOrDefault(message.getDestination(), List.of())
        .forEach(receiver -> receiver.receive(message));
  }

  public void clearRegistration() {
    receivers.clear();
  }
}