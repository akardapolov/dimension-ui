package ru.dimension.ui.component.broker;

import lombok.Builder;
import lombok.experimental.Accessors;
import ru.dimension.ui.component.broker.MessageBroker.Block;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.broker.MessageBroker.Panel;
import ru.dimension.ui.state.ChartKey;

@Builder
@Accessors(fluent = true)
public record Destination(Component component, Module module, Panel panel, Block block, ChartKey chartKey) {
  public static Destination withDefault(Component component) {
    return new Destination(component, Module.NONE, Panel.NONE, Block.NONE, null);
  }
  public static Destination withDefault(Component component, Module module) {
    return new Destination(component, module, Panel.NONE, Block.NONE, null);
  }

  public Destination withChartKey(ChartKey chartKey) {
    return new Destination(component(), module(), panel(), block(), chartKey);
  }
}
