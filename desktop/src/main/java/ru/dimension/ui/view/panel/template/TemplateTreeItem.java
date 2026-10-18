package ru.dimension.ui.view.panel.template;

import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.type.ConnectionType;

public final class TemplateTreeItem {

  public enum Kind { GROUP, TEMPLATE }

  private final Kind kind;
  private final ConnectionType connectionType;
  private final Task task;
  private final Connection connection;
  private final int templateCount;

  private TemplateTreeItem(Kind kind, ConnectionType connectionType, Task task, Connection connection,
                           int templateCount) {
    this.kind = kind;
    this.connectionType = connectionType;
    this.task = task;
    this.connection = connection;
    this.templateCount = templateCount;
  }

  public static TemplateTreeItem group(ConnectionType type, int count) {
    return new TemplateTreeItem(Kind.GROUP, type, null, null, count);
  }

  public static TemplateTreeItem template(Task task, Connection connection) {
    return new TemplateTreeItem(Kind.TEMPLATE, connection.getType(), task, connection, 0);
  }

  public boolean isGroup() {
    return kind == Kind.GROUP;
  }

  public ConnectionType getConnectionType() {
    return connectionType;
  }

  public Task getTask() {
    return task;
  }

  public Connection getConnection() {
    return connection;
  }

  @Override
  public String toString() {
    if (isGroup()) {
      String label = connectionType == null ? "JDBC" : connectionType.getName();
      return label + " (" + templateCount + ")";
    }
    return task.getName();
  }
}