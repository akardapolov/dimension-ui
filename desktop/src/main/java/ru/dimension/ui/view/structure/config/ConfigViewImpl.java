package ru.dimension.ui.view.structure.config;

import dagger.Lazy;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.GUIHelper.ActiveColumnCellRenderer;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.BaseFrame;
import ru.dimension.ui.view.handler.profile.MultiSelectTaskHandler;
import ru.dimension.ui.view.handler.profile.ProfileButtonPanelHandler;
import ru.dimension.ui.view.handler.profile.ProfileSelectionHandler;
import ru.dimension.ui.view.handler.task.MultiSelectQueryHandler;
import ru.dimension.ui.view.handler.task.TaskButtonPanelHandler;
import ru.dimension.ui.view.handler.task.TaskSelectionHandler;
import ru.dimension.ui.view.panel.config.connection.ConnectionPanel;
import ru.dimension.ui.view.panel.config.profile.ProfilePanel;
import ru.dimension.ui.view.structure.ConfigView;
import ru.dimension.ui.view.tab.ConfigTab;
import ru.dimension.ui.model.column.ProfileColumnNames;
import ru.dimension.ui.model.config.Connection;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.view.handler.connection.ConnectionButtonPanelHandler;
import ru.dimension.ui.view.handler.connection.ConnectionSelectionHandler;
import ru.dimension.ui.view.handler.connection.ConnectionTemplateTableHandler;
import ru.dimension.ui.view.handler.query.QueryButtonPanelHandler;
import ru.dimension.ui.view.handler.query.QueryMetadataHandler;
import ru.dimension.ui.view.handler.query.QueryMetricButtonPanelHandler;
import ru.dimension.ui.view.handler.query.QueryMetricHandler;
import ru.dimension.ui.view.handler.query.QuerySelectionHandler;
import ru.dimension.ui.view.panel.config.query.QueryPanel;
import ru.dimension.ui.view.panel.config.task.TaskPanel;

@Log4j2
@Singleton
public class ConfigViewImpl extends JDialog implements ConfigView {

  private final Lazy<BaseFrame> jFrame;
  private final Lazy<ConfigPresenter> configPresenter;

  private final JXTableCase profileCase;
  private final JXTableCase taskCase;
  private final JXTableCase connectionCase;
  private final JXTableCase queryCase;

  private final ConfigTab configTab;

  private final ProfileSelectionHandler profileSelectionHandler;
  private final TaskSelectionHandler taskSelectionHandler;
  private final ConnectionSelectionHandler connectionSelectionHandler;
  private final QuerySelectionHandler querySelectionHandler;
  private final QueryMetadataHandler queryMetadataHandler;
  private final QueryMetricButtonPanelHandler queryMetricButtonPanelHandler;
  private final ConnectionTemplateTableHandler connectionTemplateTableHandler;
  private final ProfilePanel profilePanel;
  private final TaskPanel taskPanel;
  private final ConnectionPanel connectionPanel;
  private final QueryPanel queryPanel;

  private final ProfileButtonPanelHandler profileButtonPanelHandler;
  private final TaskButtonPanelHandler taskButtonPanelHandler;
  private final ConnectionButtonPanelHandler connectionButtonPanelHandler;
  private final QueryButtonPanelHandler queryButtonPanelHandler;
  private final QueryMetricHandler queryMetricHandler;
  private final MultiSelectTaskHandler multiSelectTaskHandler;
  private final MultiSelectQueryHandler multiSelectQueryHandler;

  private final JCheckBox checkboxConfig;

  @Inject
  public ConfigViewImpl(Lazy<BaseFrame> jFrame,
                        Lazy<ConfigPresenter> configPresenter,
                        @Named("profileSelectionHandler") ProfileSelectionHandler profileSelectionHandler,
                        @Named("taskSelectionHandler") TaskSelectionHandler taskSelectionHandler,
                        @Named("connectionSelectionHandler") ConnectionSelectionHandler connectionSelectionHandler,
                        @Named("querySelectionHandler") QuerySelectionHandler querySelectionHandler,
                        @Named("queryMetadataHandler") QueryMetadataHandler queryMetadataHandler,
                        @Named("connectionTemplateTableHandler") ConnectionTemplateTableHandler connectionTemplateTableHandler,
                        @Named("profileConfigCase") JXTableCase profileCase,
                        @Named("taskConfigCase") JXTableCase taskCase,
                        @Named("connectionConfigCase") JXTableCase connectionCase,
                        @Named("queryConfigCase") JXTableCase queryCase,
                        @Named("jTabbedPaneConfig") ConfigTab configTab,
                        @Named("profileConfigPanel") ProfilePanel profilePanel,
                        @Named("taskConfigPanel") TaskPanel taskPanel,
                        @Named("connectionConfigPanel") ConnectionPanel connectionPanel,
                        @Named("queryConfigPanel") QueryPanel queryPanel,
                        @Named("profileButtonPanelHandler") ProfileButtonPanelHandler profileButtonPanelHandler,
                        @Named("taskButtonPanelHandler") TaskButtonPanelHandler taskButtonPanelHandler,
                        @Named("connectionButtonPanelHandler") ConnectionButtonPanelHandler connectionButtonPanelHandler,
                        @Named("queryButtonPanelHandler") QueryButtonPanelHandler queryButtonPanelHandler,
                        @Named("queryMetricButtonPanelHandler") QueryMetricButtonPanelHandler queryMetricButtonPanelHandler,
                        @Named("queryMetricHandler") QueryMetricHandler queryMetricHandler,
                        @Named("multiSelectTaskHandler") MultiSelectTaskHandler multiSelectTaskHandler,
                        @Named("multiSelectQueryHandler") MultiSelectQueryHandler multiSelectQueryHandler,
                        @Named("checkboxConfig") JCheckBox checkboxConfig) {
    this.jFrame = jFrame;
    this.configPresenter = configPresenter;

    this.profileCase = profileCase;
    this.taskCase = taskCase;
    this.connectionCase = connectionCase;
    this.queryCase = queryCase;

    this.configTab = configTab;
    this.profilePanel = profilePanel;
    this.taskPanel = taskPanel;
    this.connectionPanel = connectionPanel;
    this.queryPanel = queryPanel;

    this.profileSelectionHandler = profileSelectionHandler;
    this.taskSelectionHandler = taskSelectionHandler;
    this.connectionSelectionHandler = connectionSelectionHandler;
    this.querySelectionHandler = querySelectionHandler;
    this.queryMetadataHandler = queryMetadataHandler;
    this.queryMetricButtonPanelHandler = queryMetricButtonPanelHandler;
    this.queryMetricHandler = queryMetricHandler;
    this.multiSelectTaskHandler = multiSelectTaskHandler;
    this.multiSelectQueryHandler = multiSelectQueryHandler;
    this.connectionTemplateTableHandler = connectionTemplateTableHandler;

    this.profileButtonPanelHandler = profileButtonPanelHandler;
    this.taskButtonPanelHandler = taskButtonPanelHandler;
    this.connectionButtonPanelHandler = connectionButtonPanelHandler;
    this.queryButtonPanelHandler = queryButtonPanelHandler;

    this.checkboxConfig = checkboxConfig;

    Border finalBorder = GUIHelper.getGrayBorder();
    this.profileCase.getJxTable().setBorder(finalBorder);
    this.taskCase.getJxTable().setBorder(finalBorder);
    this.connectionCase.getJxTable().setBorder(finalBorder);
    this.queryCase.getJxTable().setBorder(finalBorder);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row()
        .cellXYRemainder(fillConfigPane()).fillXY();

    gbl.done();

    this.setTitle("Configuration");

    this.packConfig(false);
  }

  private JPanel fillConfigPane() {
    JPanel panelSettings = new JPanel();
    panelSettings.setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(panelSettings, PGHelper.getPGConfig(), false);

    gbl.row().cell(checkboxConfig).fillX()
        .cell(new JLabel()).fillX()
        .cell(new JLabel()).fillX()
        .cell(new JLabel()).fillX();

    gbl.row()
        .cell(new JXTitledSeparator("Profile")).fillX()
        .cell(new JXTitledSeparator("Task")).fillX()
        .cell(new JXTitledSeparator("Connection")).fillX()
        .cell(new JXTitledSeparator("Query")).fillX();

    gbl.row()
        .cell(profileCase.getJScrollPane()).fillXY()
        .cell(taskCase.getJScrollPane()).fillXY()
        .cellX(connectionCase.getJScrollPane(), 1).fillXY()
        .cellX(queryCase.getJScrollPane(), 1).fillXY();

    configTab.addTab("Profile", profilePanel);
    configTab.setMnemonicAt(0, KeyEvent.VK_P);
    configTab.addTab("Task", taskPanel);
    configTab.setMnemonicAt(1, KeyEvent.VK_T);
    configTab.addTab("Connection", connectionPanel);
    configTab.setMnemonicAt(2, KeyEvent.VK_O);
    configTab.addTab("Query", queryPanel);
    configTab.setMnemonicAt(3, KeyEvent.VK_Q);

    gbl.row()
        .cellXRemainder(configTab).fillXY();

    gbl.done();

    return panelSettings;
  }


  public void hideProfile() {
    this.setVisible(false);
  }

  public void showConfig(int id) {
    try {
      log.info("Profile id to view: {}", id);

      if (profileCase.getJxTable().getSelectedRowCount() > 0) {
        int rowIndex = getRowIndexForIdColumnFromModel(ProfileColumnNames.ID.getColName(), id);
        profileCase.getJxTable().setRowSelectionInterval(rowIndex, rowIndex);
      }
    } catch (Exception e) {
      log.catching(e);
    }

    this.packConfig(true);
  }

  private int getRowIndexForIdColumnFromModel(String columnName,
                                              int columnValue) {
    int index = 0;
    for (int i = 0; i < profileCase.getDefaultTableModel().getRowCount(); i++) {
      int rowValue = (int) profileCase.getDefaultTableModel()
          .getValueAt(i, profileCase.getDefaultTableModel().findColumn(columnName));
      if (columnValue == rowValue) {
        index = i;
      }
    }
    return index;
  }

  @Override
  public void bindPresenter() {
    configPresenter.get().fillProfileModel(Profile.class);
    profileCase.getJxTable().getColumnExt(0).setVisible(false);
    profileCase.getJxTable().getColumnModel().getColumn(0).setCellRenderer(new ActiveColumnCellRenderer());

    configPresenter.get().fillProfileModel(Task.class);
    taskCase.getJxTable().getColumnExt(0).setVisible(false);
    taskCase.getJxTable().getColumnModel().getColumn(0).setCellRenderer(new ActiveColumnCellRenderer());

    configPresenter.get().fillProfileModel(Connection.class);
    connectionCase.getJxTable().getColumnExt(0).setVisible(false);
    connectionCase.getJxTable().getColumnModel().getColumn(0)
        .setCellRenderer(new ActiveColumnCellRenderer());

    configPresenter.get().fillProfileModel(Query.class);
    queryCase.getJxTable().getColumnExt(0).setVisible(false);
    queryCase.getJxTable().getColumnModel().getColumn(0).setCellRenderer(new ActiveColumnCellRenderer());
    addWindowListener(configPresenter.get());
  }

  private void packConfig(boolean visible) {
    this.setVisible(visible);
    this.setModal(true);
    this.setResizable(true);
    this.pack();

    this.setSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width - 400,
                               Toolkit.getDefaultToolkit().getScreenSize().height - 100));
    this.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width) / 2 - getWidth() / 2,
                     (Toolkit.getDefaultToolkit().getScreenSize().height) / 2 - getHeight() / 2);
  }

}
