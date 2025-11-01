package ru.dimension.ui.view.structure;

import ru.dimension.ui.view.BaseView;
import ru.dimension.ui.view.structure.template.TemplatePresenter;

public interface TemplateView extends BaseView {

  void bindPresenter(TemplatePresenter presenter);
  void showTemplate();
  void hideTemplate();
}
