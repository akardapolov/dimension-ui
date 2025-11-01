package ru.dimension.ui.view.structure.config;

import static ru.dimension.ui.model.config.ConfigClasses.Connection;
import static ru.dimension.ui.model.config.ConfigClasses.Query;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.type.ConnectionType;
import ru.dimension.ui.router.listener.ConfigListener;
import ru.dimension.ui.router.listener.ProfileAddListener;
import ru.dimension.ui.state.NavigatorState;
import ru.dimension.ui.view.structure.ConfigView;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.model.config.ConfigClasses;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.view.ConfigState;
import ru.dimension.ui.router.event.EventListener;

@Log4j2
@Singleton
public class ConfigPresenter extends WindowAdapter implements ConfigListener, ProfileAddListener {

  private final ConfigView configView;
  private final NavigatorState navigatorState;
  private final EventListener eventListener;
  private final ConfigurationManager configurationManager;

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;

  @Inject
  public ConfigPresenter(@Named("configView") ConfigView configView,
                         @Named("navigatorState") NavigatorState navigatorState,
                         @Named("eventListener") EventListener eventListener,
                         @Named("configurationManager") ConfigurationManager configurationManager,
                         @Named("profileConfigCase") JXTableCase profileCase,
                         @Named("taskConfigCase") JXTableCase taskCase,
                         @Named("connectionConfigCase") JXTableCase connectionCase,
                         @Named("queryConfigCase") JXTableCase queryCase) {
    this.configView = configView;
    this.navigatorState = navigatorState;
    this.eventListener = eventListener;
    this.configurationManager = configurationManager;

    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;

    this.eventListener.addConfigStateListener(this);
    this.eventListener.addProfileAddListener(this);
  }

  @Override
  public void windowClosing(WindowEvent e) {
    log.info("Window configuration closing event received");
  }

  @Override
  public void fireShowConfig(ConfigState configState) {
    if (configState == ConfigState.SHOW) {
      this.configView.showConfig(navigatorState.getSelectedProfile());
    }
    if (configState == ConfigState.HIDE) {
      this.configView.hideProfile();
    }
  }

  public <T> void fillProfileModel(Class<T> clazz) {

    if (ConfigClasses.Profile.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Profile..");
      configurationManager.getConfigList(Profile.class)
          .forEach(e -> profileCase.getDefaultTableModel().addRow(new Object[]{e.getId(), e.getName()}));

    }

    if (ConfigClasses.Task.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Task..");
      configurationManager.getConfigList(Task.class)
          .forEach(e -> taskCase.getDefaultTableModel().addRow(new Object[]{e.getId(), e.getName()}));
    }

    if (Connection.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Connection..");
      configurationManager.getConfigList(Connection.class)
          .forEach(
              e -> {
                if (e.getType() != null) {
                  connectionCase.getDefaultTableModel()
                      .addRow(new Object[]{e.getId(), e.getName(), e.getType()});
                } else {
                  connectionCase.getDefaultTableModel()
                      .addRow(new Object[]{e.getId(), e.getName(), ConnectionType.JDBC});
                }
              }
          );
    }

    if (Query.equals(ConfigClasses.fromClass(clazz))) {
      log.info("Query..");
      configurationManager.getConfigList(Query.class)
          .forEach(e -> queryCase.getDefaultTableModel().addRow(new Object[]{e.getId(), e.getName()}));
    }

  }

  @Override
  public void fireProfileAdd() {
    profileCase.getDefaultTableModel().getDataVector().removeAllElements();
    profileCase.getDefaultTableModel().fireTableDataChanged();

    taskCase.getDefaultTableModel().getDataVector().removeAllElements();
    taskCase.getDefaultTableModel().fireTableDataChanged();

    connectionCase.getDefaultTableModel().getDataVector().removeAllElements();
    connectionCase.getDefaultTableModel().fireTableDataChanged();

    queryCase.getDefaultTableModel().getDataVector().removeAllElements();
    queryCase.getDefaultTableModel().fireTableDataChanged();

    fillProfileModel(Profile.class);
    fillProfileModel(Task.class);
    fillProfileModel(Connection.class);
    fillProfileModel(Query.class);
  }
}

