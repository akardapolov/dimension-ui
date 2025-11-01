package ru.dimension.ui.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Isolated;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.HandlerMock;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.view.tab.ConnectionTypeTabPane;


@Log4j2
@Isolated
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskEditHandlerTest extends HandlerMock {

  Connection connectionExpectedNew;

  @BeforeAll
  public void setUpLocal() throws IOException {
    // Query
    String queryName = "Test";
    String queryDescription = "Description Test";

    Query query = new Query();
    query.setName(queryName);
    query.setDescription(queryDescription);

    createQueryTest(query);

    // Connection
    connectionExpectedNew = objectMapper.readValue(getTestData("connection_new_edit.json"), Connection.class);
    String passEncNew = encryptDecryptLazy.get().encrypt(connectionExpectedNew.getPassword());
    connectionExpectedNew.setPassword(passEncNew);

    createConnectionTest(connectionExpectedNew);
  }

  @Test
  @Order(1)
  public void create_new_task_test() {
    assertNotNull(taskButtonPanelHandlerLazy.get());
    assertNotNull(taskPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(connectionButtonPanelHandlerLazy.get());
    assertNotNull(connectionPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(configViewLazy.get());

    createNewConnectionTestData();

    String taskName = "Test";
    String taskDescription = "Description Test";

    Task task = new Task();
    task.setName(taskName);
    task.setDescription(taskDescription);
    task.setConnectionId(connectionExpectedNew.getId());

    List<Connection> connectionAll = configurationManagerLazy.get().getConfigList(Connection.class);
    List<List<?>> connectionDataList;
    connectionDataList = new LinkedList<>();
    connectionAll.forEach(connection -> connectionDataList.add(
        new ArrayList<>(Arrays.asList(connection.getName(), connection.getUserName(),
                                      connection.getUrl(), connection.getJar(), connection.getDriver()))));
    this.taskPanelLazy.get().getTaskConnectionComboBox().setTableData(connectionDataList);

    createTaskTest(task);

    assertEquals(taskName, getTaskName(taskName));
    assertEquals(taskDescription, getTaskDescription(taskName));
  }

  @Test
  @Order(2)
  public void copy_new_task_test() {
    assertNotNull(taskButtonPanelHandlerLazy.get());
    assertNotNull(taskPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());

    assertNotNull(connectionButtonPanelHandlerLazy.get());
    assertNotNull(connectionPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(configViewLazy.get());

    createNewConnectionTestData();

    String taskName = "Test1";
    String taskDescription = "Description Test1";

    Task task = new Task();
    task.setName(taskName);
    task.setDescription(taskDescription);

    createTaskTest(task);

    buttonTaskPanelMock.getBtnCopy().doClick();
    buttonTaskPanelMock.getBtnSave().doClick();

    String taskNameCopy = taskName + "_copy";
    String taskDescriptionCopy = taskDescription + "_copy";
    assertEquals(taskNameCopy, getTaskName(taskNameCopy));
    assertEquals(taskDescriptionCopy, getTaskDescription(taskNameCopy));
  }

  @Test
  @Order(3)
  public void copy_new_task_duplicate_test() {
    assertNotNull(taskButtonPanelHandlerLazy.get());
    assertNotNull(taskPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());

    assertNotNull(connectionButtonPanelHandlerLazy.get());
    assertNotNull(connectionPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(configViewLazy.get());

    createNewConnectionTestData();

    String taskName = "Test2";

    Task task = new Task();
    task.setName(taskName);

    List<Connection> connectionAll = configurationManagerLazy.get().getConfigList(Connection.class);
    List<List<?>> connectionDataList;
    connectionDataList = new LinkedList<>();
    connectionAll.forEach(connection -> connectionDataList.add(
        new ArrayList<>(Arrays.asList(connection.getName(), connection.getUserName(),
                                      connection.getUrl(), connection.getJar(), connection.getDriver()))));
    this.taskPanelLazy.get().getTaskConnectionComboBox().setTableData(connectionDataList);

    createTaskTest(task);

    buttonTaskPanelMock.getBtnCopy().doClick();

    taskPanelLazy.get().getJTextFieldTask().setText(taskName);

    disposeWindows();

    assertThrows(NotFoundException.class, buttonTaskPanelMock.getBtnSave()::doClick,
                 String.format("Name %s already exists, please enter another one.", taskName));
  }

  private void createNewConnectionTestData() {
    String connectionName = "Test" + Math.random();
    Connection connection = new Connection();
    connection.setName(connectionName);

    buttonConnectionPanelMock.getBtnNew().doClick();
    connectionPanelLazy.get().getConnTypeTab().setSelectedTab(ConnectionTypeTabPane.JDBC);
    connectionPanelLazy.get().getJTextFieldConnectionName().setText(connection.getName());
    buttonConnectionPanelMock.getBtnSave().doClick();
  }

  private String getTaskName(String taskNameCopy) {
    return configurationManagerLazy.get().getConfig(Task.class, taskNameCopy).getName();
  }

  private String getTaskDescription(String taskNameCopy) {
    return configurationManagerLazy.get().getConfig(Task.class, taskNameCopy).getDescription();
  }
}
