package ru.dimension.ui.router.listener;

import ru.dimension.ui.model.view.ProgressbarState;

public interface ProgressbarListener {

  void fireProgressbarVisible(ProgressbarState progressbarState);
}
