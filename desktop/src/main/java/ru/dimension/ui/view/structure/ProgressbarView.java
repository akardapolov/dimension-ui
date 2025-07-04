package ru.dimension.ui.view.structure;

import ru.dimension.ui.view.BaseView;
import ru.dimension.ui.model.view.ProgressbarState;

public interface ProgressbarView extends BaseView {

  void setProgressbarVisible(ProgressbarState progressbarState);
}
