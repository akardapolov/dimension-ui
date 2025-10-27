package ru.dimension.ui.component.broker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageBroker {
  public enum Component { WORKSPACE, PREVIEW, DASHBOARD, PLAYGROUND, DESIGN, ADHOC}
  public enum Module { MANAGE, MODEL, CONFIG, CHARTS, CHART, NONE}
  public enum Panel { REALTIME, HISTORY, INSIGHT, NONE}
  public enum Block { CONFIG, CHART, DETAIL, NONE}

  public enum Action { ADD_CHART, REMOVE_CHART, REMOVE_ALL_CHART,

    SET_CHECKBOX_COLUMN, SET_CHECKBOX_METRIC,
    EXPAND_COLLAPSE_ALL, CHART_LEGEND_STATE_ALL,
    SHOW_HIDE_CONFIG_ALL,
    CHANGE_TAB,
    REALTIME_RANGE_CHANGE,
    HISTORY_RANGE_CHANGE,
    HISTORY_CUSTOM_UI_RANGE_CHANGE,
    ADD_CHART_FILTER, REMOVE_CHART_FILTER,
    SET_PROFILE_TASK_QUERY_KEY,
    NEED_TO_SAVE_COMMENT,
    NEED_TO_SAVE_GROUP_FUNCTION,
    NEED_TO_UPDATE_LIST_DESIGN,
    SHOW_CHART_FULL
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