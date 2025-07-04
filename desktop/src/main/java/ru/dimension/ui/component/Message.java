package ru.dimension.ui.component;

import lombok.Builder;
import lombok.experimental.Accessors;
import ru.dimension.ui.component.MessageBroker.Action;

@Builder
@Accessors(fluent = true)
public record Message(Destination destination, Action action, ParameterStore parameters) {

  public static class MessageBuilder {
    private ParameterStore parameters = new ParameterStore();

    public MessageBuilder parameters(ParameterStore parameters) {
      if (parameters != null) {
        this.parameters = parameters;
      }
      return this;
    }

    public MessageBuilder parameter(String key, Object value) {
      this.parameters.put(key, value);
      return this;
    }
  }
}
