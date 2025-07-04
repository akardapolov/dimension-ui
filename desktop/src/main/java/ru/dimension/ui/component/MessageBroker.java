package ru.dimension.ui.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageBroker {
  public enum Component { WORKSPACE, DASHBOARD, REPORT, ADHOC}
  public enum Module { NONE, MODEL, CONFIG, CHARTS, CHART}
  public enum Panel { NONE, REALTIME, HISTORY, ANALYZE}
  public enum Block { NONE, CONFIG, CHART, DETAIL}

  public enum Action { ADD_CHART, REMOVE_CHART, REMOVE_ALL_CHART,

    SET_CHECKBOX_COLUMN, SET_CHECKBOX_METRIC,
    EXPAND_COLLAPSE_ALL, CHART_LEGEND_STATE_ALL,
    CHANGE_TAB,
    REALTIME_RANGE_CHANGE,
    HISTORY_RANGE_CHANGE,
    SHOW_HIDE_DETAIL_ALL
  }

  private final Map<Destination, List<MessageAction>> receivers = new HashMap<>();
  private static volatile MessageBroker instance;

  private MessageBroker() {
    for (Component dest : Component.values()) {
      receivers.put(Destination.withDefault(dest), new ArrayList<>());
    }
  }

  public static MessageBroker getInstance() {
    if (instance == null) {
      synchronized (MessageBroker.class) {
        if (instance == null) {
          instance = new MessageBroker();
        }
      }
    }
    return instance;
  }

  public void addReceiver(Destination destination, MessageAction receiver) {
    receivers.computeIfAbsent(destination, k -> new ArrayList<>()).add(receiver);
  }

  public void deleteReceiver(Destination destination, MessageAction receiver) {
    receivers.computeIfAbsent(destination, k -> new ArrayList<>()).remove(receiver);
  }

  public void sendMessage(Message message) {
    List<MessageAction> messageActions = receivers.get(message.destination());
    if (messageActions != null) {
      messageActions.forEach(receiver -> receiver.receive(message));
    }
  }
}