package ru.dimension.ui.model.config;

public enum ConfigClasses {
  Profile(Profile.class),
  Task(Task.class),
  Connection(Connection.class),
  Query(Query.class),
  Table(Table.class),
  Unknown(null);

  private final Class<?> targetClass;

  ConfigClasses(Class<?> targetClass) {
    this.targetClass = targetClass;
  }

  public static ConfigClasses fromClass(Class<?> cls) {
    for (ConfigClasses c : values()) {
      if (c.targetClass == cls) {
        return c;
      }
    }
    return Unknown;
  }
}
