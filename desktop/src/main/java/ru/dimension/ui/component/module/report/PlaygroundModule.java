package ru.dimension.ui.component.module.report;

import jakarta.inject.Inject;
import lombok.Getter;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.module.factory.MetricColumnPanelFactory;
import ru.dimension.ui.component.module.report.playground.PlaygroundModel;
import ru.dimension.ui.component.module.report.playground.PlaygroundPresenter;
import ru.dimension.ui.component.module.report.playground.PlaygroundView;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.ReportManager;

public class PlaygroundModule {

  private final PlaygroundModel model;
  @Getter
  private final PlaygroundView view;
  @Getter
  private final PlaygroundPresenter presenter;

  @Inject
  public PlaygroundModule(ProfileManager profileManager,
                          ConfigurationManager configurationManager,
                          ReportManager reportManager,
                          FilesHelper filesHelper,
                          EventBus eventBus,
                          DStore dStore,
                          MetricColumnPanelFactory metricColumnPanelFactory) {
    Component component = Component.PLAYGROUND;
    this.model = new PlaygroundModel(component,
                                     profileManager,
                                     configurationManager,
                                     reportManager,
                                     filesHelper,
                                     dStore);
    this.view = new PlaygroundView(model);
    this.presenter = new PlaygroundPresenter(model, view, eventBus, metricColumnPanelFactory);
  }
}