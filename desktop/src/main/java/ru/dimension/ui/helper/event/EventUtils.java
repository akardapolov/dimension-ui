package ru.dimension.ui.helper.event;

import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.report.event.*;

public class EventUtils {

  public static MessageBroker.Component getComponent(Object event) {
    return switch (event) {
      case AddChartEvent e          -> e.getComponent();
      case RemoveChartEvent e       -> e.getComponent();
      case SaveCommentEvent e       -> e.getComponent();
      case SaveGroupFunctionEvent e -> e.getComponent();
      default -> null;
    };
  }
}