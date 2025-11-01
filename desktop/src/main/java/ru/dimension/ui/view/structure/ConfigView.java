package ru.dimension.ui.view.structure;

import ru.dimension.ui.view.BaseView;
import ru.dimension.ui.view.structure.config.ConfigPresenter;

public interface ConfigView extends BaseView {

  void bindPresenter(ConfigPresenter presenter);
  void showConfig(int id);
  void hideProfile();
}
