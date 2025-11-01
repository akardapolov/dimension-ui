package ru.dimension.ui.view.structure;

import ru.dimension.ui.model.view.ToolbarButtonState;
import ru.dimension.ui.view.structure.toolbar.ToolbarPresenter;

public interface ToolbarView {
  void bindPresenter(ToolbarPresenter presenter);
  void setProfileButtonState(ToolbarButtonState toolbarButtonState);
}