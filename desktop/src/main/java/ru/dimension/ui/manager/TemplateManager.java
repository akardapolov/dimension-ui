package ru.dimension.ui.manager;

import java.util.List;
import ru.dimension.ui.model.config.Query;

public interface TemplateManager {

  <T> List<T> getConfigList(Class<T> clazz);

  List<Query> getQueryListByConnDriver(String connDriver);
}
