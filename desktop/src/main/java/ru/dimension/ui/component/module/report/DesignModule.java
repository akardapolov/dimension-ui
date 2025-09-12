package ru.dimension.ui.component.module.report;

import lombok.Getter;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.module.report.design.DesignModel;
import ru.dimension.ui.component.module.report.design.DesignPresenter;
import ru.dimension.ui.component.module.report.design.DesignView;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.ReportManager;

public class DesignModule {
  private final DesignModel model;
  @Getter
  private final DesignView view;
  @Getter
  private final DesignPresenter presenter;

  public DesignModule(ProfileManager profileManager,
                      ConfigurationManager configurationManager,
                      ReportManager reportManager,
                      FilesHelper filesHelper,
                      DStore dStore) {
    Component component = Component.DESIGN;
    this.model = new DesignModel(component, profileManager, configurationManager, reportManager, filesHelper, dStore);
    this.view = new DesignView(model);
    this.presenter = new DesignPresenter(model, view);
  }
}