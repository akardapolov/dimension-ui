package ru.dimension.ui.component.module.preview;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Module;
import ru.dimension.ui.component.model.DetailState;
import ru.dimension.ui.component.module.preview.charts.PreviewChartsModel;
import ru.dimension.ui.component.module.preview.charts.PreviewChartsPresenter;
import ru.dimension.ui.component.module.preview.charts.PreviewChartsView;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.state.SqlQueryState;
import ru.dimension.ui.state.UIState;

@Log4j2
@Getter
public class PreviewChartsModule {
  private final MessageBroker.Component component;

  private final PreviewChartsModel model;
  private final PreviewChartsView view;
  private final PreviewChartsPresenter presenter;

  private final MessageBroker broker = MessageBroker.getInstance();

  public PreviewChartsModule(MessageBroker.Component component,
                             ProfileManager profileManager,
                             SqlQueryState sqlQueryState,
                             DStore dStore) {
    this.component = component;

    this.model = new PreviewChartsModel(profileManager, sqlQueryState, dStore);
    this.view = new PreviewChartsView(model);
    this.presenter = new PreviewChartsPresenter(component, model, view);

    UIState.INSTANCE.putShowDetailAll(component.name(), DetailState.SHOW);

    broker.addReceiver(Destination.withDefault(component, Module.CHARTS), presenter);
  }
}
