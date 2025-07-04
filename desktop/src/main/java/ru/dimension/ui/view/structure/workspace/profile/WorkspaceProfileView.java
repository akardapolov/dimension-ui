package ru.dimension.ui.view.structure.workspace.profile;

import dagger.Lazy;
import java.awt.BorderLayout;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.config.prototype.profile.WorkspaceProfileComponent;
import ru.dimension.ui.config.prototype.profile.WorkspaceProfileModule;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.laf.LafColorGroup;
import ru.dimension.ui.model.info.ProfileInfo;
import ru.dimension.ui.model.info.TaskInfo;
import ru.dimension.ui.router.listener.ProfileStartStopListener;
import ru.dimension.ui.view.structure.workspace.WorkspacePresenter;
import ru.dimension.ui.Application;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskKey;
import ru.dimension.ui.model.RunStatus;
import ru.dimension.ui.router.event.EventListener;
import ru.dimension.ui.view.structure.workspace.task.WorkspaceTaskView;

@Log4j2
public class WorkspaceProfileView extends JPanel implements ProfileStartStopListener {

  private final int profileId;

  private final JTabbedPane profileTaskPane = new JTabbedPane();

  private final WorkspaceProfileComponent workspaceProfileComponent;

  @Inject
  @Named("eventListener")
  EventListener eventListener;

  @Inject
  @Named("profileManager")
  ProfileManager profileManager;

  @Inject
  @Named("workspacePresenter")
  Lazy<WorkspacePresenter> workspacePresenter;

  @Inject
  @Named("splitProfileListButtonsAndStatus")
  JSplitPane splitProfileListAndButtons;

  @Named("jPanelProfileStatus")
  @Inject
  JPanel jPanelProfileStatus;

  @Named("workspaceProfileStartButton")
  @Inject
  JButton startButton;

  @Named("workspaceProfileStopButton")
  @Inject
  JButton stopButton;

  @Named("profileStatusJLabel")
  @Inject
  JLabel profileStatusJLabel;

  public WorkspaceProfileView(int profileId) {
    this.workspaceProfileComponent = Application.getInstance().initProfile(new WorkspaceProfileModule(this));
    this.workspaceProfileComponent.inject(this);

    this.profileId = profileId;

    this.workspacePresenter.get();

    this.startButton.addActionListener(workspacePresenter.get());
    this.stopButton.addActionListener(workspacePresenter.get());

    this.setupUIForProfileInfo();

    this.cleanStartStopButtonPanel();

    this.setProfileStatusJLabel(profileId);

    this.setLayout(new BorderLayout());

    this.add(profileTaskPane);
  }

  public void setupUIForProfileInfo() {
    ProfileInfo profileInfo = profileManager.getProfileInfoById(profileId);

    startButton.setEnabled(profileInfo.getStatus().equals(RunStatus.NOT_RUNNING));
    stopButton.setEnabled(profileInfo.getStatus().equals(RunStatus.RUNNING));

    profileInfo.getTaskInfoList().forEach(taskId -> {
      TaskInfo taskInfo = this.profileManager.getTaskInfoById(taskId);
      profileTaskPane.add(taskInfo.getName(), getTaskView(taskInfo.getId()));
    });
  }

  private void cleanStartStopButtonPanel() {
    jPanelProfileStatus.removeAll();
    jPanelProfileStatus.revalidate();
    jPanelProfileStatus.repaint();

    jPanelProfileStatus.add(getProfileInfoJPanel());

    splitProfileListAndButtons.setDividerLocation(200);
  }

  private JPanel getProfileInfoJPanel() {
    JPanel jPanel = new JPanel();
    PainlessGridBag gbl = new PainlessGridBag(jPanel, PGHelper.getPGConfig(0), false);

    gbl.row()
        .cellXRemainder(getButtonPanel()).fillX();
    gbl.row()
        .cellXRemainder(getStatusPanel()).fillX();
    gbl.row()
        .cellXYRemainder(new JLabel()).fillXY();

    gbl.done();

    LaF.setBackgroundConfigPanel(LafColorGroup.CONFIG_PANEL, jPanel);

    return jPanel;
  }

  private JPanel getButtonPanel() {
    JPanel buttonPanel = new JPanel();
    LaF.setBackgroundConfigPanel(LafColorGroup.CONFIG_PANEL, buttonPanel);

    buttonPanel.setBorder(new EtchedBorder());
    PainlessGridBag gblButton = new PainlessGridBag(buttonPanel, PGHelper.getPGConfig(), false);

    gblButton.row()
        .cell(startButton)
        .cell(stopButton);

    gblButton.done();

    return buttonPanel;
  }

  private JPanel getStatusPanel() {
    JPanel buttonPanel = new JPanel();
    LaF.setBackgroundConfigPanel(LafColorGroup.CONFIG_PANEL, buttonPanel);

    PainlessGridBag gblProfileStatus = new PainlessGridBag(buttonPanel, PGHelper.getPGConfig(), false);

    gblProfileStatus.row()
        .cell(profileStatusJLabel);

    gblProfileStatus.done();

    return buttonPanel;
  }

  private JPanel getTaskView(int taskId) {
    JSplitPane sqlPane = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 150);

    ProfileTaskKey profileTaskKey = new ProfileTaskKey(profileId, taskId);
    WorkspaceTaskView workspaceTaskView = new WorkspaceTaskView(sqlPane, profileTaskKey, workspaceProfileComponent);
    workspaceTaskView.loadSql();

    PainlessGridBag gbl = new PainlessGridBag(workspaceTaskView, PGHelper.getPGConfig(), false);

    gbl.row()
        .cellXYRemainder(sqlPane).fillXY();

    gbl.done();

    return workspaceTaskView;
  }

  private void setProfileStatusJLabel(int profileId) {
    ProfileInfo profileInfo = profileManager.getProfileInfoById(profileId);
    profileStatusJLabel.setText("Status: " + profileInfo.getStatus().getDescription());
  }

  @Override
  public void fireOnStartOnWorkspaceProfileView(int profileId) {
    startButton.setEnabled(false);
    stopButton.setEnabled(true);

    setProfileStatusJLabel(profileId);
  }

  @Override
  public void fireOnStopOnWorkspaceProfileView(int profileId) {
    startButton.setEnabled(true);
    stopButton.setEnabled(false);

    setProfileStatusJLabel(profileId);
  }
}
