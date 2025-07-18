package ru.dimension.ui.view.analyze.router;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.dimension.ui.component.broker.ParameterStore;
import ru.dimension.ui.view.analyze.router.MessageRouter.Action;
import ru.dimension.ui.view.analyze.router.MessageRouter.Destination;

@AllArgsConstructor
@Builder
@Data
public class Message {
  private final Destination destination;
  private final Action action;
  private final ParameterStore parameters;

  public static class MessageBuilder {
    private ParameterStore parameters = new ParameterStore();

    public MessageBuilder parameter(String key, Object value) {
      this.parameters.put(key, value);
      return this;
    }
  }
}
