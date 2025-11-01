package ru.dimension.ui.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.HandlerMock;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;


@Log4j2
public class ProfileEditHandlerTest extends HandlerMock {

  Profile profileNewExpected, profileCopiedNew, profileCopiedExpected;

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

    // Task
    taskTestData().forEach(task -> configurationManagerLazy.get().addConfig(task, Task.class));

    // Profile
    profileNewExpected = objectMapper.readValue(getTestData("profile_new_edit.json"), Profile.class);
    profileCopiedNew = objectMapper.readValue(getTestData("profile_copied_new.json"), Profile.class);
    profileCopiedExpected = objectMapper.readValue(getTestData("profile_copied_expected.json"), Profile.class);
  }

  @Test
  public void create_new_profile_test() {
    startTabView();

    createNewTestData(profileNewExpected);

    Profile profileActual = getProfile(profileNewExpected.getName());
    profileActual.setId(profileNewExpected.getId());

    assertEquals(profileNewExpected, profileActual);
  }

  @Test
  public void copy_new_profile_test() {
    startTabView();

    createNewTestData(profileCopiedNew);

    buttonProfilePanelMock.getBtnCopy().doClick();
    buttonProfilePanelMock.getBtnSave().doClick();

    Profile profileActual = getProfile(profileCopiedExpected.getName());
    profileActual.setId(profileCopiedExpected.getId());

    assertEquals(profileCopiedExpected, profileActual);
  }

  @Test
  public void copy_new_profile_duplicate_test() {
    assertNotNull(profileButtonPanelHandlerLazy.get());
    assertNotNull(profilePanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(configViewLazy.get());

    String profileName = "Test2";

    Profile profile = new Profile();
    profile.setName(profileName);
    profile.setDescription(profileName);
    profile.setTaskList(List.of(0));

    createNewTestData(profile);

    buttonProfilePanelMock.getBtnCopy().doClick();

    profilePanelLazy.get().getJTextFieldProfile().setText(profileName);

    disposeWindows();

    assertThrows(NotFoundException.class, buttonProfilePanelMock.getBtnSave()::doClick,
                 String.format("Name %s already exists, please enter another one.", profileName));
  }

  private void createNewTestData(Profile profile) {
    buttonProfilePanelMock.getBtnNew().doClick();
    profilePanelLazy.get().getJTextFieldProfile().setText(profile.getName());
    profilePanelLazy.get().getJTextFieldDescription().setText(profile.getDescription());

    profileCase.getDefaultTableModel().addRow(new Object[]{profile.getId(), profile.getName()});

    List<Task> taskAllList = taskTestData();

    taskAllList.stream()
        .filter(t -> !profile.getTaskList().contains(t.getId()))
        .forEach(
            taskIn -> profilePanelLazy.get().getMultiSelectTaskPanel()
                .getTaskListCase().getDefaultTableModel()
                .addRow(new Object[]{taskIn.getId(), taskIn.getName()}));

    taskAllList.stream()
        .filter(t -> profile.getTaskList().contains(t.getId()))
        .forEach(
            taskIn -> {
              profilePanelLazy.get().getMultiSelectTaskPanel()
                  .getSelectedTaskCase().getDefaultTableModel()
                  .addRow(new Object[]{taskIn.getId(), taskIn.getName()});
            });

    buttonProfilePanelMock.getBtnSave().doClick();
    startTabView();
  }

  private List<Task> taskTestData() {

    Task task2 = new Task();
    task2.setId(1);
    task2.setName("Test 1");

    Task task3 = new Task();
    task3.setId(2);
    task3.setName("Test 2");

    Task task4 = new Task();
    task4.setId(3);
    task4.setName("Test 3");

    Task task5 = new Task();
    task5.setId(4);
    task5.setName("Test 4");

    return Stream.of(task2, task3, task4, task5).collect(Collectors.toList());
  }

  private void startTabView() {
    assertNotNull(profileButtonPanelHandlerLazy.get());
    assertNotNull(profilePanelLazy.get());
    assertNotNull(configurationManagerLazy.get());
    assertNotNull(configViewLazy.get());
  }

  private Profile getProfile(String profileName) {
    return configurationManagerLazy.get().getConfig(Profile.class, profileName);
  }
}
