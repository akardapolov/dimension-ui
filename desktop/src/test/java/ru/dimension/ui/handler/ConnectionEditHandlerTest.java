package ru.dimension.ui.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.HandlerMock;
import ru.dimension.ui.model.config.Connection;


@Log4j2
public class ConnectionEditHandlerTest extends HandlerMock {

  Connection connectionExpectedNew, connectionExpectedCopy;

  @BeforeAll
  public void setUpLocal() throws IOException {
    connectionExpectedNew = objectMapper.readValue(getTestData("connection_new_edit.json"), Connection.class);
    connectionExpectedCopy = objectMapper.readValue(getTestData("connection_copy_edit.json"), Connection.class);

    String passEncNew = encryptDecrypt.get().encrypt(connectionExpectedNew.getPassword());
    String passEncCopy = encryptDecrypt.get().encrypt(connectionExpectedNew.getPassword());

    connectionExpectedNew.setPassword(passEncNew);
    connectionExpectedCopy.setPassword(passEncCopy);
  }

  @Test
  public void create_new_connection_test() {
    startTabView();

    connectionExpectedNew.setName(connectionExpectedNew.getName() + "Test");
    createConnectionTest(connectionExpectedNew);

    Connection connectionActual = getConnection(connectionExpectedNew.getName());
    connectionActual.setId(connectionExpectedNew.getId());
    connectionActual.setPassword(connectionExpectedNew.getPassword());

    assertEquals(connectionExpectedNew, connectionActual);
  }

  @Test
  public void copy_new_connection_test() {
    startTabView();

    createConnectionTest(connectionExpectedNew);

    buttonConnectionPanelMock.getBtnCopy().doClick();
    buttonConnectionPanelMock.getBtnSave().doClick();

    Connection connectionActual = getConnection(connectionExpectedCopy.getName());
    connectionActual.setId(connectionExpectedCopy.getId());
    connectionActual.setPassword(connectionExpectedCopy.getPassword());

    assertEquals(connectionExpectedCopy, connectionActual);
  }


  @Test
  public void copy_new_connection_duplicate_test() {
    startTabView();

    String connectionName = "Test3";

    Connection connection = new Connection();
    connection.setName(connectionName);

    createConnectionTest(connection);

    buttonConnectionPanelMock.getBtnCopy().doClick();

    connectionPanelLazy.get().getJTextFieldConnectionName().setText(connectionName);

    disposeWindows();

    assertThrows(NotFoundException.class, buttonConnectionPanelMock.getBtnSave()::doClick,
                 String.format("Name %s already exists, please enter another one.", connectionName));
  }

  @Test
  public void edit_pass_connection_test() {
    startTabView();

    connectionExpectedNew.setName(connectionExpectedNew.getName() + "Test1");
    createConnectionTest(connectionExpectedNew);

    buttonConnectionPanelMock.getBtnEdit().doClick();

    connectionPanelLazy.get().getJTextFieldConnectionPassword().setText("custom");

    disposeWindows();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    CompletableFuture.runAsync(buttonConnectionPanelMock.getBtnSave()::doClick, executor).join();
  }

  @Test
  public void edit_no_pass_connection_test() {
    startTabView();

    connectionExpectedNew.setId(connectionExpectedNew.getId() + 10);
    connectionExpectedNew.setName(connectionExpectedNew.getName() + "copy_");
    createConnectionTest(connectionExpectedNew);

    buttonConnectionPanelMock.getBtnEdit().doClick();

    disposeWindows();

    ExecutorService executor = Executors.newSingleThreadExecutor();
    CompletableFuture.runAsync(buttonConnectionPanelMock.getBtnSave()::doClick, executor).join();
  }

  private void startTabView() {
    assertNotNull(connectionButtonPanelHandlerLazy.get());
    assertNotNull(connectionPanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(configViewLazy.get());
  }

  private Connection getConnection(String connectionName) {
    return configurationManagerLazy.get().getConfig(Connection.class, connectionName);
  }
}
