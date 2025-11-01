package ru.dimension.ui.view.structure;

import ru.dimension.ui.view.BaseView;
import ru.dimension.ui.model.view.ProgressbarState;
import ru.dimension.ui.view.structure.progressbar.ProgressbarPresenter;
import ru.dimension.ui.view.structure.template.TemplatePresenter;

public interface ProgressbarView extends BaseView {

  void bindPresenter(ProgressbarPresenter presenter);
  void setProgressbarVisible(ProgressbarState progressbarState);
}
