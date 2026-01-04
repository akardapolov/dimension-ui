package ru.dimension.ui.component.module;

import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.di.Assisted;
import ru.dimension.ui.bus.EventBus;
import ru.dimension.ui.bus.event.ProfileAddEvent;
import ru.dimension.ui.bus.event.ProfileRemoveEvent;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.model.ModelModel;
import ru.dimension.ui.component.module.model.ModelPresenter;
import ru.dimension.ui.component.module.model.ModelView;
import ru.dimension.ui.helper.event.EventRouteRegistry;
import ru.dimension.ui.helper.event.EventUtils;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;
import ru.dimension.ui.view.table.row.Rows.QueryRow;
import ru.dimension.ui.view.table.row.Rows.TaskRow;

@Log4j2
public class ModelModule {

  private final ModelModel model;
  @Getter
  private final ModelView view;
  private final ModelPresenter presenter;

  @SuppressWarnings("FieldCanBeLocal")
  private final EventRouteRegistry eventRegistry;

  @Inject
  public ModelModule(@Assisted MessageBroker.Component component,
                     ProfileManager profileManager,
                     EventBus eventBus) {
    model = new ModelModel(profileManager, eventBus);
    view = new ModelView();
    presenter = new ModelPresenter(component, model, view);

    this.eventRegistry = EventRouteRegistry.forComponent(component, EventUtils::getComponent)
        .routeGlobal(ProfileAddEvent.class, this::fireProfileAdd)
        .routeGlobal(ProfileRemoveEvent.class, this::handleProfileRemove)
        .register(eventBus);

    setupSelectionListeners();

    presenter.initializeModel();
  }

  private void setupSelectionListeners() {
    // Profile Selection
    view.getProfileTable().table().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        view.getProfileTable().selectedItem()
            .ifPresent(row -> presenter.handleProfileSelection(row.getId()));
      }
    });

    // Task Selection
    view.getTaskTable().table().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        view.getTaskTable().selectedItem()
            .ifPresent(row -> presenter.handleTaskSelection(row.getId()));
      }
    });

    // Query Selection
    view.getQueryTable().table().getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting() && view.getQueryTable().selectedItem().isPresent()) {
       QueryRow query = view.getQueryTable().selectedItem().get();
        ProfileRow profile = view.getProfileTable().selectedItem().orElse(null);
        TaskRow task = view.getTaskTable().selectedItem().orElse(null);

        if (profile != null && task != null) {
          presenter.handleQuerySelection(profile.getId(), task.getId(), query.getId());
        }
      }
    });
  }

  public void fireProfileAdd(ProfileAddEvent event) {
    presenter.initializePartialModel();
  }

  public void handleProfileRemove(ProfileRemoveEvent event) {
    presenter.handleProfileRemoval(event.profileId());
  }
}