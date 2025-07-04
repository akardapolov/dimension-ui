package ru.dimension.ui.router.listener;

import ru.dimension.ui.model.view.ToolbarButtonState;

public interface ToolbarListener {

  void fireToolbarButtonStateChange(ToolbarButtonState toolbarButtonState);
}
