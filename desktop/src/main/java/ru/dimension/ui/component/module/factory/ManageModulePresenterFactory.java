package ru.dimension.ui.component.module.factory;

import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.module.manage.ManagePresenter;
import ru.dimension.ui.component.module.manage.ManageView;

public interface ManageModulePresenterFactory {
  ManagePresenter create(MessageBroker.Component component, ManageView manageView);
}
