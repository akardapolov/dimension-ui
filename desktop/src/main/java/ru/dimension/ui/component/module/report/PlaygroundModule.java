package ru.dimension.ui.component.module.report;

import lombok.Getter;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker.Component;
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

  public PlaygroundModule(ProfileManager profileManager,
                          ConfigurationManager configurationManager,
                          ReportManager reportManager,
                          FilesHelper filesHelper,
                          DStore dStore) {
    Component component = Component.PLAYGROUND;
    this.model = new PlaygroundModel(component,
                                     profileManager,
                                     configurationManager,
                                     reportManager,
                                     filesHelper,
                                     dStore);
    this.view = new PlaygroundView(model);
    this.presenter = new PlaygroundPresenter(model, view);
  }
}