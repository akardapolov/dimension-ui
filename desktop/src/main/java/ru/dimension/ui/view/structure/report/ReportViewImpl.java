package ru.dimension.ui.view.structure.report;

import static ru.dimension.ui.laf.LafColorGroup.CONFIG_PANEL;

import dagger.Lazy;
import java.awt.event.KeyEvent;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTitledSeparator;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.manager.ProfileManager;
import ru.dimension.ui.model.ProfileTaskQueryKey;
import ru.dimension.ui.model.config.Profile;
import ru.dimension.ui.model.config.Query;
import ru.dimension.ui.model.config.Task;
import ru.dimension.ui.model.report.QueryReportData;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.view.BaseFrame;
import ru.dimension.ui.view.handler.report.design.DesignPanelHandler;
import ru.dimension.ui.view.handler.report.report.ReportPanelHandler;
import ru.dimension.ui.view.panel.report.ReportTabsPane;
import ru.dimension.ui.view.structure.ReportView;


@Log4j2
@Singleton
public class ReportViewImpl extends JPanel implements ReportView {

  private final Lazy<BaseFrame> jFrame;
  private final Lazy<ReportPresenter> reportPresenter;
  private final JXTableCase profileReportCase;
  private final JXTableCase taskReportCase;
  private final JXTableCase queryReportCase;
  private final ReportTabsPane reportTabsPane;
  private final DesignPanelHandler designPanelHandler;
  private final ReportPanelHandler reportPanelHandler;
  private final ProfileManager profileManager;
  private final Map<ProfileTaskQueryKey, QueryReportData> mapReportData;
  private final JSplitPane jSplitPaneReport;


  @Inject
  public ReportViewImpl(Lazy<BaseFrame> jFrame,
                        Lazy<ReportPresenter> reportPresenter,
                        @Named("profileReportCase") JXTableCase profileReportCase,
                        @Named("taskReportCase") JXTableCase taskReportCase,
                        @Named("queryReportCase") JXTableCase queryReportCase,
                        @Named("reportTaskPanel") ReportTabsPane reportTabsPane,
                        @Named("profileManager") ProfileManager profileManager,
                        @Named("mapReportData") Map<ProfileTaskQueryKey, QueryReportData> mapReportData,
                        @Named("designPanelHandler") DesignPanelHandler designPanelHandler,
                        @Named("reportPanelHandler") ReportPanelHandler reportPanelHandler
  ) {
    this.jFrame = jFrame;
    this.reportPresenter = reportPresenter;

    this.jSplitPaneReport = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 170);

    this.profileReportCase = profileReportCase;
    this.taskReportCase = taskReportCase;
    this.queryReportCase = queryReportCase;

    this.profileManager = profileManager;
    this.mapReportData = mapReportData;

    this.reportTabsPane = reportTabsPane;
    this.reportTabsPane.setMnemonicAt(0, KeyEvent.VK_D);
    this.reportTabsPane.setMnemonicAt(1, KeyEvent.VK_R);

    this.designPanelHandler = designPanelHandler;
    this.reportPanelHandler = reportPanelHandler;

    jSplitPaneReport.setLeftComponent(fillEntitiesPane());
    jSplitPaneReport.setRightComponent(reportTabsPane);
    jSplitPaneReport.setResizeWeight(1.0);

    PainlessGridBag gbl = new PainlessGridBag(this, PGHelper.getPGConfig(), false);
    gbl.row().cellXYRemainder(jSplitPaneReport).fillXY();

    gbl.done();
  }

  private JPanel fillEntitiesPane() {
    JPanel panel = new JPanel();
    LaF.setBackgroundConfigPanel(CONFIG_PANEL, panel);
    panel.setBorder(new EtchedBorder());

    PainlessGridBag gbl = new PainlessGridBag(panel, PGHelper.getPGConfig(), false);

    gbl.row()
        .cell(new JXTitledSeparator("Profile")).fillX();
    gbl.row()
        .cell(profileReportCase.getJScrollPane()).fillX();
    gbl.row()
        .cell(new JXTitledSeparator("Task")).fillX();
    gbl.row()
        .cell(taskReportCase.getJScrollPane()).fillX();
    gbl.row()
        .cell(new JXTitledSeparator("Query")).fillX();
    gbl.row()
        .cell(queryReportCase.getJScrollPane()).fillX();
    gbl.row()
        .cellXYRemainder(new JLabel()).fillXY();
    gbl.done();

    return panel;
  }

  @Override
  public void showReport() {
    this.reportTabsPane.refresh();
    this.setVisible(true);
  }

  @Override
  public void bindPresenter() {

    this.reportPresenter.get().fillModel(Profile.class);
    profileReportCase.getJxTable().getColumnExt(0).setVisible(false);
    profileReportCase.getJxTable().getColumnModel().getColumn(0)
        .setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());

    this.reportPresenter.get().fillModel(Task.class);
    taskReportCase.getJxTable().getColumnExt(0).setVisible(false);
    taskReportCase.getJxTable().getColumnModel().getColumn(0).setCellRenderer(new GUIHelper.ActiveColumnCellRenderer());

    this.reportPresenter.get().fillModel(Query.class);
  }
}
