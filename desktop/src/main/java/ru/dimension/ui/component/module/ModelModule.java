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
import ru.dimension.ui.model.column.ColumnNames;

@Log4j2
public class ModelModule {

  private final ModelModel model;
  @Getter
  private final ModelView view;
  private final ModelPresenter presenter;

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
    view.getProfileTableCase().getJxTable().getSelectionModel().addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return;
      }
      int selectedRow = view.getProfileTableCase().getJxTable().getSelectedRow();
      if (selectedRow >= 0) {
        int profileId = (int) view.getProfileTableCase().getDefaultTableModel()
            .getValueAt(selectedRow, ColumnNames.ID.ordinal());
        presenter.handleProfileSelection(profileId);
      }
    });

    view.getTaskTableCase().getJxTable().getSelectionModel().addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return;
      }
      int selectedRow = view.getTaskTableCase().getJxTable().getSelectedRow();
      if (selectedRow >= 0) {
        int taskId = (int) view.getTaskTableCase().getDefaultTableModel()
            .getValueAt(selectedRow, ColumnNames.ID.ordinal());
        presenter.handleTaskSelection(taskId);
      }
    });

    view.getQueryTableCase().getJxTable().getSelectionModel().addListSelectionListener(e -> {
      if (e.getValueIsAdjusting()) {
        return;
      }
      int selectedRow = view.getQueryTableCase().getJxTable().getSelectedRow();
      if (selectedRow >= 0) {
        int profileId = (int) view.getProfileTableCase().getDefaultTableModel()
            .getValueAt(view.getProfileTableCase().getJxTable().getSelectedRow(), ColumnNames.ID.ordinal());
        int taskId = (int) view.getTaskTableCase().getDefaultTableModel()
            .getValueAt(view.getTaskTableCase().getJxTable().getSelectedRow(), ColumnNames.ID.ordinal());
        int queryId = (int) view.getQueryTableCase().getDefaultTableModel()
            .getValueAt(selectedRow, ColumnNames.ID.ordinal());
        presenter.handleQuerySelection(profileId, taskId, queryId);
      }
    });
  }

  public void fireProfileAdd(ProfileAddEvent event) {
    log.info("Received {} via MBassador in ModelModule", event);
    presenter.initializePartialModel();
  }

  public void handleProfileRemove(ProfileRemoveEvent event) {
    log.info("ModelModule received ProfileRemoveEvent: profileId={}", event.profileId());
    presenter.handleProfileRemoval(event.profileId());
  }
}