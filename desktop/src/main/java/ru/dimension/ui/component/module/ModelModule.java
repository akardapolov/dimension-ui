package ru.dimension.ui.component.module;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.component.module.model.ModelModel;
import ru.dimension.ui.component.module.model.ModelPresenter;
import ru.dimension.ui.component.module.model.ModelView;
import ru.dimension.ui.component.Message;
import ru.dimension.ui.component.MessageAction;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.column.ColumnNames;

@Log4j2
public class ModelModule implements MessageAction {

  private final ModelModel model;
  @Getter
  private final ModelView view;
  private final ModelPresenter presenter;

  public ModelModule(ProfileManager profileManager) {
    model = new ModelModel(profileManager);
    view = new ModelView();
    presenter = new ModelPresenter(model, view);

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

  @Override
  public void receive(Message message) {
    switch (message.action()) {
      case SET_CHECKBOX_COLUMN -> {
        log.info("Set checkbox for column: ");
      }
    }
  }

}