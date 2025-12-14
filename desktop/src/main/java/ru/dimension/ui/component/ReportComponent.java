package ru.dimension.ui.component;

import jakarta.inject.Inject;
import javax.swing.JTabbedPane;
import lombok.Getter;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.module.factory.DesignModuleFactory;
import ru.dimension.ui.component.module.factory.PlaygroundModuleFactory;
import ru.dimension.ui.component.module.report.DesignModule;
import ru.dimension.ui.component.module.report.PlaygroundModule;

public class ReportComponent {
  @Getter
  private JTabbedPane mainTabPane;

  private final PlaygroundModuleFactory playgroundModuleFactory;
  private final DesignModuleFactory designModuleFactory;

  private PlaygroundModule playgroundModule;
  private DesignModule designModule;

  @Inject
  public ReportComponent(PlaygroundModuleFactory playgroundModuleFactory,
                         DesignModuleFactory designModuleFactory) {
    this.playgroundModuleFactory = playgroundModuleFactory;
    this.designModuleFactory = designModuleFactory;

    initializeComponents();
  }

  private void initializeComponents() {
    mainTabPane = new JTabbedPane();

    this.playgroundModule = playgroundModuleFactory.create();
    this.designModule = designModuleFactory.create();

    mainTabPane.addTab("Playground", this.playgroundModule.getView());
    mainTabPane.addTab("Design", this.designModule.getView());
  }
}