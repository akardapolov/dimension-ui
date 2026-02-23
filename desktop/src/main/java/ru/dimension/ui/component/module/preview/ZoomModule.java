package ru.dimension.ui.component.module.preview;

import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.db.core.DStore;
import ru.dimension.di.Assisted;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.preview.zoom.ZoomModel;
import ru.dimension.ui.component.module.preview.zoom.ZoomPresenter;
import ru.dimension.ui.component.module.preview.zoom.ZoomView;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.state.SqlQueryState;

@Log4j2
@Getter
public class ZoomModule {
  private final MessageBroker.Component component;

  private final ZoomModel model;
  private final ZoomView view;
  private final ZoomPresenter presenter;

  @Inject
  public ZoomModule(@Assisted MessageBroker.Component component,
                    ProfileManager profileManager,
                    SqlQueryState sqlQueryState,
                    DStore dStore) {
    this.component = component;

    this.model = new ZoomModel(component, profileManager, sqlQueryState, dStore);
    this.view = new ZoomView(model);
    this.presenter = new ZoomPresenter(model, view);
  }
}