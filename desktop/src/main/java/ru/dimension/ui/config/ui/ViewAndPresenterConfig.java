package ru.dimension.ui.config.ui;

import ru.dimension.di.DimensionDI;
import ru.dimension.di.ServiceLocator;
import ru.dimension.ui.view.structure.ConfigView;
import ru.dimension.ui.view.structure.ProgressbarView;
import ru.dimension.ui.view.structure.TemplateView;
import ru.dimension.ui.view.structure.ToolbarView;
import ru.dimension.ui.view.structure.config.ConfigPresenter;
import ru.dimension.ui.view.structure.config.ConfigViewImpl;
import ru.dimension.ui.view.structure.progressbar.ProgressbarPresenter;
import ru.dimension.ui.view.structure.progressbar.ProgressbarViewImpl;
import ru.dimension.ui.view.structure.template.TemplatePresenter;
import ru.dimension.ui.view.structure.template.TemplateViewImpl;
import ru.dimension.ui.view.structure.toolbar.ToolbarPresenter;
import ru.dimension.ui.view.structure.toolbar.ToolbarViewImpl;

public final class ViewAndPresenterConfig {

  private ViewAndPresenterConfig() {
  }

  public static void configure(DimensionDI.Builder builder) {
    builder
        // Views
        .bindNamed(ToolbarView.class, "toolbarView", ToolbarViewImpl.class)
        .bindNamed(ConfigView.class, "configView", ConfigViewImpl.class)
        .bindNamed(TemplateView.class, "templateView", TemplateViewImpl.class)
        .bindNamed(ProgressbarView.class, "progressbarView", ProgressbarViewImpl.class)

        // Presenters (resolution via ServiceLocator)
        .provideNamed(ToolbarPresenter.class, "toolbarPresenter", () -> ServiceLocator.get(ToolbarPresenter.class))
        .provideNamed(ProgressbarPresenter.class, "progressbarPresenter", () -> ServiceLocator.get(ProgressbarPresenter.class))
        .provideNamed(ConfigPresenter.class, "configPresenter", () -> ServiceLocator.get(ConfigPresenter.class))
        .provideNamed(TemplatePresenter.class, "templatePresenter", () -> ServiceLocator.get(TemplatePresenter.class));
  }
}