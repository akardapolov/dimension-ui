package ru.dimension.ui.component;

import lombok.Builder;
import lombok.experimental.Accessors;
import ru.dimension.ui.component.MessageBroker.Block;
import ru.dimension.ui.component.MessageBroker.Component;
import ru.dimension.ui.component.MessageBroker.Module;
import ru.dimension.ui.component.MessageBroker.Panel;

@Builder
@Accessors(fluent = true)
public record Destination(Component component, Module module, Panel panel, Block block) {
  public static Destination withDefault(Component component) {
    return new Destination(component, Module.NONE, Panel.NONE, Block.NONE);
  }
  public static Destination withDefault(Component component, Module module) {
    return new Destination(component, module, Panel.NONE, Block.NONE);
  }
}
