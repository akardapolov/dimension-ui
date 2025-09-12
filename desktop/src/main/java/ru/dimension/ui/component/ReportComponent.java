package ru.dimension.ui.component;

import javax.swing.JTabbedPane;
import lombok.Getter;
import ru.dimension.db.core.DStore;
import ru.dimension.ui.component.broker.Destination;
import ru.dimension.ui.component.broker.MessageBroker;
import ru.dimension.ui.component.broker.MessageBroker.Component;
import ru.dimension.ui.component.module.report.DesignModule;
import ru.dimension.ui.component.module.report.PlaygroundModule;
import ru.dimension.ui.helper.FilesHelper;
import ru.dimension.ui.manager.ConfigurationManager;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.manager.ReportManager;

public class ReportComponent {
  @Getter
  private JTabbedPane mainTabPane;

  private final ProfileManager profileManager;
  private final ConfigurationManager configurationManager;
  private final ReportManager reportManager;
  private final FilesHelper filesHelper;
  private final DStore dStore;

  private PlaygroundModule playgroundModule;
  private DesignModule designModule;

  private final MessageBroker broker = MessageBroker.getInstance();

  public ReportComponent(ProfileManager profileManager,
                         ConfigurationManager configurationManager,
                         ReportManager reportManager,
                         FilesHelper filesHelper,
                         DStore dStore) {

    this.profileManager = profileManager;
    this.configurationManager = configurationManager;
    this.reportManager = reportManager;
    this.filesHelper = filesHelper;
    this.dStore = dStore;

    initializeComponents();
  }

  private void initializeComponents() {
    mainTabPane = new JTabbedPane();

    this.playgroundModule = new PlaygroundModule(profileManager,
                                                 configurationManager,
                                                 reportManager,
                                                 filesHelper,
                                                 dStore);

    this.designModule = new DesignModule(profileManager,
                                         configurationManager,
                                         reportManager,
                                         filesHelper,
                                         dStore);

    mainTabPane.addTab("Playground", this.playgroundModule.getView());
    mainTabPane.addTab("Design", this.designModule.getView());

    broker.addReceiver(Destination.withDefault(Component.PLAYGROUND), playgroundModule.getPresenter());
    broker.addReceiver(Destination.withDefault(Component.DESIGN), designModule.getPresenter());
  }
}