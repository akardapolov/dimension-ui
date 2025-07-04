package ru.dimension.ui.model.info;

import lombok.Data;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.EntityList;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;

@Data
public class ConfigInfo {

  EntityList<Profile> profiles;
  EntityList<Task> tasks;
  EntityList<Connection> connections;
  EntityList<Query> queries;
}
