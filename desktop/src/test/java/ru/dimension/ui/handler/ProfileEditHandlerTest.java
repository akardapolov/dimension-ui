package ru.dimension.ui.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Isolated;
import ru.dimension.di.ServiceLocator;
import ru.dimension.tt.swing.TTTable;
import ru.dimension.ui.HandlerMock;
import ru.dimension.ui.exception.NotFoundException;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.view.handler.core.ConfigSelectionContext;
import ru.dimension.ui.view.table.row.Rows;
import ru.dimension.ui.view.table.row.Rows.ProfileRow;

@Log4j2
@Isolated
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProfileEditHandlerTest extends HandlerMock {

  Profile profileNewExpected, profileCopiedNew, profileCopiedExpected;

  Connection connectionExpectedNew;

  @BeforeEach
  public void resetContext() {
    if (configSelectionContextLazy != null) {
      ConfigSelectionContext ctx = configSelectionContextLazy.get();
      ctx.setSelectedProfileId(null);
      ctx.setSelectedTaskId(null);
      ctx.setSelectedConnectionId(null);
      ctx.setSelectedQueryId(null);
    }
  }

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
  @Order(1)
  public void create_new_profile_test() {
    startTabView();

    createNewTestData(profileNewExpected);

    Profile profileActual = getProfile(profileNewExpected.getName());
    profileActual.setId(profileNewExpected.getId());

    assertEquals(profileNewExpected, profileActual);
  }

  @Test
  @Order(2)
  public void copy_new_profile_test() throws InterruptedException, InvocationTargetException {
    assertNotNull(profileButtonPanelHandlerLazy.get());
    assertNotNull(profilePanelLazy.get());
    assertNotNull(configurationManagerLazy.get());

    startTabView();

    createNewTestData(profileCopiedNew);

    selectProfileInTable(profileCopiedNew.getName());

    waitForEvents();

    int lastRow = profileCase.getJxTable().getRowCount() - 1;
    profileCase.getJxTable().setRowSelectionInterval(lastRow, lastRow);

    waitForEvents();

    Profile createdProfile = configurationManagerLazy.get()
        .getConfigList(Profile.class).stream()
        .filter(p -> p.getName().equals("Test profile copied"))
        .findFirst()
        .orElseThrow();

    ConfigSelectionContext context = ServiceLocator.get(ConfigSelectionContext.class, "configSelectionContext");
    context.setSelectedProfileId(createdProfile.getId());

    buttonProfilePanelMock.getBtnCopy().doClick();
    buttonProfilePanelMock.getBtnSave().doClick();

    Profile profileActual = getProfile(profileCopiedExpected.getName());
    profileActual.setId(profileCopiedExpected.getId());

    assertEquals(profileCopiedExpected, profileActual);
  }

  private void selectProfileInTable(String profileName) throws InterruptedException, InvocationTargetException {
    TTTable<ProfileRow, JXTable> tt = profileCase.getTypedTable();
    JXTable table = profileCase.getJxTable();

    for (int i = 0; i < tt.model().items().size(); i++) {
      if (tt.model().items().get(i).getName().equals(profileName)) {
        int finalI = i;
        SwingUtilities.invokeAndWait(() -> table.setRowSelectionInterval(finalI, finalI));
        break;
      }
    }
  }

  @Test
  @Order(3)
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

    TTTable<ProfileRow, JXTable> tt = profileCase.getTypedTable();
    tt.addItem(new Rows.ProfileRow(profile.getId(), profile.getName()));

    List<Task> taskAllList = taskTestData();

    taskAllList.stream()
        .filter(t -> !profile.getTaskList().contains(t.getId()))
        .forEach(taskIn ->
                     profilePanelLazy.get().getMultiSelectTaskPanel()
                         .getTaskListCase().<Rows.TaskRow>getTypedTable()
                         .addItem(new Rows.TaskRow(taskIn.getId(), taskIn.getName()))
        );

    taskAllList.stream()
        .filter(t -> profile.getTaskList().contains(t.getId()))
        .forEach(taskIn ->
                     profilePanelLazy.get().getMultiSelectTaskPanel()
                         .getSelectedTaskCase().<Rows.TaskRow>getTypedTable()
                         .addItem(new Rows.TaskRow(taskIn.getId(), taskIn.getName()))
        );

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
