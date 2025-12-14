package ru.dimension.ui.component.module.factory;

import javax.swing.JCheckBox;
import org.jdesktop.swingx.JXTaskPaneContainer;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.report.playground.MetricColumnPanel;
import ru.dimension.ui.model.ProfileTaskQueryKey;

public interface MetricColumnPanelFactory {
  MetricColumnPanel create(MessageBroker.Component component,
                           ProfileTaskQueryKey key,
                           JCheckBox collapseCard,
                           JXTaskPaneContainer container);
}