package ru.dimension.ui.view.panel.report;

import static ru.dimension.ui.laf.LafColorGroup.CHART_PANEL;
import static ru.dimension.ui.laf.LafColorGroup.REPORT;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.plaf.basic.CalendarHeaderHandler;
import org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler;
import org.painlessgridbag.PainlessGridBag;
import ru.dimension.ui.helper.DateHelper;
import ru.dimension.ui.helper.GUIHelper;
import ru.dimension.ui.helper.PGHelper;
import ru.dimension.ui.helper.ReportHelper;
import ru.dimension.ui.laf.LaF;
import ru.dimension.ui.model.table.JXTableCase;
import ru.dimension.ui.model.view.tab.ReportTabPane;
import ru.dimension.ui.view.panel.DateTimePicker;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class ReportTabsPane extends JTabbedPane {

  private final JButton saveBtnPDFReport;
  private final JXTaskPaneContainer containerCardDesign;
  private final JSplitPane jspDesign;
  private final JSplitPane jspReport;
  private final DateTimePicker dateTimePickerFrom;
  private final DateTimePicker dateTimePickerTo;
  private final JLabel lblFromDesign;
  private final JLabel lblToDesign;
  private final JButton saveBtnDesign;
  private final JButton openBtnDesign;
  private final JButton clearBtnDesign;
  private final JButton showBtnDesign;
  private final JButton delBtnDesign;
  private final JButton delBtnReport;
  private final JCheckBox collapseBtnDesign;
  private final JButton generateReportBtnDesign;
  private final JXTableCase savedReportCase;
  private final ReportHelper reportHelper;
  private final JScrollPane jspCardDesign;
  private final JScrollPane scrollPanePDF;
  private String reportPdfPath;
  private final FilesPanel filesPanel;
  private final JTabbedPane jTabbedPaneChart;
  private final JSplitPane jspOpenDesign;
  private final JXTableCase designReportCase;
  private final JLabel designSaveInfo;
  private boolean showedFlag;
  private JPanel rightPanel;

  @Inject
  public ReportTabsPane(@Named("dateTimePickerFrom") DateTimePicker dateTimePickerFrom,
                        @Named("dateTimePickerTo") DateTimePicker dateTimePickerTo,
                        @Named("containerCardDesign") JXTaskPaneContainer containerCardDesign,
                        @Named("savedReportCase") JXTableCase savedReportCase,
                        @Named("designReportCase") JXTableCase designReportCase,
                        ReportHelper reportHelper) {

    this.reportHelper = reportHelper;
    this.reportPdfPath = " ";
    this.showedFlag = false;

    //Components of the design panel
    this.jspDesign = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 230);
    this.rightPanel = new JPanel(new GridLayout(1, 1));
    this.rightPanel.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, rightPanel);

    this.jspDesign.setResizeWeight(0);
    this.jspDesign.setResizeWeight(1);

    //Components of the report panel
    this.jspReport = GUIHelper.getJSplitPane(JSplitPane.HORIZONTAL_SPLIT, 10, 200);

    this.jspOpenDesign = GUIHelper.getJSplitPane(JSplitPane.VERTICAL_SPLIT, 10, 150);

    this.containerCardDesign = containerCardDesign;
    this.jspCardDesign = new JScrollPane();
    GUIHelper.setScrolling(jspCardDesign);

    this.designReportCase = designReportCase;

    SimpleDateFormat format = new SimpleDateFormat(reportHelper.getFormatPattern());

    this.dateTimePickerFrom = dateTimePickerFrom;
    this.dateTimePickerFrom.setFormats(format);
    this.dateTimePickerFrom.setTimeFormat(format);
    UIManager.put(CalendarHeaderHandler.uiControllerID, SpinningCalendarHeaderHandler.class.getName());
    this.dateTimePickerFrom.getMonthView().setZoomable(true);

    this.dateTimePickerTo = dateTimePickerTo;
    this.dateTimePickerTo.setFormats(format);
    this.dateTimePickerTo.setTimeFormat(format);
    UIManager.put(CalendarHeaderHandler.uiControllerID, SpinningCalendarHeaderHandler.class.getName());
    this.dateTimePickerTo.getMonthView().setZoomable(true);

    Map.Entry<Date, Date> range = DateHelper.getRangeDate();
    dateTimePickerFrom.setDate(range.getKey());
    dateTimePickerTo.setDate(range.getValue());

    this.lblFromDesign = new JLabel("From");
    this.lblToDesign = new JLabel("To");

    this.saveBtnDesign = new JButton("Save");
    this.saveBtnDesign.setToolTipText("Save design");
    this.saveBtnDesign.setMnemonic('S');
    this.saveBtnDesign.setEnabled(false);

    this.openBtnDesign = new JButton("Open");
    this.openBtnDesign.setToolTipText("Open saved design");
    this.openBtnDesign.setMnemonic('O');

    this.clearBtnDesign = new JButton("Clear");
    this.clearBtnDesign.setMnemonic('C');
    this.clearBtnDesign.setEnabled(false);

    this.showBtnDesign = new JButton("Show");
    this.showBtnDesign.setMnemonic('H');

    this.collapseBtnDesign = new JCheckBox("Collapse all");
    this.collapseBtnDesign.setMnemonic('A');
    this.collapseBtnDesign.setVisible(false);

    this.generateReportBtnDesign = new JButton("Report");
    this.generateReportBtnDesign.setMnemonic('T');
    this.generateReportBtnDesign.setToolTipText("Generate report");
    this.generateReportBtnDesign.setEnabled(false);

    this.delBtnDesign = new JButton("Delete");
    this.delBtnDesign.setMnemonic('D');
    this.delBtnDesign.setToolTipText("Delete saved design");
    this.delBtnDesign.setEnabled(false);

    this.filesPanel = new FilesPanel();
    this.jTabbedPaneChart = new JTabbedPane();

    this.designSaveInfo = new JLabel();
    this.designSaveInfo.setForeground(new Color(255, 93, 93));

    this.saveBtnPDFReport = new JButton("Save");
    this.saveBtnPDFReport.setMnemonic('S');
    this.saveBtnPDFReport.setEnabled(false);
    this.delBtnReport = new JButton("Delete");
    this.delBtnReport.setMnemonic('D');
    this.delBtnReport.setToolTipText("Delete saved report");
    this.delBtnReport.setEnabled(false);

    this.savedReportCase = savedReportCase;
    this.scrollPanePDF = new JScrollPane();
    GUIHelper.setScrolling(scrollPanePDF);
    JPanel pdfPanel = new JPanel(new GridLayout(1, 1));
    pdfPanel.setBorder(new EtchedBorder());
    LaF.setBackgroundConfigPanel(CHART_PANEL, pdfPanel);
    this.scrollPanePDF.setViewportView(pdfPanel);

    this.add("Design", fillDesignPanel());
    this.add("Report", fillReportPanel());
  }

  private JPanel fillDesignPanel() {
    JPanel jPanel = new JPanel();
    JPanel datePanel = new JPanel();
    datePanel.setBorder(new EtchedBorder());

    PainlessGridBag gblDP = new PainlessGridBag(datePanel, PGHelper.getPGConfig(), false);

    JPanel designPanelInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    designPanelInfo.add(designSaveInfo);

    gblDP.row().cell(lblFromDesign).cell(dateTimePickerFrom)
        .cell(lblToDesign).cell(dateTimePickerTo)
        .cell(showBtnDesign).cell(saveBtnDesign)
        .cell(delBtnDesign).cell(clearBtnDesign)
        .cell(generateReportBtnDesign)
        .cell(new JLabel()).fillX()
        .cell(designPanelInfo).fillXY();
    gblDP.done();

    JPanel jpLeft = new JPanel(new VerticalLayout());
    LaF.setBackgroundColor(REPORT, jpLeft);

    jpLeft.add(collapseBtnDesign);
    jpLeft.add(containerCardDesign);

    jspCardDesign.setViewportView(jpLeft);

    jspOpenDesign.setTopComponent(designReportCase.getJScrollPane());
    jspOpenDesign.setBottomComponent(jspCardDesign);

    JPanel leftPanel = new JPanel(new GridLayout(1, 1));
    leftPanel.setBorder(new EtchedBorder());
    leftPanel.add(jspOpenDesign);

    this.rightPanel.add(jTabbedPaneChart);
    jspDesign.setLeftComponent(leftPanel);
    jspDesign.setRightComponent(rightPanel);

    PainlessGridBag gbl = new PainlessGridBag(jPanel, PGHelper.getPGConfig(), false);

    gbl.row().cell(datePanel).fillX();
    gbl.row().cellXRemainder(jspDesign).fillXY();
    gbl.done();
    return jPanel;
  }

  private JPanel fillReportPanel() {
    JPanel jPanel = new JPanel();
    JPanel jPanelBtn = new JPanel(new FlowLayout(FlowLayout.LEFT));
    jPanelBtn.setBorder(new EtchedBorder());
    jPanelBtn.add(saveBtnPDFReport);
    jPanelBtn.add(delBtnReport);

    jspReport.setLeftComponent(savedReportCase.getJScrollPane());
    jspReport.setRightComponent(scrollPanePDF);

    PainlessGridBag gbl = new PainlessGridBag(jPanel, PGHelper.getPGConfig(), false);
    gbl.row().cell(jPanelBtn).fillX();
    gbl.row().cellXRemainder(jspReport).fillXY();
    gbl.done();

    return jPanel;
  }

  public void refresh() {
    jspDesign.setDividerLocation(200);
  }

  public void setSelectedTab(ReportTabPane tabbedPane) {
    switch (tabbedPane) {
      case DESIGN -> this.setSelectedIndex(0);
      case REPORT -> this.setSelectedIndex(1);
    }
  }
}
