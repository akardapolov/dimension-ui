package ru.dimension.ui.component.module;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.module.charts.ChartsModel;
import ru.dimension.ui.component.module.charts.ChartsPresenter;
import ru.dimension.ui.component.module.charts.ChartsView;
import ru.dimension.ui.component.Destination;
import ru.dimension.ui.component.MessageBroker;
import ru.dimension.ui.component.MessageBroker.Component;
import ru.dimension.ui.component.MessageBroker.Module;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
public class ChartsModule {

  private final ChartsModel model;
  @Getter
  private final ChartsView view;
  @Getter
  private final ChartsPresenter presenter;

  private final MessageBroker broker = MessageBroker.getInstance();

  public ChartsModule(ProfileManager profileManager,
                      SqlQueryState sqlQueryState,
                      DStore dStore) {

    this.model = new ChartsModel(profileManager, sqlQueryState, dStore);

    this.view = new ChartsView();
    this.presenter = new ChartsPresenter(model, view);

    broker.addReceiver(Destination.withDefault(Component.DASHBOARD, Module.CHARTS), presenter);
  }
}
